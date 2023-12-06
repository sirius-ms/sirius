package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.MassOfInterest;
import de.unijena.bioinf.lcms.align.TraceAligner;
import de.unijena.bioinf.lcms.trace.*;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.h2.mvstore.rtree.SpatialKey;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TraceMerger {

    protected MergeMvStorage storage;

    protected Deviation minimumMzDev = new Deviation(3); // learn from data
    protected ScanPointMapping mapping;

    protected MassOfInterest[] mois;
    protected int[] correspondingTraces;
    protected IntArrayList[] mergedIds;

    protected TracePicker picker;

    public TraceMerger(TracePicker picker, ScanPointMapping mapping, MassOfInterest[] mois) throws IOException {
        this.storage = MergeStorage.createTemporaryStorage(mapping);
        this.picker = picker;
        this.mapping = mapping;
        this.mois = mois;
        this.correspondingTraces = new int[mois.length];
        reserveTraceAreas();
    }

    public void merge(ProcessedSample[] samples, UnivariateFunction[] rtRecalibration, UnivariateFunction[] mzRecalibration) {
        // init
        IntArrayList ids = new IntArrayList();
        for (TraceSeparationAndUnificationMap.Rectangle rect : storage.traceSeparationAndUnificationMap) {
            ids.add(rect.id);
            storage.mergedTraces.put(rect.id, new MergedTrace(
                    mapping, rect.id, 0,0,0,rect.avgMz, rect.minMz, rect.maxMz, new double[0], new float[0],
                    new int[0]
                    )
            );
        }

        for (int k=0; k < samples.length; ++k) {
            samples[k].active();
            final PairwiseScanPointMapping mapper = new PairwiseScanPointMapping(mapping, samples[k].getMapping(), rtRecalibration[k]);
            for (int j=0; j < ids.size(); ++j) {
                final int id = ids.getInt(j);
                final TraceSeparationAndUnificationMap.Rectangle rect = storage.traceSeparationAndUnificationMap.getRectangle(id).get();
                final MergedTrace merged = storage.mergedTraces.get(id);
                TracePicker tracePicker = new TracePicker(samples[k].getTraceStorage(), samples[k].getMapping());
                int startIndex = mapper.reverseRtIndex(rect.minRt);
                int endIndex = mapper.reverseRtIndex(rect.maxRt);
                int innerStartIndex, innerEndIndex;
                double innerStartMz, innerEndMz;
                Optional<TraceSeparationAndUnificationMap.Rectangle> inner = rect.getInnerArea();
                if (inner.isPresent()) {
                    TraceSeparationAndUnificationMap.Rectangle i = inner.get();
                    innerStartIndex = mapper.reverseRtIndex(i.minRt);
                    innerEndIndex = mapper.reverseRtIndex(i.maxRt);
                    innerStartMz = i.minMz;
                    innerEndMz = i.maxMz;
                } else {
                    innerStartIndex=startIndex; innerEndIndex=endIndex; innerStartMz = rect.minMz; innerEndMz=rect.maxMz;
                }

                Optional<ContiguousTrace> contiguousTrace = tracePicker.detectTraceAlongTimeWindow(
                        startIndex, endIndex, rect.minMz, rect.maxMz, innerStartIndex, innerEndIndex, innerStartMz, innerEndMz
                );
                contiguousTrace.ifPresent(x->mergeTraces(merged, x, mapper));


            }
            samples[k].inactive();
        }

        // DEBUG
        {
            double mz1=72.08162689208984;
            double mz2=72.93767166137695;
            final List<MergedTrace> a1 = storage.mergedTraces.values().stream().filter(x->Math.abs(x.averagedMz()-mz1) < 0.0003).toList();
            final List<MergedTrace> a2 = storage.mergedTraces.values().stream().filter(x->Math.abs(x.averagedMz()-mz2) < 0.0003).toList();
            System.out.println("DEBUG!");
        }

        ids.forEach(x->{
            MergedTrace trace = storage.mergedTraces.get(x);
            if (trace.numberOfMergedScanPoints.length>0) {
                storage.finishedTraces.put(x, trace.finishMerging());
            }
        });
    }

    /**
     * For each aligned MoI, we reserve a rectangle of m/z and rt. Next, we search for intersecting rectangles and
     * divide them such that no rectangles intersect anymore.
     */
    private void reserveTraceAreas() {
        // sort mois by number of alignments
        IntArrayList orderedByAlignments = new IntArrayList(mois.length);
        for (int k=0; k < mois.length; ++k) {
            if (mois[k] instanceof TraceAligner.MergedMassOfInterest) {
                orderedByAlignments.add(k);
            }
        }
        orderedByAlignments.sort((i,j)->numberOfAlignments(mois[j])-numberOfAlignments(mois[i]));
        // create rectangles
        for (int i=0; i < orderedByAlignments.size(); ++i) {
            final TraceAligner.MergedMassOfInterest moi = (TraceAligner.MergedMassOfInterest) mois[orderedByAlignments.getInt(i)];
            storage.traceSeparationAndUnificationMap.addRectangle(new TraceSeparationAndUnificationMap.Rectangle(
                    (float)moi.getMinMz(),
                    (float)moi.getMaxMz(),
                    (float)(moi.getRt()-moi.getWidthLeft()),
                    (float)(moi.getRt()+moi.getWidthRight()),
                    (float)moi.getMz(),
                    orderedByAlignments.getInt(i)
            ));
        }
        // merge overlapping traces
        storage.traceSeparationAndUnificationMap.resolveOverlappingRectangles(
                new Deviation(9, 0.003),
                true,
                (m,xs)->{}
        );
    }

    private static int numberOfAlignments(MassOfInterest moi) {
        if (moi instanceof TraceAligner.MergedMassOfInterest) return ((TraceAligner.MergedMassOfInterest) moi).mergedRts.length;
        else return 1;
    }

    private void mergeTraces(MergedTrace merged, ContiguousTrace trace, PairwiseScanPointMapping pw) {
        final int fromIndex = pw.right2left[trace.startId()];
        final int toIndex = trace.endId()+1 < pw.right2left.length ? pw.right2left[trace.endId()+1] : pw.left2right.length;
        merged.extend(fromIndex, toIndex-1);
        for (int id = trace.startId(); id < trace.endId(); ++id) {
            if (trace.mz(id)>0) {
                final int targetId = pw.right2left[id];
                final int shift = merged.startId();
                final int targetEndId = (id+1) < pw.right2left.length ? pw.right2left[id+1] : pw.left2right.length;
                for (int j=targetId; j < targetEndId; ++j) {
                    merged.mergePoint(j-shift, trace.mz(id), trace.intensity(id)); // todo: maybe interpolation?
                }
            }
        }
        storage.mergedTraces.put(merged.getUid(), merged); // update trace
    }


    static class PairwiseScanPointMapping {

        private int[] left2right, right2left;
        protected double[] rtRights;

        public PairwiseScanPointMapping(ScanPointMapping left, ScanPointMapping right, UnivariateFunction right2leftRecal) {
            if (right2leftRecal==null) right2leftRecal = new Identity();
            this.left2right = new int[left.length()];
            this.right2left = new int[right.length()];
            Arrays.fill(left2right, 0);
            Arrays.fill(right2left, 0);
            this.rtRights = new double[right.length()];
            int j=0,i=0;
            for (j=0; j < right2left.length; ++j) {
                rtRights[j] = right2leftRecal.value(right.getRetentionTimeAt(j));
                final double rtRight =  rtRights[j];
                while (i < left2right.length-1 && Math.abs(left.getRetentionTimeAt(i+1)-rtRight) < Math.abs(left.getRetentionTimeAt(i)-rtRight)) {
                    ++i;
                }
                right2left[j] = i;
            }
            i=0;j=0;
            for (i=0; i < left2right.length; ++i) {
                final double rtleft = left.getRetentionTimeAt(i);
                while (j < right2left.length-1 && Math.abs(rtRights[j+1]-rtleft) < Math.abs(rtRights[j]-rtleft)) {
                    ++j;
                }
                left2right[i] = j;
            }
        }
        private int reverseRtIndex(double y) {
            int i = Arrays.binarySearch(rtRights, y);
            if (i < 0) i = -(i+1);
            if (i>=rtRights.length) return rtRights.length-1;
            return i;

        }
    }


}
