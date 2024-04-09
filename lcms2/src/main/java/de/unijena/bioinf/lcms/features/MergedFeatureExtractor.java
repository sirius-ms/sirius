package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.run.SampleStats;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MergedFeatureExtractor implements MergedFeatureExtractionStrategy{

    @Override
    public Iterator<AlignedFeatures> extractFeatures(ProcessedSample mergedSample, MergedTrace mergedTrace, Ms2MergeStrategy ms2MergeStrategy, IsotopePatternExtractionStrategy isotopePatternExtractionStrategy, Long2LongMap trace2trace, Long2LongMap sourceTrace2trace, Int2ObjectMap<ProcessedSample> idx2sample) {
        ProcessedSample[] samplesInTrace = new ProcessedSample[mergedTrace.getSampleIds().size()];
        for (int i = 0; i < mergedTrace.getSampleIds().size(); ++i) {
            samplesInTrace[i] = idx2sample.get(mergedTrace.getSampleIds().getInt(i));
        }

        final Trace mTrace = mergedTrace.toTrace(mergedSample);
        final SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mTrace).stream().filter(x->mTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  && x.leftEdge < x.rightEdge - 1).toArray(TraceSegment[]::new);
        if (traceSegments.length == 0)
            return Collections.emptyIterator();

        Arrays.sort(traceSegments, Comparator.comparingDouble(a -> a.apex));

        double[][] intervals = new double[traceSegments.length][2];
        for (int i = 0; i < traceSegments.length; i++) {
            intervals[i][0] = mTrace.retentionTime(traceSegments[i].leftEdge);
            intervals[i][1] = mTrace.retentionTime(traceSegments[i].rightEdge);
        }

        AlignedFeatures[] monoisotopic = extractAlignedFeatures(traceSegments, mergedSample, samplesInTrace, mergedTrace, ms2MergeStrategy, trace2trace, sourceTrace2trace, AlignedFeatures::new, AlignedFeatures[]::new);

        if (!mergedTrace.getIsotopeUids().isEmpty()) {
            AlignedIsotopicFeatures[][] isotopicFeatures = new AlignedIsotopicFeatures[monoisotopic.length][mergedTrace.getIsotopeUids().size()];

            for (int j = 0; j < mergedTrace.getIsotopeUids().size(); j++) {
                MergedTrace isoTrace = mergedSample.getStorage().getMergeStorage().getMerged(mergedTrace.getIsotopeUids().getInt(j));
                ProcessedSample[] isoSamplesInTrace = new ProcessedSample[isoTrace.getSampleIds().size()];
                for (int k = 0; k < isoTrace.getSampleIds().size(); ++k) {
                    isoSamplesInTrace[k] = idx2sample.get(isoTrace.getSampleIds().getInt(k));
                }

                TraceSegment[] isoTraceSegments = assignIntervalstoTrace(mergedSample, isoTrace, intervals);
                AlignedIsotopicFeatures[] isoFeatures = extractAlignedFeatures(isoTraceSegments, mergedSample, isoSamplesInTrace, isoTrace, ms2MergeStrategy, trace2trace, sourceTrace2trace, AlignedIsotopicFeatures::new, AlignedIsotopicFeatures[]::new);
                for (int i = 0; i < monoisotopic.length; i++) {
                    isotopicFeatures[i][j] = isoFeatures[i];
                }
            }

            for (int i = 0; i < monoisotopic.length; i++) {
                if (monoisotopic[i] == null)
                    continue;
                List<AlignedIsotopicFeatures> isotopicFeaturesList = new ArrayList<>();
                for (int j = 0; j < isotopicFeatures[i].length; j++) {
                    if (isotopicFeatures[i][j] == null)
                        break;
                    isotopicFeaturesList.add(isotopicFeatures[i][j]);
                }
                monoisotopic[i].setIsotopicFeatures(isotopicFeaturesList);
                final IsotopePattern isotopePattern = isotopePatternExtractionStrategy.extractIsotopePattern(monoisotopic[i], isotopicFeaturesList);
                monoisotopic[i].getMSData().ifPresent(data -> data.setIsotopePattern(isotopePattern));
            }
        }
        for (AlignedFeatures f : monoisotopic) {
            if (f!=null) detectChargeState(f, mergedSample);
        }
        return Arrays.stream(monoisotopic).filter(Objects::nonNull).iterator();
    }

    private void detectChargeState(AlignedFeatures features, ProcessedSample sample) {
        // a priori we assume single charge
        int polarity = sample.getPolarity()>0 ? 1 : -1;

        // do we have isotopes?
        final int[] counter = new int[4];

        if (features.getIsotopicFeatures().isPresent()) {
            List<AlignedIsotopicFeatures> iso = features.getIsotopicFeatures().get();
            for (int k = 0; k < iso.size(); ++k) {
                for (int ch = 1; ch < counter.length; ++ch) {
                    final double min = de.unijena.bioinf.lcms.isotopes.IsotopePattern.getMinimumMzFor(features.getAverageMass(), k + 1, ch);
                    final double max = de.unijena.bioinf.lcms.isotopes.IsotopePattern.getMaximumMzFor(features.getAverageMass(), k + 1, ch);
                    if (iso.get(k).getAverageMass() >= min && iso.get(k).getAverageMass() <= max) {
                        counter[ch] += (4 - ch);
                        break;
                    }
                }
            }
        }
        // TODO: use other detection methods such as MS/MS
        int bestExplanation = Arrays.stream(counter).max().orElse(0);
        for (int j=1; j < counter.length; ++j) {
            if (counter[j]>=bestExplanation) {
                features.setCharge((byte)(j*polarity));
                for (Feature f : features.getFeatures().get()) {
                    f.setCharge((byte)(j*polarity));
                }
                return;
            }
        }
    }

    private <F extends AbstractAlignedFeatures> F[] extractAlignedFeatures(
            TraceSegment[] traceSegments, ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace mergedTrace, Ms2MergeStrategy ms2MergeStrategy, Long2LongMap trace2trace, Long2LongMap sourceTrace2trace, Supplier<F> featureSupplier, IntFunction<F[]> featureArraySupplier
    ) {
        Trace mTrace = mergedTrace.toTrace(mergedSample);
        final SampleStats stats = mergedSample.getStorage().getStatistics();

        // segments for each individual trace
        TraceSegment[][] individualSegments = new TraceSegment[mergedTrace.getSampleIds().size()][];
        for (int k=0; k < individualSegments.length; ++k) {
            ProcessedSample sample = samplesInTrace[k];
            assert sample.getUid() == mergedTrace.getSampleIds().getInt(k);
            individualSegments[k] = assignSegmentsToIndividualTrace(sample, traceSegments, mergedSample.getStorage().getMergeStorage().getTrace(sample.getMapping(), mergedTrace.getTraceIds().getInt(k)) );
        }

        final Int2ObjectOpenHashMap<ProcessedSample> uid2sample = new Int2ObjectOpenHashMap<>();
        final MSData[] msData = new MSData[traceSegments.length];
        for (int i = 0; i < msData.length; i++)
            msData[i] = MSData.builder().build();

        for (ProcessedSample sample : samplesInTrace)
            uid2sample.put(sample.getUid(), sample);

        ms2MergeStrategy.assignMs2(mergedSample, mergedTrace, traceSegments, uid2sample, (mergedTrace1, segment, index, mergedSpectrum, spectra) -> {
            List<CollisionEnergy> ce = new ArrayList<>();
            List<IsolationWindow> iw = new ArrayList<>();
            DoubleList pmz = new DoubleArrayList();
            for (int i = 0; i < spectra.size(); i++) {
                if (spectra.get(i).getHeader().getEnergy().isPresent()) {
                    ce.add(spectra.get(i).getHeader().getEnergy().get());
                }
                if (spectra.get(i).getHeader().getIsolationWindow().isPresent()) {
                    iw.add(spectra.get(i).getHeader().getIsolationWindow().get());
                }
                pmz.add(spectra.get(i).getHeader().getPrecursorMz());
            }

            MergedMSnSpectrum mergedMSnSpectrum = MergedMSnSpectrum.of(new SimpleSpectrum(mergedSpectrum), ce.toArray(CollisionEnergy[]::new), iw.toArray(IsolationWindow[]::new), pmz.toDoubleArray());

            msData[index].setMergedMSnSpectrum(mergedMSnSpectrum);
            msData[index].setMsnSpectra(spectra.stream().map(s -> new MutableMs2Spectrum(s.getFilteredSpectrum(), s.getHeader().getPrecursorMz(), s.getHeader().getEnergy().get(), 2)).toList());
        });


        F[] featureArr = featureArraySupplier.apply(traceSegments.length);

        for (int i = 0; i < traceSegments.length; i++) {
            if (traceSegments[i] == null)
                continue;

            F alignedFeatures = featureSupplier.get();
            alignedFeatures.setRunId(mergedSample.getRun().getRunId());
            alignedFeatures.setIonType(PrecursorIonType.unknown(mergedSample.getPolarity()));

            buildFeature(mergedTrace.getUid(), mTrace, traceSegments[i], stats, trace2trace, alignedFeatures);

            final int o = mTrace.startId();
            msData[i].setMergedMs1Spectrum(Spectrums.subspectrum(new SimpleSpectrum(mergedTrace.getMz(), mergedTrace.getInts()), traceSegments[i].leftEdge - o, traceSegments[i].rightEdge - o));
            if (msData[i].getMergedMs1Spectrum() != null || msData[i].getMergedMSnSpectrum() != null || msData[i].getIsotopePattern() != null)
                alignedFeatures.setMsData(msData[i]);

            List<Feature> childFeatures = new ArrayList<>();
            for (int k=0; k < individualSegments.length; ++k) {
                // FIXME invalid segments
                if (individualSegments[k][i] == null || individualSegments[k][i].leftEdge >= individualSegments[k][i].rightEdge)
                    continue;

                int childTraceId = mergedTrace.getTraceIds().getInt(k);
                int childSampleId = mergedTrace.getSampleIds().getInt(k);
                ProcessedSample sample = uid2sample.get(childSampleId);
                Feature feature = buildFeature(childTraceId, mergedSample.getStorage().getMergeStorage().getTrace(sample.getMapping(), childTraceId), individualSegments[k][i], stats, sourceTrace2trace, Feature.builder().runId(samplesInTrace[k].getRun().getRunId()).build());
                childFeatures.add(feature);
            }
            if (childFeatures.isEmpty())
                continue;

            alignedFeatures.setFeatures(childFeatures);
            featureArr[i] = alignedFeatures;
        }

        return featureArr;
    }

    private <F extends AbstractFeature> F buildFeature (
            int traceUid,
            Trace mTrace,
            TraceSegment segment,
            SampleStats stats,
            Long2LongMap trace2trace,
            final F feature
    ) {
        final int o = mTrace.startId();

        RetentionTime rt = new RetentionTime(
                mTrace.retentionTime(segment.leftEdge - o),
                mTrace.retentionTime(segment.rightEdge - o),
                mTrace.retentionTime(segment.apex - o)
        );

        feature.setRetentionTime(rt);
        feature.setApexMass(mTrace.mz(segment.apex));
        feature.setApexIntensity((double) mTrace.intensity(segment.apex));
        feature.setAverageMass(mTrace.averagedMz());
        feature.setSnr(((stats.noiseLevel(segment.apex) > 0) ? (double) (mTrace.intensity(segment.apex) / stats.noiseLevel(segment.apex)) : null));

        if (!trace2trace.containsKey(traceUid)) {
            throw new RuntimeException(String.format("Unknown trace with uid %d", traceUid));
        }

        feature.setTraceRef(new TraceRef(trace2trace.get(traceUid), o, segment.leftEdge - o, segment.apex - o, segment.rightEdge -o));




        return feature;
    }

    public String extractFeaturesToString(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace alignedFeature, Ms2MergeStrategy ms2MergeStrategy) {
        // segments for merged trace
        Trace mergedTrace = alignedFeature.toTrace(mergedSample);
        SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mergedTrace).stream().filter(x->mergedTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  ).toArray(TraceSegment[]::new);
        if (traceSegments.length==0 && alignedFeature.getUid()>=0) return null;
        // segments for each individual trace
//        TraceSegment[][] individualSegments = new TraceSegment[alignedFeature.getSampleIds().size()][];
//        for (int k=0; k < individualSegments.length; ++k) {
//            ProcessedSample sample = samplesInTrace[k];
//            assert sample.getUid() == alignedFeature.getSampleIds().getInt(k);
//            individualSegments[k] = assignSegmentsToIndividualTrace(mergedSample, sample, traceSegments, mergedTrace, mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(k)) );
//        }
        final Int2ObjectOpenHashMap<ProcessedSample> uid2sample = new Int2ObjectOpenHashMap<>();
        for (ProcessedSample sample : samplesInTrace) {
            uid2sample.put(sample.getUid(), sample);
        }
        {
            ms2MergeStrategy.assignMs2(mergedSample, alignedFeature, traceSegments, uid2sample, (mergedTrace1, segment, index, mergedSpectrum, spectra) -> {
                // TODO: do something with MS/MS
            });
        }
        {
            StringBuffer buf = new StringBuffer();
            buf.append("{ \"rt\": [");
            List<String> xs = new ArrayList<>();
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                xs.add(String.valueOf(mergedTrace.retentionTime(k)));
            }
            buf.append(String.join( ", ", xs));
            buf.append("], \"parent\": [");
            xs.clear();
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                xs.add(String.valueOf(mergedTrace.intensity(k)));
            }
            buf.append(String.join( ", ", xs));
            buf.append("], \"children\": {");
            for (int s=0; s < samplesInTrace.length; ++s) {
                buf.append("\"" + samplesInTrace[s].getUid() + "\": [");
                xs.clear();
                final ProcessedSample S = samplesInTrace[s];
                final Trace t = mergedSample.getStorage().getMergeStorage().getTrace(samplesInTrace[s].getMapping(), alignedFeature.getTraceIds().getInt(s));
                for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                    xs.add(String.valueOf(S.getScanPointInterpolator().interpolateIntensity(t, k)));
                }
                buf.append(String.join( ", ", xs));
                buf.append("]");
                if (s+1 < samplesInTrace.length) buf.append(", ");
            }
            buf.append("}, ");
            buf.append("\"isotopes\": [" + alignedFeature.getIsotopeUids().intStream().mapToObj(Integer::toString).collect(Collectors.joining(", ")) + "],");
            buf.append("\"normalization\": {");
            for (int s=0; s < samplesInTrace.length; ++s) {
                ProcessedSample S = samplesInTrace[s];
                buf.append("\"" + samplesInTrace[s].getUid() + "\": " + S.getNormalizer().normalize(1));
                if (s+1 < samplesInTrace.length) buf.append(", ");
            }
            buf.append("}, ");
            buf.append("\"noise\": [");
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                double noise = stats.noiseLevel(k);
                buf.append(noise);
                if (k < mergedTrace.endId()) buf.append(", ");
            }
            buf.append("], ");
            buf.append("\"mz\": " + mergedTrace.averagedMz() + ", \"apexRt\": " + mergedTrace.retentionTime(mergedTrace.apex()) + ", ");
            buf.append("\"segments\": [");
            xs.clear();
            final int o = mergedTrace.startId();
            for (int k=0; k < traceSegments.length; ++k) {
                xs.add("{\"left\":" + (traceSegments[k].leftEdge-o) + ", \"right\": " + (traceSegments[k].rightEdge-o) + ", \"apex\": " + (traceSegments[k].apex-o) + "}");
            }
            buf.append(String.join( ", ", xs));
            buf.append("]}");
            return buf.toString();
        }
    }

    private TraceSegment[] assignSegmentsToIndividualTrace(ProcessedSample sample, TraceSegment[] mergedSegments, ContiguousTrace trace) {
        // easiest one: just map parent to childs
        final TraceSegment[] childSegments = new TraceSegment[mergedSegments.length];
        for (int k=0; k < mergedSegments.length; ++k) {
            TraceSegment s = mergedSegments[k];
            if (s == null)
                continue;

            int a = Math.max(trace.startId(), sample.getScanPointInterpolator().reverseMapLowerIndex(s.leftEdge));
            int b = Math.min(trace.endId(), sample.getScanPointInterpolator().reverseMapLargerIndex(s.rightEdge));
            if (b <= a) {
                continue;
            }

            childSegments[k] = new TraceSegment(a, a, b);
        }

        return resolveSegments(childSegments, trace);
    }

    private TraceSegment[] assignIntervalstoTrace(ProcessedSample sample, MergedTrace trace, double[][] intervals) {
        final TraceSegment[] childSegments = new TraceSegment[intervals.length];

        for (int k=0; k < intervals.length; k++) {
            final int a = findNearestIndex(sample, trace, intervals[k][0]);
            final int b = findNearestIndex(sample, trace, intervals[k][1]);
            if (b <= a)
                continue;

            childSegments[k] = new TraceSegment(a, a, b);
        }

        return resolveSegments(childSegments, trace.toTrace(sample));
    }

    private TraceSegment[] resolveSegments(TraceSegment[] segments, Trace trace) {
        // resolve overlapping segments
        for (int k=0; k < segments.length - 1; ++k) {
            if (segments[k] == null || segments[k + 1] == null) continue;
            if (segments[k + 1].leftEdge < segments[k].rightEdge) {
                int valley = segments[k + 1].leftEdge;
                for (int j=segments[k + 1].leftEdge; j <= segments[k].rightEdge; ++j){
                    if (trace.intensity(j) < trace.intensity(valley)) {
                        valley=j;
                    }
                }
                segments[k].rightEdge = valley;
                segments[k + 1].leftEdge = valley;
            }
        }

        // filter empty segments and find apex
        for (int k=0; k < segments.length; ++k) {
            if (segments[k] == null) continue;
            if (segments[k].leftEdge == segments[k].rightEdge) {
                segments[k] = null;
                continue;
            }
            segments[k].apex = segments[k].leftEdge;
            for (int j=segments[k].leftEdge; j <= segments[k].rightEdge; ++j){
                if (trace.intensity(j) > trace.intensity(segments[k].apex)) {
                    segments[k].apex=j;
                }
            }
        }

        return segments;
    }

    private int findNearestIndex(ProcessedSample sample, MergedTrace mergedTrace, double rt) {
        Trace trace = mergedTrace.toTrace(sample);
        double diff = Double.POSITIVE_INFINITY;
        int res = mergedTrace.getStartId();
        for (int i = mergedTrace.getStartId(); i <= mergedTrace.getEndId(); i++) {
            if (Math.abs(rt - trace.retentionTime(i)) < diff) {
                diff = Math.abs(rt - trace.retentionTime(i));
                res = i;
            }
        }
        return res;
    }


}
