package de.unijena.bioinf.lcms.merge2;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align2.*;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.lcms.trace.TraceRectangleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

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
        AlignmentStorage alignmentStorage = merged.getTraceStorage().getAlignmentStorage();
        prepareRects(merged, alignment);
        MergeStorage mergeStorage = merged.getTraceStorage().getMergeStorage();
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
            final SampleStats sampleStats = sample.getTraceStorage().getStatistics();
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
        merged.getTraceStorage().setStatistics(SampleStats.builder().noiseLevelPerScan(mergedNoiseLevelPerScan).ms2NoiseLevel(0f).ms1MassDeviationWithinTraces(new Deviation(10)).minimumMs1MassDeviationBetweenTraces(new Deviation(10)).build());

    }

    private void mergeRect(Rect r, ProcessedSample merged, ProcessedSample sample) {
        // get all mois in this rectangle
        MoI[] mois = merged.getTraceStorage().getAlignmentStorage().getMoIWithin(r.minMz, r.maxMz).stream().filter(x -> r.contains(x.getMz(), x.getRetentionTime())).toArray(MoI[]::new);
        MergedTrace mergedTrace = merged.getTraceStorage().getMergeStorage().getMerged(r.id);
        IntOpenHashSet traceIds = new IntOpenHashSet();
        for (MoI a : mois) {
            if (a instanceof AlignedMoI) {
                ((AlignedMoI) a).forSampleIdx(sample.getUid()).ifPresent(x->traceIds.add(x.getTraceId()));
            }
        }
        if (traceIds.isEmpty()) return; // nothing to merge for this sample
        // merge traces
        ContiguousTrace[] traces = traceIds.intStream().mapToObj(x -> sample.getTraceStorage().getContigousTrace(x)).toArray(ContiguousTrace[]::new);
        int startId = Arrays.stream(traces).mapToInt(ContiguousTrace::startId).min().orElse(0);
        int endId = Arrays.stream(traces).mapToInt(ContiguousTrace::endId).max().orElse(0);
        final double[] mz = new double[endId-startId+1];
        final float[] intensities = new float[endId-startId+1];
        for (ContiguousTrace trace : traces) {
            if (Math.abs( trace.averagedMz()- 637.2791) < 1e-2) {
                System.out.println("Gotcha!");
            }
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
        T = merged.getTraceStorage().getMergeStorage().addTrace(T);
        mergedTrace.getTraceIds().add(T.getUid());
        mergedTrace.getSampleIds().add(sample.getUid());
        // mergin trace
        ScanPointInterpolator mapper = sample.getScanPointInterpolator();
        final int newStartId = mapper.lowerIndex(T.startId());
        final int newEndId = mapper.largerIndex(T.endId());
        mergedTrace.extend(newStartId, newEndId);

        for (int k=newStartId; k <= newEndId; ++k) {
            final double normInt = sample.getNormalizer().normalize(mapper.interpolateIntensity(T, k));
            mergedTrace.getMz()[k - mergedTrace.getStartId()] += mapper.interpolateMz(T, k) * normInt;
            mergedTrace.getInts()[k - mergedTrace.getStartId()] += normInt;
        }
    }

    private void prepareRects(ProcessedSample merged, AlignmentBackbone alignment) {
        final Int2ObjectOpenHashMap<RecalibrationFunction> mzRecalibration = new Int2ObjectOpenHashMap<>();
        final Int2ObjectOpenHashMap<RecalibrationFunction> rtRecalibration = new Int2ObjectOpenHashMap<>();
        for (int k=0; k < alignment.getSamples().length; ++k) {
            mzRecalibration.put(alignment.getSamples()[k].getUid(), alignment.getSamples()[k].getMzRecalibration());
            rtRecalibration.put(alignment.getSamples()[k].getUid(), alignment.getSamples()[k].getRtRecalibration());
        }
        MergeStorage mergeStorage = merged.getTraceStorage().getMergeStorage();
        TraceRectangleMap rectangleMap = mergeStorage.getRectangleMap();
        for (MoI m : merged.getTraceStorage().getAlignmentStorage()) {
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
