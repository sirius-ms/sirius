package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.*;
import de.unijena.bioinf.lcms.traceextractor.MassOfInterestConfidenceEstimatorStrategy;
import de.unijena.bioinf.lcms.utils.Tracker;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.slf4j.LoggerFactory;

import java.util.*;

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
        List<BasicJJob<Float>> jobs = new ArrayList<>();
        double summedUpNoiseLevel = 0f;

        FloatArrayList avgPeakWidths = new FloatArrayList();
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
            final int medianNumberOfAlignments = (int)(alignment.getStatistics().getMedianNumberOfAlignments());
            for (Rect r : mergeStorage.getRectangleMap()) {
                jobs.add(globalJobManager.submitJob(new BasicJJob<Float>() {
                    @Override
                    protected Float compute() throws Exception {
                        return mergeAllMoIsForSampleWithinRect(r, merged, sample, tracker, medianNumberOfAlignments);
                    }
                }));
            }
            for (BasicJJob<Float> job : jobs) {
                float pw = job.takeResult();
                if (pw>0) {
                    avgPeakWidths.add(pw);
                }
            }
            jobs.clear();
            sample.inactive();
        }
        SampleStats mergedStats = merged.getStorage().getStatistics();
        if (avgPeakWidths.size()>=20) {
            double avg = Statistics.robustAverage(avgPeakWidths.toFloatArray());
            mergedStats.setExpectedPeakWidth(avg);
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
        merged.getStorage().setStatistics(mergedStats.withNoiseLevelPerScan(mergedNoiseLevelPerScan));

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
            if (moi.majorityIsIsotopeOrMulticharge()) {
                tracker.moiDeleted(moi);
                continue;
            }
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

            // Absorb every rectangle that overlaps this one, and keep absorbing: growing the bounds can
            // bring further rectangles into range. A single pass measured the overlap against the
            // original bounds only, so rectangles that overlapped the grown one were left behind and
            // whether two of them ended up together depended on which mass of interest came first.
            final List<Rect> absorbed = new ArrayList<>();
            final IntOpenHashSet absorbedIds = new IntOpenHashSet();
            final ArrayDeque<Rect> pending = new ArrayDeque<>(rectangleMap.overlappingRectangle(r));
            while (!pending.isEmpty()) {
                final Rect other = pending.poll();
                // a rectangle can be queued twice before it is absorbed, and counting it twice would
                // bias the mean below
                if (!absorbedIds.add(other.id)) continue;
                absorbed.add(other);
                rectangleMap.removeRect(other);   // so it cannot come back from the next query
                if (r.expandTo(other)) {
                    for (Rect next : rectangleMap.overlappingRectangle(r)) {
                        if (!absorbedIds.contains(next.id)) pending.add(next);
                    }
                }
            }
            // One mean over the whole group, not a running one - see Rect#expandTo. Sorted first
            // because float addition is not associative, so even a correct mean would otherwise depend
            // on the order the spatial index returned the rectangles in.
            if (!absorbed.isEmpty()) {
                final double[] masses = new double[absorbed.size() + 1];
                masses[0] = r.avgMz;
                for (int i = 0; i < absorbed.size(); ++i) masses[i + 1] = absorbed.get(i).avgMz;
                Arrays.sort(masses);
                double sum = 0d;
                for (double mass : masses) sum += mass;
                r.avgMz = sum / masses.length;
            }
            rectangleMap.addRect(r);
            tracker.createRect(merged, r);
        }
        long TIME2 = System.currentTimeMillis();
        System.out.printf("Time for preparing rects: %f seconds\n", (TIME2-TIME1)/1000d);
    }


    ////////////////////////////////////////////////////////////////
    // if true, we check in the original data for each sample if we missed a trace
    // this is quite expensive and should not be necessary if
    // the alignment did its job
    private static final boolean GAP_FILLING = false;

    private float mergeAllMoIsForSampleWithinRect(Rect r, ProcessedSample merged, ProcessedSample sample, Tracker tracker, int medianAlignments) {
        float avgPeakWidth = 0f;
        // get all mois in this rectangle
        MoI[] mois = merged.getStorage().getAlignmentStorage().getMoIWithin(r.minMz, r.maxMz).stream().filter(x -> !(((AlignedMoI)x).majorityIsIsotopeOrMulticharge()) && r.contains(x.getMz(), x.getRetentionTime())).toArray(MoI[]::new);
        MoI[] moisForSample = Arrays.stream(mois).flatMap(a->((AlignedMoI) a).forSampleIdx(sample.getUid()).stream()).toArray(MoI[]::new);
        // we want to merge them into the MergedTrace corresponding to this rectangle
        IntOpenHashSet traceIds = new IntOpenHashSet(Arrays.stream(moisForSample).mapToInt(MoI::getTraceId).toArray());
        ContiguousTrace[] traces;
        if (traceIds.isEmpty()) {
            // can we find the moi in the original traces ("GapFilling"-like)?
            if (GAP_FILLING) {
                List<ContiguousTrace> contigousTraces = sample.getStorage().getTraceStorage().getContigousTraces(r.minMz, r.maxMz, sample.getMapping().idForRetentionTime(r.minRt),
                        sample.getMapping().idForRetentionTime(r.maxMz));
                LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).warn("Cannot find MOI for " + r + ", use Gap Filling approach instead and found " + contigousTraces.size() + " traces that fit.");
                traces = contigousTraces.stream().filter(x->r.containsRt(sample.getRtRecalibration().value(x.retentionTime(x.apex())))).toArray(ContiguousTrace[]::new);
            } else {
                tracker.emptyRect(sample, r);
                return 0f; // nothing to merge for this sample
            }
        } else {
            // get all traces in the sample that can be merged into the mergedTrace
            traces = traceIds.intStream().mapToObj(x -> sample.getStorage().getTraceStorage().getContigousTrace(x)).toArray(ContiguousTrace[]::new);
        }

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

        {
            for (MoI m : mois) {
                if (((AlignedMoI)m).getAligned().length>medianAlignments && m.getConfidence()>MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) {
                    final float peakWidth = estimatePeakWidth(merged.getMapping(), newStartId, newApex, intensityP);
                    if (peakWidth>0) {
                        avgPeakWidth=peakWidth;
                        break;
                    }
                }
            }
        }

        tracker.mergedTrace(merged, sample, r, projectedTrace, moisForSample);
        return avgPeakWidth;
    }

    private static float estimatePeakWidth(ScanPointMapping mapping, int offset, int apex, float[] intensity) {
        apex -= offset;
        int fwhmLeft = apex, fwhmRight = apex;
        final float threshold50 = intensity[apex]*0.5f;
        for (; fwhmLeft >= 0 && intensity[fwhmLeft]>=threshold50; --fwhmLeft ) {}
        ++fwhmLeft;
        for (; fwhmRight < intensity.length && intensity[fwhmRight]>=threshold50; ++fwhmRight ) {}
        --fwhmRight;
        final float threshold20 = intensity[apex]*0.2f;
        int l=fwhmLeft,r=fwhmRight;
        for (; l >= 0 && intensity[l]>=threshold20; --l ) {}
        ++l;
        for (; r < intensity.length && intensity[r]>=threshold20; ++r ) {}
        --r;
        final double width50 = mapping.getRetentionTimeAt(fwhmRight+offset)-mapping.getRetentionTimeAt(fwhmLeft+offset);
        final double width20 = mapping.getRetentionTimeAt(r+offset)-mapping.getRetentionTimeAt(l+offset);
        // assuming a perfect Gaussian shape, we would expect the fwhm at a sigma of 1.18 and the 20% height at a sigma of 1.8
        if (width50<=0) return (float)(width20/3.6);
        final double expectedSigma = Math.sqrt((width50/2.36)*(width20/3.6));
        return (float)(expectedSigma*2);
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

                    // Average mass of the isotope trace, weighted by intensity, exactly as the
                    // monoisotopic merge above computes it.
                    double isotopeAvgMz = 0d, isotopeSumInt = 0d;
                    for (int k = 0; k < mz.length; ++k) {
                        if (intensities[k] > 0) {
                            isotopeAvgMz += mz[k];
                            isotopeSumInt += intensities[k];
                        }
                    }
                    if (!(isotopeSumInt > 0)) {
                        LoggerFactory.getLogger(MergeTracesWithoutGapFilling.class).error("Isotope trace disappearded during merging.");
                        break;
                    }
                    isotopeAvgMz /= isotopeSumInt;

                    double[] mzShortened = new double[shortenedEndId-shortenedStartId+1];
                    float[] intensityShortened = new float[mzShortened.length];
                    for (int k=shortenedStartId; k <= shortenedEndId; ++k) {
                        final int i = k - startId;
                        intensityShortened[k-shortenedStartId] = intensities[i];
                        // A scan inside the trace that no isotope trace covered has no mass of its own,
                        // and dividing its zero by zero wrote a NaN into the m/z array - on this test
                        // data for 489628 of 701699 points, because the isotopes are followed across far
                        // fewer scans than the monoisotopic peak whose range this array spans. The
                        // monoisotopic merge fills such a gap with the trace average; this one did not,
                        // and the check below could not catch it: it read the accumulated weighted sum
                        // rather than the quotient just computed, at the wrong offset, and the sum is a
                        // finite zero exactly where the quotient is NaN.
                        mzShortened[k-shortenedStartId] = intensities[i] > 0 ? mz[i]/intensities[i] : isotopeAvgMz;
                        if (!Double.isFinite(mzShortened[k-shortenedStartId])) {
                            throw new RuntimeException("non-finite m/z in isotope trace at scan " + k);
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
