package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.features.IsotopePatternExtractionStrategy;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractionStrategy;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MergedSpectrum;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.utils.MultipleCharges;
import de.unijena.bioinf.lcms.utils.Tracker;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.RawTraceRef;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PickFeaturesAndImportToSirius implements ProjectSpaceImporter<PickFeaturesAndImportToSirius.DbMapper>  {

    private final MergedFeatureExtractionStrategy segmentationStrategy;
    private final IsotopePatternExtractionStrategy isotopePatternExtractionStrategy;
    private final Ms2MergeStrategy ms2MergeStrategy;

    public PickFeaturesAndImportToSirius(MergedFeatureExtractionStrategy segmentationStrategy, IsotopePatternExtractionStrategy isotopePatternExtractionStrategy, Ms2MergeStrategy ms2MergeStrategy) {
        this.segmentationStrategy = segmentationStrategy;
        this.isotopePatternExtractionStrategy = isotopePatternExtractionStrategy;
        this.ms2MergeStrategy = ms2MergeStrategy;
    }
    @Override
    public DbMapper initializeImport(SiriusDatabaseAdapter adapter) {
        return new DbMapper();
    }

    @Override
    public void importRun(SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample sample) throws IOException {
        dbMapper.sampleIndizes.put(sample.getUid(), sample.getRun().getRunId());
        dbMapper.samples.put(sample.getUid(), sample);
    }

    @Override
    public void importMergedRun(SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample sample) throws IOException {
        dbMapper.sampleIndizes.put(sample.getUid(), sample.getRun().getRunId());
        dbMapper.mergedUiD = sample.getUid();
    }

    public long importTrace(SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProjectedTrace trace) throws IOException {
        SourceTrace sourceTrace = new SourceTrace();
        sourceTrace.setRawIntensities(trace.rawIntensityArrayList());
        sourceTrace.setRawScanIndexOffset(trace.getRawStartId());
        sourceTrace.setIntensities(trace.projectedIntensityArrayList());
        sourceTrace.setScanIndexOffset(trace.getProjectedStartId());
        sourceTrace.setRunId(dbMapper.sampleIndizes.get(trace.getSampleId()));
        adapter.importTrace(sourceTrace);
        return sourceTrace.getSourceTraceId();
    }

    @Override
    public AlignedFeatures[] importMergedTrace(TraceSegmentationStrategy traceSegmenter, SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTrace, Tracker tracker) throws IOException {
        // import each individual trace
        MergeTraceId tid = importMergedTraceWithoutIsotopes(adapter, dbMapper, mergedTrace);
        // now import isotopes
        MergeTraceId[] isotopes = new MergeTraceId[mergedTrace.getIsotopes().length];
        for (int k=0; k < isotopes.length; ++k) {
            isotopes[k] = importMergedTraceWithoutIsotopes(adapter, dbMapper, mergedTrace.getIsotopes()[k]);
        }
        // now extract the compounds
        AlignedFeatures[] features = extractCompounds(traceSegmenter, adapter, dbMapper, mergedSample, mergedTrace, tid, isotopes, tracker);
        if (features.length == 0) {
            removeMergedTrace(adapter, tid, isotopes);
            tracker.noFeatureFound(mergedTrace);
        } else {
            tracker.importFeatures(mergedTrace, features);
        }
        return features;
    }

    private AlignedFeatures[] extractCompounds(TraceSegmentationStrategy traceSegmenter, SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTrace, MergeTraceId dbId, MergeTraceId[] dbIsotopeIds, Tracker tracker) {
        // first we use segmentation to split the trace into segments
        // when then apply this segments to all traces and isotopes
        tracker.startExtractingCompounds(mergedSample, mergedTrace);
        MergedFeatureExtractionStrategy.Result pickedFeatures = segmentationStrategy.featureFinding(traceSegmenter,mergedSample,mergedTrace);
        // if there is no compound found in the trace...
        if (pickedFeatures.traceSegmentsForMergedTrace().length==0) return new AlignedFeatures[0];

        FeaturesAndSegment res = findFeaturesForTraces(mergedSample, mergedTrace, dbId, dbIsotopeIds, pickedFeatures);

        // doublecheck assignment
        /*
        if (true){
            Optional<TraceSegment[]> mergedSegments = checkForMergeOfNeighbouringFeatures(mergedSample, res.features, traceSegments);
            if (mergedSegments.isPresent() && mergedSegments.get().length>0) {
                res = findFeaturesForTraces(mergedSample, mergedTrace, dbId, dbIsotopeIds, mergedSegments.get());
            }
        }
         */
        // assign ms data
        assignMs2(dbMapper, mergedSample, mergedTrace, res.traceSegments, res.rawSegments, res.features);
        // assign ms1 data
        extractMs1(dbMapper, mergedSample, mergedTrace, res.traceSegments, res.rawSegments, res.features, dbIsotopeIds);
        // reassign charge
        reassignCharge(res.features);

        // for these annoying traces of signals that are all over the place we merge signals together if their MSMS is super similar or they both don't have msms at all
        AlignedFeatures[] finalFeatures = res.features.length >= 6 ? mergeFeaturesWithNoProperPeakAndIdenticalMsMs(mergedSample,mergedTrace,res.features,dbMapper) : res.features;

        // store feature
        for (int i = 0; i < finalFeatures.length; i++) {
            if (finalFeatures[i]==null) continue;
            try {
                if (!adapter.importAlignedFeature(finalFeatures[i])) {
                    finalFeatures[i] = null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Arrays.stream(finalFeatures).filter(Objects::nonNull).toArray(AlignedFeatures[]::new);
    }

    record FeaturesAndSegment (AlignedFeatures[] features, TraceSegment[] traceSegments, TraceSegment[][] rawSegments) {

    }

    private @NotNull FeaturesAndSegment findFeaturesForTraces(ProcessedSample mergedSample, MergedTrace mergedTrace, MergeTraceId dbId, MergeTraceId[] dbIsotopeIds, MergedFeatureExtractionStrategy.Result pickedFeatures) {
        /*
        TraceSegment[][] rawSegments;
        if (mergedTrace.getSamples().length == 1) {
            // do not project if samples.size == 1! -> just copy the trace segments
            rawSegments = new TraceSegment[1][];
            rawSegments[0] = Arrays.copyOf(traceSegments, traceSegments.length);
        } else {
            // now project these segments onto the raw sources
            rawSegments = segmentationStrategy.extractProjectedSegments(mergedSample, mergedTrace, traceSegments);
            // remove merged segments that do not have a single supported segment
            for (int i = 0; i < traceSegments.length; ++i) {
                int count = 0;
                for (int j = 0; j < rawSegments.length; ++j) {
                    if (rawSegments[j][i] != null) ++count;
                }
                if (count <= 0) {
                    // delete trace segment
                    // todo: better join them instead of deleting them
                    traceSegments[i] = null;
                }
            }
        }
         */

        final int charge = estimateChargeFromIsotopes(mergedSample, mergedTrace);
        final TraceSegment[] traceSegments = pickedFeatures.traceSegmentsForMergedTrace();
        final TraceSegment[][] rawSegments = pickedFeatures.traceSegmentsForIndividualTraces();
        // generatate monoisotopic features for each segment
        AlignedFeatures[] features = new AlignedFeatures[traceSegments.length];
        for (int fid = 0; fid < traceSegments.length; ++fid) {
            if (traceSegments[fid]==null) continue;
            features[fid] = new AlignedFeatures();
            features[fid].setDetectedAdducts(new DetectedAdducts());
            features[fid].setFeatures(new ArrayList<>());

            // assign segments
            int o = mergedTrace.startId();
            TraceSegment traceSegment = traceSegments[fid];
            features[fid].setTraceRef(new TraceRef(dbId.mergeTrace, o, traceSegment.leftEdge-o, traceSegment.apex-o, traceSegment.rightEdge-o));
            features[fid].setMsData(new MSData());
            setGenericAttributes(mergedTrace, traceSegment, traceSegment.apex,  features[fid], charge, mergedSample, mergedSample);
            features[fid].setIsotopicFeatures(new ArrayList<>());
            // set features
            for (int j=0; j < rawSegments.length; ++j) {
                TraceSegment r = rawSegments[j][fid];
                if (r ==null || r.rightEdge-r.leftEdge<=2) continue;
                ProjectedTrace subTrace = mergedTrace.getTraces()[j];
                Feature sub = new Feature();
                ProcessedSample S = mergedTrace.getSamples()[j];
                int o1 = subTrace.getProjectedStartId();
                int o2 = subTrace.getRawStartId();
                int ra = Math.max(subTrace.getRawStartId(), S.getScanPointInterpolator().reverseMapLowerIndex(r.leftEdge));
                int rb = Math.min(subTrace.getRawEndId(), S.getScanPointInterpolator().reverseMapLargerIndex(r.rightEdge));
                int rapex = getAdjustedApex(subTrace.raw(S.getMapping()), ra, rb);
                setGenericAttributes(subTrace.projected(mergedSample.getMapping()), r, rapex, sub, charge, S, mergedSample);
                sub.setTraceRef(new RawTraceRef(dbId.rawTraces[j], o1, r.leftEdge-o1, r.apex-o1, r.rightEdge-o1, ra-o2, rapex-o2, rb-o2, o2));

                features[fid].getFeatures().get().add(sub);
            }
        }
        return new FeaturesAndSegment(features, traceSegments, rawSegments);
    }

    private AlignedFeatures[] mergeFeaturesWithNoProperPeakAndIdenticalMsMs(ProcessedSample merged, MergedTrace trace, AlignedFeatures[] allFeatures, DbMapper dbMapper) {
        List<List<AlignedFeatures>> mergedFeatures = new ArrayList<>();
        List<AlignedFeatures> lastOne = new ArrayList<>();
        lastOne.add(allFeatures[0]);
        mergedFeatures.add(lastOne);
        for (int f=1; f < allFeatures.length; ++f) {
            AlignedFeatures left =  allFeatures[f-1];
            AlignedFeatures right = allFeatures[f];


            // check if both are no "real" peaks
            // i.e. their apexes and valleys are of similar height
            int leftApex = left.getTraceRef().absoluteApexId();
            int rightApex = right.getTraceRef().absoluteApexId();
            int valley = left.getTraceRef().getEnd()+left.getTraceRef().getScanIndexOffsetOfTrace();
            int valley2 = right.getTraceRef().getStart()+right.getTraceRef().getScanIndexOffsetOfTrace();
            // never merge if there is a gap in between
            if (Math.abs(valley-valley2)>1) {
                lastOne = new ArrayList<>();
                lastOne.add(right);
                mergedFeatures.add(lastOne);
                continue;
            }
            final float leftIntensity = trace.intensity(leftApex);
            final float rightIntensity = trace.intensity(rightApex);
            final float valleyIntensity = (trace.intensity(valley)+trace.intensity(valley2))/2f;
            final boolean visibleValley = (leftIntensity+rightIntensity > 3*valleyIntensity);
            final boolean clearValley = valleyIntensity/Math.min(leftIntensity,rightIntensity) < 0.75;
            // never merge if there is a visible apex
            if (visibleValley&&clearValley) {
                lastOne = new ArrayList<>();
                lastOne.add(right);
                mergedFeatures.add(lastOne);
                continue;
            }
            // check MsMs cosine score
            SimpleSpectrum l = left.getMSData().get().getMergedMSnSpectrum();
            SimpleSpectrum r = right.getMSData().get().getMergedMSnSpectrum();
            boolean merge = false;
            if (l!=null && r!=null) {
                CosineQueryUtils q = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10)));
                SpectralSimilarity spectralSimilarity = q.cosineProduct(q.createQuery(l, left.getAverageMass()), q.createQuery(r, right.getAverageMass()));
                if (visibleValley||clearValley) {
                    merge = (spectralSimilarity.similarity>=0.95 || (spectralSimilarity.similarity>=0.9 && spectralSimilarity.sharedPeaks>=6));
                } else {
                    merge = (spectralSimilarity.similarity>=0.9 || (spectralSimilarity.similarity>=0.8 && spectralSimilarity.sharedPeaks>=6));
                }
            } else if (l==null && r==null) {
                merge = true;
            } else merge = false;
            if (merge) {
                lastOne.add(right);
            } else {
                lastOne = new ArrayList<>();
                lastOne.add(right);
                mergedFeatures.add(lastOne);
            }
        }
        List<AlignedFeatures> result = new ArrayList<>();
        for (List<AlignedFeatures> tomerge : mergedFeatures) {
            if (tomerge.size()>1) {
                result.add((AlignedFeatures)mergeNeighbouringFeatures(merged,trace,tomerge.toArray(AbstractAlignedFeatures[]::new), dbMapper));
            } else if(!tomerge.isEmpty()){
                result.add(tomerge.getFirst());
            }
        }
        return result.toArray(AlignedFeatures[]::new);
    }

    private AbstractAlignedFeatures mergeNeighbouringFeatures(ProcessedSample mergedSample, MergedTrace trace, AbstractAlignedFeatures[] fs, DbMapper dbMapper) {
        AbstractAlignedFeatures merged = fs[0];
        // merge trace ref
        {
            int left = fs[0].getTraceRef().getStart();
            int right = fs[fs.length-1].getTraceRef().getEnd();
            int offset = merged.getTraceRef().getScanIndexOffsetOfTrace();
            int apex = Arrays.stream(fs).map(x->x.getTraceRef().absoluteApexId()).max(Comparator.comparingDouble(trace::intensity)).orElse(-1) - offset;
            merged.setTraceRef(new TraceRef(merged.getTraceRef().getTraceId(), offset, left, apex, right));
        }
        // merge isotopic features
        if (merged instanceof AlignedFeatures) {
            AlignedFeatures mf = (AlignedFeatures) merged;
            Int2ObjectOpenHashMap<List<AlignedIsotopicFeatures>> mapByMass = new Int2ObjectOpenHashMap<>();
            for (AbstractAlignedFeatures f : fs) {
                for (AlignedIsotopicFeatures i : mf.getIsotopicFeatures().get()) {
                    mapByMass.computeIfAbsent((int) (i.getAverageMass() * 100), ArrayList::new).add(i);
                }
            }
            List<AlignedIsotopicFeatures> isolist = new ArrayList<>();
            mapByMass.values().forEach(isos -> {
                // find corresponding isotope trace
                MergedTrace isoTrace = null;
                for (int j = 0; j < trace.getIsotopes().length; ++j) {
                    if (Math.abs(trace.getIsotopes()[j].averagedMz() - isos.get(0).getAverageMass()) < 0.1) {
                        isoTrace = trace.getIsotopes()[j];
                        break;
                    }
                }
                isolist.add((AlignedIsotopicFeatures) mergeNeighbouringFeatures(mergedSample, isoTrace, isos.toArray(AbstractAlignedFeatures[]::new), dbMapper));
            });
            mf.setIsotopicFeatures(isolist);
        }
        // merge features
        {
            final Long2ObjectOpenHashMap<ProjectedTrace> traces = new Long2ObjectOpenHashMap<>();
            final Long2ObjectOpenHashMap<ProcessedSample> samples = new Long2ObjectOpenHashMap<>();
            for (ProjectedTrace t : trace.getTraces()) {
                traces.put(dbMapper.sampleIndizes.get(t.getSampleId()), t);
                samples.put(dbMapper.sampleIndizes.get(t.getSampleId()), dbMapper.samples.get(t.getSampleId()));
            }
            Long2ObjectOpenHashMap<Feature>[] sample2feature = new Long2ObjectOpenHashMap[fs.length];
            LongOpenHashSet allRunIds = new LongOpenHashSet();
            for (int l=0; l < fs.length; ++l) {
                sample2feature[l] = new Long2ObjectOpenHashMap<>();
                for (Feature f : fs[l].getFeatures().get()) {
                    sample2feature[l].put(f.getRunId(), f);
                    allRunIds.add(f.getRunId());
                }
            }
            allRunIds.longStream().sorted().forEach(runId -> {
                List<Feature> features = Arrays.stream(sample2feature).map(x->x.get(runId)).filter(Objects::nonNull).toList();
                Feature tomerge = features.get(0);
                RawTraceRef ref = mergeTraceRef(features);
                tomerge.setTraceRef(ref);

                setGenericAttributes(
                        traces.get(runId).projected(trace.getMapping()),new TraceSegment(ref.absoluteApexId(), ref.getStart()+ref.getScanIndexOffsetOfTrace(), ref.getEnd()+ref.getScanIndexOffsetOfTrace()),
                        ref.getRawApex(), tomerge,tomerge.getCharge(), samples.get(runId), mergedSample
                );
            });
        }
        // merge ms data
        List<AbstractAlignedFeatures> featureWithData = Arrays.stream(fs).filter(x->x.getMSData().isPresent()).toList();
        MSData mergedMsData = mergeMsData(featureWithData, featureWithData.stream().map(x->x.getMSData().get()).toList());
        if (mergedMsData!=null) {
            merged.setMsData(mergedMsData);
            merged.setHasMs1(mergedMsData.getMergedMs1Spectrum()!=null);
            merged.setHasMsMs(mergedMsData.getMergedMSnSpectrum()!=null);
        }
        // set remaining attributes
        setGenericAttributes(trace,
                new TraceSegment(merged.getTraceRef().absoluteApexId(), merged.getTraceRef().getStart()+merged.getTraceRef().getScanIndexOffsetOfTrace(),
                        merged.getTraceRef().getEnd()+merged.getTraceRef().getScanIndexOffsetOfTrace()), merged.getTraceRef().absoluteApexId(), merged,
                merged.getCharge(), mergedSample, mergedSample);
        return merged;
    }

    private MSData mergeMsData(List<AbstractAlignedFeatures> features, List<MSData> tomerge) {
        if (tomerge.isEmpty()) return null;
        int bestPatternLen = 0;
        for (MSData d : tomerge) {
            bestPatternLen = Math.max(bestPatternLen,d.getIsotopePattern()!=null ? d.getIsotopePattern().size() : 0);
        }
        int best = -1;
        for (int l=0; l < tomerge.size(); ++l) {
            IsotopePattern iso = tomerge.get(l).getIsotopePattern();
            if (iso!=null && iso.size()>=bestPatternLen && (best<0 || features.get(l).getApexIntensity()>features.get(best).getApexIntensity())) {
                best=l;
            }
        }

        List<MergedMSnSpectrum> ms2 = tomerge.stream().filter(x -> x.getMsnSpectra() != null).flatMap(x -> x.getMsnSpectra().stream()).toList();
        MSData data = new MSData(tomerge.get(0).getAlignedFeatureId(), ms2,
            tomerge.get(best).getIsotopePattern(),tomerge.get(best).getMergedMs1Spectrum(),ms2.isEmpty() ? null : Spectrums.getNormalizedSpectrum(Spectrums.mergeSpectra(new Deviation(10), true, false,
                tomerge.stream().map(MSData::getMergedMSnSpectrum).filter(Objects::nonNull).toArray(SimpleSpectrum[]::new)), Normalization.Sum(1d))
                );
        if (data.getMergedMSnSpectrum()!=null && data.getMsnSpectra().isEmpty()) {
            throw new RuntimeException("Empty MSMS spectrum after merging");
        }
        return data;
    }

    private RawTraceRef mergeTraceRef(List<Feature> features) {
        int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, apex = 0;
        int rleft = Integer.MAX_VALUE, rright = Integer.MIN_VALUE, rapex = 0;
        double apexint = Float.NEGATIVE_INFINITY;
        for (int r=0; r < features.size(); ++r) {
            RawTraceRef R = features.get(r).getTraceReference().get();
            left = Math.min(left,R.getStart());
            right = Math.max(right, R.getEnd());
            rleft = Math.min(rleft, R.getRawStart());
            rright = Math.max(rright, R.getRawEnd());
            if (features.get(r).getApexIntensity() > apexint) {
                apexint = features.get(r).getApexIntensity();
                apex = R.getApex();
                rapex = R.getRawApex();
            }
        }
        RawTraceRef ref = features.get(0).getTraceReference().get();
        return new RawTraceRef(ref.getTraceId(), ref.getScanIndexOffsetOfTrace(), left, apex, right, rleft, rapex, rright, ref.getRawScanIndexOfset() );
    }

    private Optional<TraceSegment[]> checkForMergeOfNeighbouringFeatures(ProcessedSample sample, AlignedFeatures[] allFeatures, TraceSegment[] traceSegments) {
        TraceSegment[] newSegments = traceSegments.clone();
        final AlignedFeatures[] features = Arrays.stream(allFeatures).filter(x->x!=null).sorted(Comparator.comparingInt(x->x.getTraceRef().absoluteApexId())).toArray(AlignedFeatures[]::new);
        final double allowedDeviation = sample.getStorage().getAlignmentStorage().getStatistics().getExpectedRetentionTimeDeviation()*2;
        final ScanPointMapping mapping = sample.getMapping();
        final Int2IntOpenHashMap feature2tracesegment = new Int2IntOpenHashMap();
        for (int k=0; k < allFeatures.length; ++k) {
            if (allFeatures[k]!=null) {
                feature2tracesegment.put(allFeatures[k].getTraceRef().absoluteApexId(), k);
            }
        }
        for (int k=0; k < features.length-1; ++k) {
            final AlignedFeatures A = features[k];
            if (A==null)
                continue;
            final AlignedFeatures B = features[k+1];
            // check if both features are very close together
            final double rtDifference = Math.abs(mapping.getRetentionTimeAt(A.getTraceRef().absoluteApexId())-mapping.getRetentionTimeAt(B.getTraceRef().absoluteApexId()));
            if (k < features.length-2) {
                final AlignedFeatures C = features[k+2];
                // check if both features are very close together
                final double rtDifference2 = Math.abs(mapping.getRetentionTimeAt(C.getTraceRef().absoluteApexId())-mapping.getRetentionTimeAt(B.getTraceRef().absoluteApexId()));
                if (rtDifference2 < rtDifference)
                    continue;
            }

            if (rtDifference < allowedDeviation) {
                // check if all aligned features are completely separated
                Set<Long> left = new HashSet<>(A.getFeatures().orElse(Collections.emptyList()).stream().map(AbstractFeature::getRunId).collect(Collectors.toList()));
                Set<Long> right = B.getFeatures().orElse(Collections.emptyList()).stream().map(AbstractFeature::getRunId).collect(Collectors.toSet());
                left.retainAll(right);
                if (left.isEmpty()) {
                    // merge features
                    int ia = feature2tracesegment.get(A.getTraceRef().absoluteApexId());
                    int ib = feature2tracesegment.get(B.getTraceRef().absoluteApexId());
                    TraceSegment segmentA = traceSegments[ia];
                    TraceSegment segmentB = traceSegments[ib];
                    int apex = A.getApexIntensity()>B.getApexIntensity() ? segmentA.apex : segmentB.apex;
                    newSegments[ia] = new TraceSegment(apex, Math.min(segmentA.leftEdge, segmentB.leftEdge), Math.max(segmentA.rightEdge, segmentB.rightEdge));
                    newSegments[ib] = null;
                    features[k+1]=null; // do not double merge
                }
            }
        }
        final TraceSegment[] allSegments = Arrays.stream(newSegments).filter(x->x!=null).toArray(TraceSegment[]::new);
        if (allSegments.length < Arrays.stream(traceSegments).filter(x->x!=null).count()) return Optional.of(allSegments);
        else return Optional.empty();
    }

    private void reassignCharge(AlignedFeatures[] features) {
        for (AlignedFeatures f : features) {
            if (f==null) continue;
            MultipleCharges.Decision decision = MultipleCharges.checkForMultipleCharges(f);
            if (decision== MultipleCharges.Decision.LIKELY && Math.abs(f.getCharge())==1) {
                setCharge(f, f.getCharge()*2);
            } else if (decision == MultipleCharges.Decision.UNLIKELY && Math.abs(f.getCharge())!=1) {
                setCharge(f, (int)Math.signum(f.getCharge()));
            }
        }
    }
    private void setCharge(AlignedFeatures f, int charge ) {
        f.setCharge((byte)charge);
        f.getFeatures().ifPresent(gg->gg.forEach(g->g.setCharge((byte)charge)));
        f.getIsotopicFeatures().ifPresent(gg->gg.forEach(g->{
            g.setCharge((byte)charge);
            g.getFeatures().ifPresent(hh->hh.forEach(h->h.setCharge((byte)charge)));
        }));
    }

    private int estimateChargeFromIsotopes(ProcessedSample sample, MergedTrace mergedTrace) {
        double mz = mergedTrace.averagedMz();
        double minDist = 1d;
        for (int k=0; k < mergedTrace.getIsotopes().length; ++k) {
            double delta = mergedTrace.getIsotopes()[k].averagedMz() - mz;
            minDist = Math.min(delta, minDist);
            mz = mergedTrace.getIsotopes()[k].averagedMz();
        }
        int charge = (int)Math.round(1.1/minDist);
        if (charge==0) {
            throw new RuntimeException("Problem with charge detection occured.");
        }
        return charge*sample.getPolarity();
    }

    private void extractMs1(DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTraceParent, TraceSegment[] traceSegmentsParent, TraceSegment[][] projectedSegmentsParent, AlignedFeatures[] featuresParent, MergeTraceId[] dbIsotopeIds) {
        SampleStats stats = mergedSample.getStorage().getStatistics();
        final Int2IntOpenHashMap sample2idx = new Int2IntOpenHashMap();
        sample2idx.defaultReturnValue(-1);
        for (int k=0; k < projectedSegmentsParent.length; ++k) sample2idx.put(mergedTraceParent.getSamples()[k].getUid(), k);
        ArrayList<AlignedIsotopicFeatures>[] isotopicFeatures = new ArrayList[featuresParent.length];
        SimpleMutableSpectrum[] isotopePatterns = new SimpleMutableSpectrum[featuresParent.length];
        for (int i=0; i < featuresParent.length; ++i) {
            isotopicFeatures[i] = new ArrayList<>();
            if (featuresParent[i]!=null) {
                isotopePatterns[i] = new SimpleMutableSpectrum();
                isotopePatterns[i].addPeak(mergedTraceParent.averagedMz(), 1d);
                featuresParent[i].setHasMs1(true);
            }
        }


        for (int isotopePeak = 0; isotopePeak < mergedTraceParent.getIsotopes().length; ++isotopePeak) {
            MergedTrace isotope = mergedTraceParent.getIsotopes()[isotopePeak];
            // we have multiple options here, we can just keep the segments we have from monoisotopic, or we again
            // use microalignments. I would first go for keeping the segments and see how it goes.
            for (int segmentIndex=0; segmentIndex < traceSegmentsParent.length; ++segmentIndex) {
                if (featuresParent[segmentIndex]==null) continue;
                // check if there IS something in this segment
                TraceSegment mergedSegment = traceSegmentsParent[segmentIndex];
                double sum=0d;
                int adjustedMergedApex = mergedSegment.apex;
                double apexInt = Double.NEGATIVE_INFINITY;
                for (int i=mergedSegment.leftEdge; i <= mergedSegment.rightEdge; ++i) {
                    if (isotope.inRange(i)) {
                        sum += isotope.intensity(i);
                        if (isotope.intensity(i)>apexInt) {
                            adjustedMergedApex = i;
                            apexInt = isotope.intensity(i);
                        }
                    }
                }

                if (sum >= stats.noiseLevel(mergedSegment.apex)) {
                    // generate isotope segment
                    AlignedIsotopicFeatures iso = new AlignedIsotopicFeatures();
                    iso.setFeatures(new ArrayList<>());
                    final TraceSegment t = new TraceSegment(adjustedMergedApex, Math.max(isotope.startId(), mergedSegment.leftEdge), Math.min(isotope.endId(), mergedSegment.rightEdge));
                    if (t.rightEdge-t.leftEdge<=1) continue;
                    {
                        int o=isotope.startId();
                        iso.setTraceRef(new TraceRef(dbIsotopeIds[isotopePeak].mergeTrace, o, t.leftEdge-o, adjustedMergedApex-o, t.rightEdge-o));
                    }
                    featuresParent[segmentIndex].getIsotopicFeatures().get().add(iso);
                    setGenericAttributes(isotope, t, adjustedMergedApex, iso, featuresParent[segmentIndex].getCharge(), mergedSample, mergedSample);

                    double weightedAverageIntensity = 0d;
                    double weighting = 0d;
                    for (int isotopeSampleIndex = 0; isotopeSampleIndex < isotope.getTraces().length; ++isotopeSampleIndex) {
                        // it is possible that the raw samples of the isotope peak are not 1:1 identical with the raw samples
                        // of the monoisotopic peak (e.g. maybe some samples are missing)
                        // thus we have to remap the indizes to map the raw segments correctly
                        // TODO: what do we do if the isotope is found in a sample but the monoisotopic is not? Current Workaround: ignore them
                        final int parentSampleIndex = sample2idx.get(isotope.getSamples()[isotopeSampleIndex].getUid());
                        if (parentSampleIndex < 0 ) {
                            LoggerFactory.getLogger(PickFeaturesAndImportToSirius.class).warn("Isotope peak is found in sample, but there is no monoisotopic peak in same sample.");
                            continue;
                        }

                        TraceSegment projectedSegment = projectedSegmentsParent[parentSampleIndex][segmentIndex];
                        ProjectedTrace subTrace = isotope.getTraces()[isotopeSampleIndex];
                        ProcessedSample subSample = isotope.getSamples()[isotopeSampleIndex];
                        // mergedSegment is the part of the merged trace that we are looking at currently
                        // projectedSegment is the part of the RAW trace that belongs to the current sample and mergedSegment
                        // subTrace is the part of the RAW trace of the ISOTOPE that belongs to the current sample and mergedSegment
                        // basically we say here that:
                        // - we only look for an isotope in sample X if we also see the monoisotopic peak in sample X
                        // - also, the trace of the isotope has to overlap with merged segment of the monoisotopic peak
                        if (projectedSegment!=null && projectedSegment.overlaps(mergedSegment) && projectedSegment.overlaps(subTrace.projected(isotope.getMapping()))) {
                            projectedSegment = new TraceSegment(Math.max(subTrace.getProjectedStartId(), projectedSegment.leftEdge), Math.max(subTrace.getProjectedStartId(), projectedSegment.leftEdge), Math.min(subTrace.getProjectedEndId(), projectedSegment.rightEdge));

                            int adjApexProj=0;
                            double apexIntensity = Double.NEGATIVE_INFINITY;
                            for (int i=projectedSegment.leftEdge; i < projectedSegment.rightEdge; ++i) {

                                if (subTrace.inProjectedRange(i) && subTrace.projectedIntensity(i) > apexIntensity) {
                                    apexIntensity = subTrace.projectedIntensity(i);
                                    adjApexProj = i;
                                }
                            }
                            if (apexIntensity<=0) continue;

                            int o=subTrace.getProjectedStartId();
                            projectedSegment = new TraceSegment(adjApexProj, projectedSegment.leftEdge, projectedSegment.rightEdge);

                            if (projectedSegment.rightEdge-projectedSegment.leftEdge <= 2) continue;

                            // we have to repeat this with the source traces
                            Feature feature = new Feature();
                            iso.getFeatures().get().add(feature);
                            int from = Math.max(subTrace.getRawStartId(), subSample.getScanPointInterpolator().reverseMapLowerIndex(projectedSegment.leftEdge));
                            int to = Math.min(subTrace.getRawEndId(), subSample.getScanPointInterpolator().reverseMapLargerIndex(projectedSegment.rightEdge));
                            int newRawApex = getAdjustedApex(subTrace.raw(subSample.getMapping()), from, to);
                            int off = subTrace.getRawStartId();
                            int off2 = subTrace.getProjectedStartId();
                            setGenericAttributes(subTrace.projected(mergedSample.getMapping()), projectedSegment, newRawApex, feature, featuresParent[segmentIndex].getCharge(), subSample, mergedSample);
                            feature.setTraceRef(new RawTraceRef(
                                    dbIsotopeIds[isotopePeak].rawTraces[isotopeSampleIndex],
                                    off2,
                                    projectedSegment.leftEdge-off2,
                                    projectedSegment.apex-off2,
                                    projectedSegment.rightEdge-off2,
                                    from-off,
                                    newRawApex-off,
                                    to-off,
                                    off
                                    ));

                            // add intensity to isotope peak
                            final int originalApex = projectedSegmentsParent[parentSampleIndex][segmentIndex].apex;
                            float peakIntensity, parentIntensity;
                            if (mergedTraceParent.getTraces()[parentSampleIndex].inProjectedRange(adjApexProj) && subTrace.inProjectedRange(originalApex)) {
                                peakIntensity = subTrace.projectedIntensity(adjApexProj) + subTrace.projectedIntensity(originalApex);
                                parentIntensity = mergedTraceParent.getTraces()[parentSampleIndex].projectedIntensity(adjApexProj) + mergedTraceParent.getTraces()[parentSampleIndex].projectedIntensity(originalApex);
                            } else {
                                peakIntensity = subTrace.projectedIntensity(adjApexProj);
                                parentIntensity = mergedTraceParent.getTraces()[parentSampleIndex].projectedIntensity(originalApex);
                            }



                            final double intensityRatio = peakIntensity / parentIntensity;
                            weightedAverageIntensity += intensityRatio * parentIntensity;
                            weighting += parentIntensity;
                        }
                    }
                    if (weighting>0) {
                        weightedAverageIntensity /= weighting;
                        isotopePatterns[segmentIndex].addPeak(isotope.averagedMz(), weightedAverageIntensity);
                    }

                }
            }
        }
        for (int i=0; i <featuresParent.length; ++i) {
            if (featuresParent[i]!=null) {
                MSData data = featuresParent[i].getMSData().orElseGet(MSData::new);
                SimpleSpectrum isotopePattern = Spectrums.getNormalizedSpectrum(isotopePatterns[i], Normalization.Sum);
                data.setIsotopePattern(new IsotopePattern(isotopePattern, IsotopePattern.Type.AVERAGE));
                data.setMergedMs1Spectrum(new SimpleSpectrum(isotopePatterns[i]));
                featuresParent[i].setMsData(data);
            }
        }
    }

    private int getAdjustedApex(Trace trace, int from, int to) {
        double intens=Double.NEGATIVE_INFINITY;
        int ap=from;
        for (int k=from; k <= to; ++k) {
            if (trace.inRange(k) && trace.intensity(k)>intens) {
                ap=k;
                intens = trace.intensity(k);
            }
        }
        return ap;
    }


    private void setGenericAttributes(Trace trace, TraceSegment segment, int rawApex, AbstractFeature feature, int charge, ProcessedSample rawSample, ProcessedSample projectedSample) {
        float apexIntensity = trace.intensity(segment.apex);
        feature.setSnr(apexIntensity / (double)rawSample.getStorage().getStatistics().noiseLevel(rawApex));
        feature.setApexIntensity((double)apexIntensity);
        feature.setCharge((byte)charge);
        feature.setApexMass(trace.mz(segment.apex));
        feature.setAverageMass(trace.averagedMz());
        feature.setRetentionTime(new RetentionTime(trace.retentionTime(segment.leftEdge), trace.retentionTime(segment.rightEdge), trace.retentionTime(segment.apex)));
        feature.setRunId(rawSample.getRun().getRunId());
        feature.setFwhm(calcFwhm(segment, trace, projectedSample.getMapping()));
        feature.setAreaUnderCurve(calcAUC(segment, trace, projectedSample.getMapping()));
    }

    private double calcAUC(TraceSegment segment, Trace trace, ScanPointMapping mapping) {
        double auc = 0;
        for (int i = segment.leftEdge; i < segment.rightEdge; i++) {
            // trapezoid equation: (f(a) + f(b)) / 2 * (b - a)
            auc += 0.5 * (trace.intensity(i) + trace.intensity(i + 1)) * (mapping.getRetentionTimeAt(i + 1) - mapping.getRetentionTimeAt(i));
        }
        return auc;
    }

    private double calcFwhm(TraceSegment segment, Trace trace, ScanPointMapping mapping) {
        int apex = segment.apex;
        double threshold = trace.intensity(apex)/2d;
        int i = apex-1;
        int leftEdge = -1;
        while (trace.inRange(i)) {
            if (trace.intensity(i) < trace.intensity(i+1)) {
                if (leftEdge < 0 && trace.intensity(i)<= threshold) {
                    leftEdge=i;
                    if (i <= segment.leftEdge) break; // we are done
                }
            } else {
                leftEdge = -1; // erase information
            }
            --i;
        }
        if (leftEdge<0) leftEdge = segment.leftEdge;
        i = apex+1;
        int rightEdge = -1;
        while (trace.inRange(i)) {
            if (trace.intensity(i) < trace.intensity(i-1)) {
                if (rightEdge < 0 && trace.intensity(i)<= threshold) {
                    rightEdge=i;
                    if (i >= segment.rightEdge) break; // we are done
                }
            } else {
                rightEdge = -1; // erase information
            }
            ++i;
        }
        if (rightEdge<0) rightEdge = segment.rightEdge;

        return mapping.getRetentionTimeAt(rightEdge)-mapping.getRetentionTimeAt(leftEdge);
    }

    public MergeTraceId importMergedTraceWithoutIsotopes(SiriusDatabaseAdapter adapter, DbMapper dbMapper, MergedTrace mergedTrace) throws IOException {
        // import each individual trace
        long[] traceIds = new long[mergedTrace.getTraces().length];
        for (int k=0; k < traceIds.length; ++k) {
            traceIds[k] = importTrace(adapter, dbMapper, mergedTrace.getTraces()[k]);
        }
        de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace merged = new de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace();
        merged.setMz(mergedTrace.getMzArrayList());
        merged.setIntensities(mergedTrace.getIntensityArrayList());
        merged.setScanIndexOffset(mergedTrace.startId());
        merged.setAverageMz(mergedTrace.averagedMz());
        merged.setRunId(dbMapper.sampleIndizes.get(dbMapper.mergedUiD));
        adapter.importTrace(merged);
        return new MergeTraceId(merged.getMergedTraceId(), traceIds);
    }

    private void removeMergedTrace(SiriusDatabaseAdapter adapter, MergeTraceId tid, MergeTraceId[] isotopes) throws IOException {
        adapter.removeMergedTrace(tid.mergeTrace);
        for (long id : tid.rawTraces) {
            adapter.removeSourceTrace(id);
        }
        for (MergeTraceId mergeTraceId : isotopes) {
            adapter.removeMergedTrace(mergeTraceId.mergeTrace);
            for (long id : mergeTraceId.rawTraces) {
                adapter.removeSourceTrace(id);
            }
        }
    }


    public void assignMs2(DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] traceSegments, TraceSegment[][] rawSegments, AlignedFeatures[] features) {
        // generate MsMs
        MergedSpectrum[][] ms2spectraPerSegment = ms2MergeStrategy.assignMs2(mergedSample, mergedTrace, traceSegments, rawSegments);

        final Long2ObjectOpenHashMap<Ms2Pointer> ms2Pointers = new Long2ObjectOpenHashMap<>();
        // generate features for each segment
        for (int i=0; i < traceSegments.length; ++i) {
            if (features[i]==null) continue;
            AlignedFeatures feature = features[i];
            MergedSpectrum[] ms2Spectra = ms2spectraPerSegment[i];
            if (ms2Spectra==null) continue;

            MergedMSnSpectrum[] msn = new MergedMSnSpectrum[ms2Spectra.length];
            for (int k=0; k < ms2Spectra.length; ++k) {
                final int K=k;
                MergedSpectrum m = ms2Spectra[k];
                int[] origSampleIds = m.getSampleIds();
                long[] sampleIds = Arrays.stream(origSampleIds).mapToLong(idx->dbMapper.samples.get(idx).getRun().getRunId()).toArray();
                ms2Pointers.clear();
                for (int j=0; j < sampleIds.length; ++j) {
                    ProcessedSample smp = dbMapper.samples.get(origSampleIds[j]);
                    final int pidx;
                    Ms2SpectrumHeader header = m.getHeaders()[j];
                    final int roundDown = smp.getScanPointInterpolator().lowerIndex(header.getParentId());
                    final int roundUp = smp.getScanPointInterpolator().largerIndex(header.getParentId());
                    if (Math.abs(traceSegments[i].apex - roundDown) < Math.abs(traceSegments[i].apex - roundUp)) {
                        pidx = roundDown;
                    } else pidx = roundUp;
                    Ms2Pointer ms2Pointer = ms2Pointers.computeIfAbsent(sampleIds[j], idx -> new Ms2Pointer(idx, new IntArrayList(),
                            new IntArrayList(), new IntArrayList()));
                    ms2Pointer.ms2scans().add(header.getScanId());
                    ms2Pointer.rawscans().add(header.getParentId());
                    ms2Pointer.projectedScans().add(pidx);
                }
                msn[k] = new MergedMSnSpectrum(m.getHeaders()[0].getMsLevel(),
                        mergedSample.getPolarity(), // TODO: what is with multiple charged compounds? Double check!!!
                        m.getCollisionEnergies()[0],
                        m.getAveragedPrecursorMz(),
                        m.getIsolationWindows(),
                        sampleIds,
                        Arrays.stream(sampleIds).mapToObj(x->ms2Pointers.get(x).ms2scans().toIntArray()).toArray(int[][]::new),
                        Arrays.stream(sampleIds).mapToObj(x->ms2Pointers.get(x).rawscans().toIntArray()).toArray(int[][]::new),
                        Arrays.stream(sampleIds).mapToObj(x->ms2Pointers.get(x).projectedScans().toIntArray()).toArray(int[][]::new),
                        Arrays.stream(m.getHeaders()).mapToDouble(Ms2SpectrumHeader::getPrecursorMz).toArray(),
                        m.getChimericPollutionRatio(),
                        new SimpleSpectrum(m)
                );

                ////////////////////
                // double check
                if (false) {
                    for (int l = 0; l < msn[k].getMs2ScanIds().length; ++l) {
                        final int L = l;
                        int[] vec = msn[k].getProjectedPrecursorScanIds()[l];
                        for (int scanId : vec) {
                            int offset = feature.getTraceRef().getScanIndexOffsetOfTrace();
                            int startId = feature.getTraceRef().getStart() + offset;
                            int endId = feature.getTraceRef().getEnd() + offset;
                            if (scanId < startId || scanId > endId) {
                                LoggerFactory.getLogger(PickFeaturesAndImportToSirius.class).warn("MS2 outside of feature! mz = " + mergedTrace.averagedMz() + ", rt = " + mergedTrace.getMapping().getRetentionTimeAt(feature.getTraceRef().absoluteApexId()));
                                if (features.length <= 3 && (scanId - endId) > 10) {
                                    System.err.println("Gotcha!");
                                    ms2MergeStrategy.assignMs2(mergedSample, mergedTrace, traceSegments, rawSegments);
                                    segmentationStrategy.featureFinding(new PersistentHomology(), mergedSample, mergedTrace);
                                }
                                // check if that also happens for subfeatures
                                Feature subFeature = feature.getFeatures().get().stream().filter(x -> x.getRunId() == msn[K].getSampleIds()[L]).findFirst().get();
                                RawTraceRef ref = subFeature.getTraceReference().get();
                                if (scanId < ref.getStart() + ref.getScanIndexOffsetOfTrace() || scanId > ref.getEnd() + ref.getScanIndexOffsetOfTrace()) {
                                    LoggerFactory.getLogger(PickFeaturesAndImportToSirius.class).warn("MS2 also outside of its subfeature!");
                                    if (features.length <= 3) {
                                        System.err.println("Gotcha!");
                                    }
                                }

                            }
                        }
                    }
                    ///////////////////
                }
            }
            feature.getMSData().get().setMsnSpectra(Arrays.asList(msn));
            if (msn.length==0) return;
            SimpleSpectrum merged;
            if (msn.length>1) {
                merged = Spectrums.mergeSpectra(new Deviation(10), true, false, Arrays.stream(msn).map(MergedMSnSpectrum::getPeaks).toArray(SimpleSpectrum[]::new));
            } else merged = msn[0].getPeaks();
            feature.getMSData().get().setMergedMSnSpectrum(Spectrums.getNormalizedSpectrum(merged, Normalization.Sum));
            feature.setHasMsMs(true);
            if (feature.getMSData().get().getMergedMSnSpectrum()!=null && feature.getMSData().get().getMsnSpectra().isEmpty()) {
                throw new RuntimeException("WTF?");
            }
        }
    }

    record Ms2Pointer(long sampleId, IntArrayList ms2scans, IntArrayList rawscans, IntArrayList projectedScans) {

    }

    record MergeTraceId(long mergeTrace, long[] rawTraces) {
    }

    public static class DbMapper {
        int mergedUiD;
        Int2LongOpenHashMap sampleIndizes = new Int2LongOpenHashMap();
        Int2ObjectOpenHashMap<ProcessedSample> samples = new Int2ObjectOpenHashMap<ProcessedSample>();

    }
}
