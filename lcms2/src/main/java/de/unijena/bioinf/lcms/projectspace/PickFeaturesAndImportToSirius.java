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
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.utils.MultipleCharges;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.RawTraceRef;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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
    public AlignedFeatures[] importMergedTrace(TraceSegmentationStrategy traceSegmenter, SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTrace, boolean allowMs1Only) throws IOException {
        // import each individual trace
        MergeTraceId tid = importMergedTraceWithoutIsotopes(adapter, dbMapper, mergedTrace);
        // now import isotopes
        MergeTraceId[] isotopes = new MergeTraceId[mergedTrace.getIsotopes().length];
        for (int k=0; k < isotopes.length; ++k) {
            isotopes[k] = importMergedTraceWithoutIsotopes(adapter, dbMapper, mergedTrace.getIsotopes()[k]);
        }
        // now extract the compounds
        AlignedFeatures[] features = extractCompounds(traceSegmenter, adapter, dbMapper, mergedSample, mergedTrace, tid, isotopes, allowMs1Only);
        if (features.length == 0) {
            removeMergedTrace(adapter, tid, isotopes);
        }
        return features;
    }

    private AlignedFeatures[] extractCompounds(TraceSegmentationStrategy traceSegmenter, SiriusDatabaseAdapter adapter, DbMapper dbMapper, ProcessedSample mergedSample, MergedTrace mergedTrace, MergeTraceId dbId, MergeTraceId[] dbIsotopeIds, boolean allowMs1Only) {
        // first we use segmentation to split the trace into segments
        // when then apply this segments to all traces and isotopes
        TraceSegment[] traceSegments = segmentationStrategy.extractMergedSegments(traceSegmenter, mergedSample, mergedTrace);

        // if there is no compound found in the trace...
        if (traceSegments.length==0) return new AlignedFeatures[0];

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

        final int charge = estimateChargeFromIsotopes(mergedSample, mergedTrace);

        // generatate monoisotopic features for each segment
        AlignedFeatures[] features = new AlignedFeatures[traceSegments.length];
        for (int fid=0; fid < traceSegments.length; ++fid) {
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
        // assign ms data
        assignMs2(dbMapper, mergedSample, mergedTrace, traceSegments, rawSegments, features);
        // assign ms1 data
        extractMs1(dbMapper, mergedSample, mergedTrace, traceSegments, rawSegments, features, dbIsotopeIds);
        // reassign charge
        reassignCharge(features);
        // store feature
        for (int i = 0; i < features.length; i++) {
            if (features[i]==null) continue;
            if (!allowMs1Only && !features[i].isHasMsMs()) {
                features[i] = null;
                continue;
            }
            try {
                if (!adapter.importAlignedFeature(features[i])) features[i] = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        features = Arrays.stream(features).filter(Objects::nonNull).toArray(AlignedFeatures[]::new);
        return features;
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
            for (int traceIndex=0; traceIndex < traceSegmentsParent.length; ++traceIndex) {
                if (featuresParent[traceIndex]==null) continue;
                // check if there IS something in this segment
                TraceSegment s = traceSegmentsParent[traceIndex];
                double sum=0d;
                int adjustedMergedApex = s.apex;
                double apexInt = Double.NEGATIVE_INFINITY;
                for (int i=s.leftEdge; i <= s.rightEdge; ++i) {
                    if (isotope.inRange(i)) {
                        sum += isotope.intensity(i);
                        if (isotope.intensity(i)>apexInt) {
                            adjustedMergedApex = i;
                            apexInt = isotope.intensity(i);
                        }
                    }
                }

                if (sum >= stats.noiseLevel(s.apex)) {
                    // generate isotope segment
                    AlignedIsotopicFeatures iso = new AlignedIsotopicFeatures();
                    iso.setFeatures(new ArrayList<>());
                    final TraceSegment t = new TraceSegment(adjustedMergedApex, Math.max(isotope.startId(), s.leftEdge), Math.min(isotope.endId(), s.rightEdge));
                    if (t.rightEdge-t.leftEdge<=1) continue;
                    {
                        int o=isotope.startId();
                        iso.setTraceRef(new TraceRef(dbIsotopeIds[isotopePeak].mergeTrace, o, t.leftEdge-o, adjustedMergedApex-o, t.rightEdge-o));
                    }
                    featuresParent[traceIndex].getIsotopicFeatures().get().add(iso);
                    setGenericAttributes(isotope, t, adjustedMergedApex, iso, featuresParent[traceIndex].getCharge(), mergedSample, mergedSample);

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

                        TraceSegment projectedSegment = projectedSegmentsParent[parentSampleIndex][traceIndex];
                        ProjectedTrace subTrace = isotope.getTraces()[isotopeSampleIndex];
                        ProcessedSample subSample = isotope.getSamples()[isotopeSampleIndex];
                        if (projectedSegment!=null) {

                            projectedSegment = new TraceSegment(projectedSegment.apex, Math.max(subTrace.getProjectedStartId(), s.leftEdge), Math.min(subTrace.getProjectedEndId(), s.rightEdge));

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
                            setGenericAttributes(subTrace.projected(mergedSample.getMapping()), projectedSegment, newRawApex, feature, featuresParent[traceIndex].getCharge(), subSample, mergedSample);
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
                            final int originalApex = projectedSegmentsParent[parentSampleIndex][traceIndex].apex;
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
                        isotopePatterns[traceIndex].addPeak(isotope.averagedMz(), weightedAverageIntensity);
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
                MergedSpectrum m = ms2Spectra[k];
                int[] origSampleIds = m.getSampleIds();
                long[] sampleIds = Arrays.stream(origSampleIds).mapToLong(idx->dbMapper.samples.get(idx).getRun().getRunId()).toArray();
                ms2Pointers.clear();
                for (int j=0; j < sampleIds.length; ++j) {
                    ProcessedSample smp = dbMapper.samples.get(origSampleIds[j]);
                    Ms2SpectrumHeader header = m.getHeaders()[j];
                    Ms2Pointer ms2Pointer = ms2Pointers.computeIfAbsent(sampleIds[j], idx -> new Ms2Pointer(idx, new IntArrayList(),
                            new IntArrayList(), new IntArrayList()));
                    ms2Pointer.ms2scans().add(header.getScanId());
                    ms2Pointer.rawscans().add(header.getParentId());
                    ms2Pointer.projectedScans().add(smp.getScanPointInterpolator().roundIndex(header.getParentId()));
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
            }
            feature.getMSData().get().setMsnSpectra(Arrays.asList(msn));
            if (msn.length==0) return;
            SimpleSpectrum merged;
            if (msn.length>1) {
                merged = Spectrums.mergeSpectra(new Deviation(10), true, false, Arrays.stream(msn).map(MergedMSnSpectrum::getPeaks).toArray(SimpleSpectrum[]::new));
            } else merged = msn[0].getPeaks();
            feature.getMSData().get().setMergedMSnSpectrum(Spectrums.getNormalizedSpectrum(merged, Normalization.Sum));
            feature.setHasMsMs(true);
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
