package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.utils.Tracker;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The MergeTraceWithGapFillingStrategy is iterating over all aligned MoIs takes their
 * rectangle. Multiple MoIs can share the same rectangle.
 * It then iterates over all non-overlapping traces in the sample that are within this rectangle and
 * merges them. If two rectangles overlap, we have to keep sure to merge the right traces.
 * It's important to remember which traces are already merged to avoid merging them twice.
 */
public class MergeTracesWithoutGapFilling {

    public void merge(ProcessedSample merged, AlignmentBackbone alignment, Tracker tracker) {
        JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
        AlignmentStorage alignmentStorage = merged.getStorage().getAlignmentStorage();
        prepareRects(merged, alignment, tracker);
        MergeStorage mergeStorage = merged.getStorage().getMergeStorage();

        // TODO: that's not a good place for calculating that...
        float[] mergedNoiseLevelPerScan = new float[merged.getMapping().length()];
        long TIME1 = System.currentTimeMillis();
        List<BasicJJob<?>> jobs = new ArrayList<>();
        double summedUpNoiseLevel = 0f;
        for (int k=0; k < alignment.getSamples().length; ++k) {
            final ProcessedSample sample = alignment.getSamples()[k];
            final ScanPointInterpolator mapper = sample.getScanPointInterpolator();
            sample.active();

            final SampleStats sampleStats = sample.getStorage().getStatistics();
            {
                // TODO: Warning: this is only possible because we have global noise. We should enforce global noise in the internal interface later
                /*
                for (int i=0; i < mergedNoiseLevelPerScan.length; ++i) {
                    mergedNoiseLevelPerScan[i] += (float)sample.getNormalizer().normalize(mapper.interpolate(sampleStats.getNoiseLevelPerScan(), i));
                }
                 */
                summedUpNoiseLevel += sample.getNormalizer().normalize(sampleStats.getNoiseLevelPerScan()[sampleStats.getNoiseLevelPerScan().length/2]);
            }
            for (Rect r : mergeStorage.getRectangleMap()) {
                jobs.add(globalJobManager.submitJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        mergeAllMoIsForSampleWithinRect(r, merged, sample, tracker);
                        return true;
                    }
                }));
            }
            jobs.forEach(JJob::takeResult);
            jobs.clear();
            sample.inactive();
        }
        long TIME2 = System.currentTimeMillis();
        System.out.printf("Time for merging: %f seconds\n", (TIME2-TIME1)/1000d);
        LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).debug("Average number of Alignmments in backbone: "  + alignment.getStatistics().getAverageNumberOfAlignments());
        LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).debug("Median number of Alignmments in backbone: " + alignment.getStatistics().getMedianNumberOfAlignments());
        if (alignment.getStatistics().getAverageNumberOfAlignments() > 0) {
            {
                final float newNoiseLevel = (float)(summedUpNoiseLevel * Math.max(1,(alignment.getStatistics().getNumberOfAlignments25Quantile())) / ((double)alignment.getSamples().length));
                Arrays.fill(mergedNoiseLevelPerScan,newNoiseLevel);
            }
        } else {
            Arrays.fill(mergedNoiseLevelPerScan, (float)summedUpNoiseLevel);
        }
        merged.getStorage().setStatistics(merged.getStorage().getStatistics().withNoiseLevelPerScan(mergedNoiseLevelPerScan));

    }


    private void prepareRects(ProcessedSample merged, AlignmentBackbone alignment, Tracker tracker) {
        long TIME1 = System.currentTimeMillis();
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

            // account for rounding errors due to float 32 :/
            r.minMz = Float.intBitsToFloat(Float.floatToIntBits(r.minMz)-1);
            r.maxMz = Float.intBitsToFloat(Float.floatToIntBits(r.maxMz)+1);
            r.minRt = Float.intBitsToFloat(Float.floatToIntBits(r.minRt)-1);
            r.maxRt = Float.intBitsToFloat(Float.floatToIntBits(r.maxRt)+1);

            for (Rect other : rectangleMap.overlappingRectangle(r)) {
                r.upgrade(other);
                rectangleMap.removeRect(other);
            }
            rectangleMap.addRect(r);
            tracker.createRect(merged, r);
        }
        long TIME2 = System.currentTimeMillis();
        System.out.printf("Time for preparing rects: %f seconds\n", (TIME2-TIME1)/1000d);
    }


    ////////////////////////////////////////////////////////////////


    private void mergeAllMoIsForSampleWithinRect(Rect r, ProcessedSample merged, ProcessedSample sample, Tracker tracker) {
        // get all mois in this rectangle
        MoI[] mois = merged.getStorage().getAlignmentStorage().getMoIWithin(r.minMz, r.maxMz).stream().filter(x -> r.contains(x.getMz(), x.getRetentionTime())).toArray(MoI[]::new);
        MoI[] moisForSample = Arrays.stream(mois).flatMap(a->((AlignedMoI) a).forSampleIdx(sample.getUid()).stream()).toArray(MoI[]::new);
        // we want to merge them into the MergedTrace corresponding to this rectangle
        IntOpenHashSet traceIds = new IntOpenHashSet(Arrays.stream(moisForSample).mapToInt(MoI::getTraceId).toArray());
        if (traceIds.isEmpty()) {
            tracker.emptyRect(sample, r);
            return; // nothing to merge for this sample
        }
        // get all traces in the sample that can be merged into the mergedTrace
        ContiguousTrace[] traces = traceIds.intStream().mapToObj(x -> sample.getStorage().getTraceStorage().getContigousTrace(x)).toArray(ContiguousTrace[]::new);

        // basically there are two merge operations:
        // 1.) We want to merge Traces along different samples
        // 2.) We also want to merge Traces within ONE sample if they lie in the same rectangle
        // We now do 2). We take all the traces in the rectangle and merge them into ONE trace, such that we later
        // have exactly one trace per sample and MergedTrace
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
        double avgMz = 0d; double sumInt = 0d;
        for (int k=0; k < mz.length; ++k) {
            if (intensities[k]>0) {
                avgMz += mz[k];
                mz[k] /= intensities[k];
                sumInt += intensities[k];
            }
        }
        avgMz/=sumInt;
        // fill in missing values
        for (int k=0; k < mz.length; ++k) {
            if (intensities[k]<=0) {
                mz[k] = avgMz;
            }
        }

        // T is just for using the interpolate method later. We do not store T as ContigousTrace but as ProjectedTrace
        ContiguousTrace T = new ContiguousTrace(
                sample.getMapping(), startId, endId, mz, intensities
        );

        /*
        T = merged.getStorage().getMergeStorage().addTrace(T);
        mergedTrace.getTraceIds().add(T.getUid());
        mergedTrace.getSampleIds().add(sample.getUid());
        */

        // Each ProjectedTrace consists of two traces:
        // 1.) The trace from the original sample as it is
        // 2.) The trace from the original sample projected onto the retention time axis of
        //     the merged sample
        ScanPointInterpolator mapper = sample.getScanPointInterpolator();
        final int newStartId = mapper.roundIndex(startId);
        final int newEndId = mapper.roundIndex(endId);
        final double[] mzP = new double[newEndId-newStartId+1];
        final float[] intensityP = new float[mzP.length];
        //mergedTrace.extend(newStartId, newEndId);
        int newApex = newStartId ;
        for (int k=newStartId; k <= newEndId; ++k) {
            final double projInt = mapper.interpolateIntensity(T, k, startId, endId);
            mzP[k - newStartId] = projInt<=0 ? Double.NaN : mapper.interpolateMz(T, k, startId, endId);
            intensityP[k - newStartId] = (float)projInt;
            if (intensityP[newApex - newStartId] < intensityP[k - newStartId]) newApex = k;
        }

        ProjectedTrace projectedTrace = new ProjectedTrace(sample.getUid(),
                T.startId(), T.endId(), T.apex(), newStartId, newEndId, newApex, mz, mzP, intensities, intensityP
        );

        addMs2ToProjectedTrace(sample, traces, projectedTrace, tracker, merged);
        merged.getStorage().getMergeStorage().addProjectedTrace(r.id, sample.getUid(), projectedTrace);
        createIsotopeProjectedTraces(merged, sample, r, projectedTrace, moisForSample, tracker);
        tracker.mergedTrace(merged, sample, r, projectedTrace, moisForSample);
    }

    private void createIsotopeProjectedTraces(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] mois, Tracker tracker) {

        /*
        Problem: a single trace might consists of multiple compounds, each compound might have a different charge.
        Is that realistic? It feels wrong, as different charges are different masses and should be separate traces.
        But it COULD happen. More likely is that the whole trace is only one charge but we do not detect the charge
        for some compounds in the trace.

        What is the solution here? For now I would suggest that we just pick all isotope peaks for all possible charges
        and detect the charge state later. Feels wrong, but the alternative would mean to detect charges for a complete
        trace without looking at the msms.
         */


        int[] chargeStates = Arrays.stream(mois).map(MoI::getIsotopes).filter(Objects::nonNull).mapToInt(x->x.charge==0 ? 1 : Math.abs(x.charge)).distinct().toArray();
        for (final int chargeState : chargeStates) {
            int isotopePeak = 1;
            IntOpenHashSet alreadyPickedTraces = new IntOpenHashSet();
            while (true) {
                // if at least one MoI has an isotope peak with this nominal mass...
                List<ContiguousTrace> isotopeTraces = new ArrayList<>();
                // then extract isotope traces
                int foundIsotopes = 0;
                for (MoI m : mois) {
                    if (m.getIsotopes()==null || (m.getIsotopes().charge!=0 && m.getIsotopes().charge!=chargeState)) continue;
                    int K = m.getIsotopes().getForNominalMass(m.getMz(), isotopePeak);
                    if (K<0) continue;
                    if (m.getTraceId() != m.getIsotopes().traceIds[K]) {
                        ++foundIsotopes;
                        if (alreadyPickedTraces.add(m.getIsotopes().traceIds[K])) {
                            ContiguousTrace tid = sample.getStorage().getTraceStorage().getContigousTrace(m.getIsotopes().traceIds[K]);
                            isotopeTraces.add(tid);
                        }
                    }
                }
                if (!isotopeTraces.isEmpty()){
                    // TODO: in theory, isotopes could be larger than original trace. We might take this into consideration
                    int startId = projectedTrace.getRawStartId();
                    int endId = projectedTrace.getRawEndId();
                    final double[] mz = new double[endId-startId+1];
                    final float[] intensities = new float[endId-startId+1];
                    for (ContiguousTrace trace : isotopeTraces) {
                        for (int k=Math.max(trace.startId(), startId), n=Math.min(trace.endId(), endId); k <=n; ++k) {
                            mz[k-startId] += trace.mz(k)*trace.intensity(k);
                            intensities[k-startId] += trace.intensity(k);
                        }
                    }

                    // we might have to shorten the trace, as isotopes might be smaller than the monoisotopic
                    int shortenedStartId = startId;
                    while (shortenedStartId < endId && intensities[shortenedStartId-startId]<=0) ++shortenedStartId;
                    int shortenedEndId = endId;
                    while (shortenedEndId > startId && intensities[shortenedEndId-startId]<=0) --shortenedEndId;

                    if (shortenedStartId>shortenedEndId) {
                        // the trace is empty, there is no such isotope peak
                        LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).error("Isotope trace disappearded during merging.");
                        break;
                    }

                    double[] mzShortened = new double[shortenedEndId-shortenedStartId+1];
                    float[] intensityShortened = new float[mzShortened.length];
                    for (int k=shortenedStartId; k <= shortenedEndId; ++k) {
                        intensityShortened[k-shortenedStartId] = intensities[k-startId];
                        mzShortened[k-shortenedStartId] = mz[k-startId]/intensities[k-startId];
                        if (!Double.isFinite(mz[k-shortenedStartId])) {
                            throw new RuntimeException("should not happen!");
                        }
                    }

                    ContiguousTrace T = new ContiguousTrace(
                            sample.getMapping(), shortenedStartId, shortenedEndId, mzShortened, intensityShortened
                    );

                    // mergin trace
                    ScanPointInterpolator mapper = sample.getScanPointInterpolator();
                    final int newStartId = mapper.lowerIndex(T.startId());
                    final int newEndId = mapper.largerIndex(T.endId());
                    final double[] mzP = new double[newEndId-newStartId+1];
                    Arrays.fill(mzP, Double.NaN);
                    final float[] intP = new float[mzP.length];
                    int newApex = newStartId;
                    for (int k=newStartId; k <= newEndId; ++k) {
                        final double projInt = mapper.interpolateIntensity(T, k, T.startId(), T.endId());
                        if (projInt > 0) {
                            mzP[k - newStartId] = mapper.interpolateMz(T, k, T.startId(), T.endId());
                            if (mzP[k-newStartId] <= 0) {
                                throw new RuntimeException("This should never happen!");
                            }
                            intP[k - newStartId] = (float)projInt;
                            if (projInt > intP[newApex-newStartId]) newApex=k;
                        }
                    }

                    ProjectedTrace projectedIsotopeTrace = new ProjectedTrace(sample.getUid(),
                            T.startId(), T.endId(), T.apex(), newStartId, newEndId, newApex, mzShortened, mzP, intensityShortened, intP
                    );

                    // add ms2 data
                    //addMs2ToProjectedTrace(sample, isotopeTraces.toArray(ContiguousTrace[]::new), projectedIsotopeTrace, tracker, merged);

                    // update
                    merged.getStorage().getMergeStorage().addIsotopeProjectedTrace(r.id, chargeState, isotopePeak, sample.getUid(), projectedIsotopeTrace);
                } else if (foundIsotopes==0) {
                    // break if there is a gap within isotope pattern
                    break;
                }
                ++isotopePeak;
            }
        }

    }

    private void addMs2ToProjectedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProjectedTrace projectedTrace, Tracker tracker, ProcessedSample merged) {
        ArrayList<MsMsTraceReference> ids = new ArrayList<>();
        for (ContiguousTrace t : sourceTraces) {
            ids.addAll(Arrays.asList(sample.getStorage().getTraceStorage().getMs2ForTrace(t.getUid())));
        }
        MsMsTraceReference[] idsArray = ids.toArray(MsMsTraceReference[]::new);
        tracker.assignMs2ToMergedTrace(sample, sourceTraces, merged, projectedTrace, idsArray);
        projectedTrace.setMs2Refs(idsArray);
        // double check assignment
        for (MsMsTraceReference ref : idsArray) {
            Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage().ms2SpectrumHeader(ref.ms2Uid);
            if (!projectedTrace.inProjectedRange(sample.getScanPointInterpolator().roundIndex(header.getParentId()))) {
                LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).warn("MSMS is not in projected trace but in source trace oO");
            }
        }



    }



}
