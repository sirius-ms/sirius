package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.isotopes.IsotopePattern;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The MergeTraceWithGapFillingStrategy is iterating over all aligned MoIs takes their
 * rectangle. Multiple MoIs can share the same rectangle.
 * It then iterates over all non-overlapping traces in the sample that are within this rectangle and
 * merges them. If two rectangles overlap, we have to keep sure to merge the right traces.
 * It's important to remember which traces are already merged to avoid merging them twice.
 */
public class MergeTracesWithoutGapFilling {

    public void merge(ProcessedSample merged, AlignmentBackbone alignment) {
        JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
        AlignmentStorage alignmentStorage = merged.getStorage().getAlignmentStorage();
        prepareRects(merged, alignment);
        MergeStorage mergeStorage = merged.getStorage().getMergeStorage();
        for (Rect r : mergeStorage.getRectangleMap()) {
            final MergedTrace mergedTrace = new MergedTrace(r.id);
            mergeStorage.addMerged(mergedTrace);
        }

        // TODO: that's not a good place for calculating that...
        float[] mergedNoiseLevelPerScan = new float[merged.getMapping().length()];

        List<BasicJJob<?>> jobs = new ArrayList<>();
        for (int k=0; k < alignment.getSamples().length; ++k) {
            final ProcessedSample sample = alignment.getSamples()[k];
            final ScanPointInterpolator mapper = sample.getScanPointInterpolator();
            sample.active();
            final SampleStats sampleStats = sample.getStorage().getStatistics();
            {
                for (int i=0; i < mergedNoiseLevelPerScan.length; ++i) {
                    mergedNoiseLevelPerScan[i] += (float)sample.getNormalizer().normalize(mapper.interpolate(sampleStats.getNoiseLevelPerScan(), i));
                }
            }
            for (Rect r : mergeStorage.getRectangleMap()) {
                jobs.add(globalJobManager.submitJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        mergeRect(r, merged, sample);
                        return true;
                    }
                }));
            }
            jobs.forEach(JJob::takeResult);
            jobs.clear();
            sample.inactive();
        }
        for (int k=0; k < mergedNoiseLevelPerScan.length; ++k) {
            mergedNoiseLevelPerScan[k] /= alignment.getSamples().length;
        }
        for (MergedTrace t : mergeStorage) {
            if (t.getSampleIds().size()==0) {
                mergeStorage.removeMerged(t.getUid());

            } else {
                t.finishMerging();
                mergeStorage.addMerged(t);
            }
        }
        merged.getStorage().setStatistics(SampleStats.builder().noiseLevelPerScan(mergedNoiseLevelPerScan).ms2NoiseLevel(0f).ms1MassDeviationWithinTraces(new Deviation(10)).minimumMs1MassDeviationBetweenTraces(new Deviation(10)).build());

    }

    private void mergeRect(Rect r, ProcessedSample merged, ProcessedSample sample) {
        // get all mois in this rectangle
        MoI[] mois = merged.getStorage().getAlignmentStorage().getMoIWithin(r.minMz, r.maxMz).stream().filter(x -> r.contains(x.getMz(), x.getRetentionTime())).toArray(MoI[]::new);
        MergedTrace mergedTrace = merged.getStorage().getMergeStorage().getMerged(r.id);
        MoI[] moisForSample = Arrays.stream(mois).flatMap(a->((AlignedMoI) a).forSampleIdx(sample.getUid()).stream()).toArray(MoI[]::new);
        IntOpenHashSet traceIds = new IntOpenHashSet(Arrays.stream(moisForSample).mapToInt(MoI::getTraceId).toArray());
        if (traceIds.isEmpty()) return; // nothing to merge for this sample
        // merge traces
        ContiguousTrace[] traces = traceIds.intStream().mapToObj(x -> sample.getStorage().getTraceStorage().getContigousTrace(x)).toArray(ContiguousTrace[]::new);
        int startId = Arrays.stream(traces).mapToInt(ContiguousTrace::startId).min().orElse(0);
        int endId = Arrays.stream(traces).mapToInt(ContiguousTrace::endId).max().orElse(0);
        final double[] mz = new double[endId-startId+1];
        final float[] intensities = new float[endId-startId+1];
        for (ContiguousTrace trace : traces) {
            for (int k=trace.startId(), n=trace.endId(); k <=n; ++k) {
                mz[k-startId] += trace.mz(k)*trace.intensity(k);
                intensities[k-startId] += trace.intensity(k);
            }
        }
        for (int k=0; k < mz.length; ++k) {
            mz[k] /= intensities[k];
        }
        ContiguousTrace T = new ContiguousTrace(
                sample.getMapping(), startId, endId, mz, intensities
        );
        T = merged.getStorage().getMergeStorage().addTrace(T);
        mergedTrace.getTraceIds().add(T.getUid());
        mergedTrace.getSampleIds().add(sample.getUid());
        // mergin trace
        ScanPointInterpolator mapper = sample.getScanPointInterpolator();
        final int newStartId = mapper.lowerIndex(T.startId());
        final int newEndId = mapper.largerIndex(T.endId());
        mergedTrace.extend(newStartId, newEndId);

        for (int k=newStartId; k <= newEndId; ++k) {
            final double normInt = sample.getNormalizer().normalize(mapper.interpolateIntensity(T, k));
            if (normInt > 0) {
                mergedTrace.getMz()[k - mergedTrace.getStartId()] += mapper.interpolateMz(T, k) * normInt;
                mergedTrace.getInts()[k - mergedTrace.getStartId()] += normInt;
            }
        }

        // add ms2 data
        addMs2(mergedTrace, sample, traces);

        // now also add isotopic traces
        createIsotopeMergedTraces(merged, sample, mergedTrace, T, moisForSample);

        // update
        merged.getStorage().getMergeStorage().addMerged(mergedTrace);

    }

    private void addMs2(MergedTrace mergedTrace, ProcessedSample sample, ContiguousTrace[] sourceTraces) {
        IntArrayList ids = new IntArrayList();
        for (ContiguousTrace t : sourceTraces) {
            ids.addElements(ids.size(), sample.getStorage().getTraceStorage().getMs2ForTrace(t.getUid()));
        }
        if (!ids.isEmpty()) {
            mergedTrace.addMs2(sample.getUid(), ids.toIntArray());
        }
    }

    private void createIsotopeMergedTraces(ProcessedSample merged, ProcessedSample sample, MergedTrace mergedTrace, ContiguousTrace t, MoI[] mois) {
        int isotopePeak = 1;
        while (true) {
            // if at least one MoI has an isotope peak with this nominal mass...
            final int isotopePeakCurrent = isotopePeak;
            List<ContiguousTrace> isotopeTraces = new ArrayList<>();
            // then extract isotope traces
            for (MoI m : mois) {
                if (m.getIsotopes()==null) continue;
                int K = m.getIsotopes().getForNominalMass(t.averagedMz(), isotopePeak);
                if (K<0) continue;
                isotopeTraces.add(sample.getStorage().getTraceStorage().getContigousTrace(m.getIsotopes().traceIds[K]));
                /*
                TraceStorage traceStorage = sample.getStorage().getTraceStorage();
                double minMz = IsotopePattern.getMinimumMzFor(m.getMz(), isotopePeak, 1)-5e-4;
                double maxMz = IsotopePattern.getMaximumMzFor(m.getMz(), isotopePeak, 1)+5e-4;
                final List<ContiguousTrace> traces = traceStorage.getContigousTraces(minMz,maxMz, m.getScanId(), m.getScanId());
                if (traces.size() > 1) {
                    // TODO: remove wrong assignments
                    LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).warn("Multiple possible isotope traces.");
                }
                if (!traces.isEmpty()) {
                    isotopeTraces.add(traces.get(0));
                }
                 */
            }
            if (!isotopeTraces.isEmpty()){
                int startId = t.startId();
                int endId = t.endId();
                final double[] mz = new double[endId-startId+1];
                final float[] intensities = new float[endId-startId+1];
                for (ContiguousTrace trace : isotopeTraces) {
                    for (int k=Math.max(trace.startId(), startId), n=Math.min(trace.endId(), endId); k <=n; ++k) {
                        mz[k-startId] += trace.mz(k)*trace.intensity(k);
                        intensities[k-startId] += trace.intensity(k);
                    }
                }
                for (int k=0; k < mz.length; ++k) {
                    mz[k] /= intensities[k];
                }
                ContiguousTrace T = new ContiguousTrace(
                        sample.getMapping(), startId, endId, mz, intensities
                );
                T = merged.getStorage().getMergeStorage().addTrace(T);

                // get corresponding merged trace
                MergedTrace isotopeMergedTrace;
                if (mergedTrace.getIsotopeUids().size() >= isotopePeak) {
                    int uid=mergedTrace.getIsotopeUids().getInt(isotopePeak-1);
                    isotopeMergedTrace = merged.getStorage().getMergeStorage().getMerged(uid);
                } else {
                    isotopeMergedTrace = new MergedTrace(merged.getStorage().getMergeStorage().getFreeIsotopeMergeUid());
                    mergedTrace.getIsotopeUids().add(isotopeMergedTrace.getUid());
                }

                isotopeMergedTrace.getTraceIds().add(T.getUid());
                isotopeMergedTrace.getSampleIds().add(sample.getUid());

                // mergin trace
                ScanPointInterpolator mapper = sample.getScanPointInterpolator();
                final int newStartId = mapper.lowerIndex(T.startId());
                final int newEndId = mapper.largerIndex(T.endId());
                isotopeMergedTrace.extend(newStartId, newEndId);

                for (int k=newStartId; k <= newEndId; ++k) {
                    final double normInt = sample.getNormalizer().normalize(mapper.interpolateIntensity(T, k));
                    if (normInt > 0) {
                        isotopeMergedTrace.getMz()[k - isotopeMergedTrace.getStartId()] += (mapper.interpolateMz(T, k) * normInt);
                        isotopeMergedTrace.getInts()[k - isotopeMergedTrace.getStartId()] += normInt;
                    }
                }

                // add ms2 data
                addMs2(isotopeMergedTrace, sample, isotopeTraces.toArray(ContiguousTrace[]::new));

                // update
                merged.getStorage().getMergeStorage().addMerged(isotopeMergedTrace);
            } else break;

            ++isotopePeak;
        }
    }

    private void prepareRects(ProcessedSample merged, AlignmentBackbone alignment) {
        final Int2ObjectOpenHashMap<RecalibrationFunction> mzRecalibration = new Int2ObjectOpenHashMap<>();
        final Int2ObjectOpenHashMap<RecalibrationFunction> rtRecalibration = new Int2ObjectOpenHashMap<>();
        for (int k=0; k < alignment.getSamples().length; ++k) {
            mzRecalibration.put(alignment.getSamples()[k].getUid(), alignment.getSamples()[k].getMzRecalibration());
            rtRecalibration.put(alignment.getSamples()[k].getUid(), alignment.getSamples()[k].getRtRecalibration());
        }
        MergeStorage mergeStorage = merged.getStorage().getMergeStorage();
        TraceRectangleMap rectangleMap = mergeStorage.getRectangleMap();
        for (MoI m : merged.getStorage().getAlignmentStorage()) {
            final AlignedMoI moi = (AlignedMoI)m;
            Rect r = new Rect(moi.getRect());
            r.minMz = (float)moi.getMz();
            r.maxMz = (float)moi.getMz();
            r.minRt = r.maxRt = (float)moi.getRetentionTime();
            for (MoI a : moi.getAligned()) {
                RecalibrationFunction mz = mzRecalibration.get(a.getSampleIdx());
                r.minMz = (float)Math.min(r.minMz, mz.value(a.getMz()));
                r.maxMz = (float)Math.max(r.maxMz, mz.value(a.getMz()));
                RecalibrationFunction rt = rtRecalibration.get(a.getSampleIdx());
                r.minRt = (float)Math.min(r.minRt, rt.value(a.getRect().minRt));
                r.maxRt = (float)Math.max(r.maxRt, rt.value(a.getRect().maxRt));
            }
            for (Rect other : rectangleMap.overlappingRectangle(r)) {
                r.upgrade(other);
                rectangleMap.removeRect(other);
            }
            rectangleMap.addRect(r);
        }
    }

}
