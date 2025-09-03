package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.AlignedMoI;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.traceextractor.MassOfInterestConfidenceEstimatorStrategy;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.Range;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class TrackFeatureToFile implements Tracker{
    private final PrintStream out;
    private final double fromMz, toMz;
    private final Range<Double> retentionTimeToTrack;

    public TrackFeatureToFile(File outFile, Range<Double> massToTrack, Range<Double> retentionTimeToTrack) {
        try {
            this.out = new PrintStream(outFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
        fromMz = massToTrack.getMinimum();
        toMz = massToTrack.getMaximum();
        this.retentionTimeToTrack = retentionTimeToTrack;
    }

    public TrackFeatureToFile(File outFile, double massToTrack, Range<Double> retentionTimeToTrack) {
        this(outFile, Range.of(massToTrack - new Deviation(10).absoluteFor(massToTrack), massToTrack + new Deviation(10).absoluteFor(massToTrack)), retentionTimeToTrack);
    }

    @Override
    public void tracePicked(double mz, double rt, ProcessedSample sample, ContiguousTrace trace) {
        if (tracked(mz, rt)) {
            ScanPointMapping mapping = sample.getMapping();
            this.out.println(String.format(Locale.US,"A trace picked with m/z = %f at rt = %f in sample %s. The trace contains the following segments: %s", mz, rt, sample.getRun().getName(), Arrays.stream(trace.getSegments()).map(s->toStr(s,mapping)).collect(Collectors.joining(", "))));
        }
    }

    private String toStr(TraceSegment s, ScanPointMapping mapping) {
        double leftEdge = mapping.getRetentionTimeAt(s.leftEdge);
        double apex = mapping.getRetentionTimeAt(s.apex);
        double rightEdge = mapping.getRetentionTimeAt(s.rightEdge);
        return String.format("[%.1f-%.1f s] with apex at %.1f s",leftEdge, rightEdge, apex);
    }

    private boolean tracked(double mz, double rt) {
        return (mz >= fromMz && mz <= toMz && retentionTimeToTrack.contains(rt));
    }
    private boolean tracked(double mz, double minrt, double maxrt) {
        return (mz >= fromMz && mz <= toMz && retentionTimeToTrack.isOverlappedBy(Range.of(minrt, maxrt)));
    }

    @Override
    public void moiAccepted(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (tracked(mz, retentionTime)) {
            float confidence = moi.getConfidence();
            String confidenceLabel;
            if (confidence >= MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) confidenceLabel="highest, use for first alignment pass";
            else if (confidence >= MassOfInterestConfidenceEstimatorStrategy.KEEP_FOR_ALIGNMENT) confidenceLabel="accepted, keep feature alive";
            else if (confidence >= MassOfInterestConfidenceEstimatorStrategy.ACCEPT) confidenceLabel="accepted, for now";
            else confidenceLabel = "rejected";
            final double intensity = trace.intensity(moi.getScanId());
            final double relativeIntensity = moi.getIntensity();

            this.out.println(String.format(Locale.US,"accept potential feature with  m/z = %f, rt = %f s, intensity = %.1f (relative: %e), confidence = %f (%s) in sample %s", mz, retentionTime, intensity, relativeIntensity, confidence, confidenceLabel, sample.getRun().getName()));
        }
    }

    @Override
    public void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (tracked(mz, retentionTime)) {
            float confidence = moi.getConfidence();
            String confidenceLabel;
            if (confidence >= MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) confidenceLabel="highest, use for first alignment pass";
            else if (confidence >= MassOfInterestConfidenceEstimatorStrategy.KEEP_FOR_ALIGNMENT) confidenceLabel="accepted, keep feature alive";
            else if (confidence >= MassOfInterestConfidenceEstimatorStrategy.ACCEPT) confidenceLabel="accepted, for now";
            else confidenceLabel = "rejected";
            final double intensity = trace.intensity(moi.getScanId());
            final double relativeIntensity = moi.getIntensity();
            this.out.println(String.format(Locale.US,"reject potential feature with  m/z = %f, rt = %f s, intensity = %.1f (relative: %e), confidence = %f (%s) in sample %s", mz, retentionTime, intensity,relativeIntensity, moi.getConfidence(),  confidenceLabel, sample.getRun().getName()));
        }
    }

    @Override
    public void alignMois(ProcessedSample rightSample, MoI left, MoI right) {
        if (tracked(left.getMz(), left.getRetentionTime())) {
            this.out.println(String.format(Locale.US,"align two potential features with m/z = %f, rt left = %f s, rt right = %f s in sample %s", left.getMz(), left.getRetentionTime(), right.getRetentionTime(), rightSample.getRun().getName()));
        }
    }

    @Override
    public void unalignedMoI(ProcessedSample s, MoI moI) {
        if (tracked(moI.getMz(), moI.getRetentionTime())) {
            this.out.println(String.format(Locale.US,"cannot align a potential feature with m/z = %f and rt = %f s in sample %s", moI.getMz(), moI.getRetentionTime(), moI.getRetentionTime(), s.getRun().getName()));
        }
    }

    @Override
    public void moiDeleted(MoI moi) {
        if (tracked(moi.getMz(), moi.getRetentionTime())) {
            if (moi instanceof AlignedMoI) {
                AlignedMoI amo = (AlignedMoI) moi;
                this.out.println(String.format(Locale.US,"forget potential feature with m/z = %f and rt = %f s which was aligned in only %d samples", moi.getMz(), moi.getRetentionTime(), amo.getAligned().length));
            } else {
                this.out.println(String.format(Locale.US,"forget potential feature with m/z = %f and rt = %f s because it did not align to any sample", moi.getMz(), moi.getRetentionTime()));
            }
        }
    }

    @Override
    public void createRect(ProcessedSample sample, Rect r) {
        if (tracked(r.avgMz, r.minRt, r.maxRt)) {
            this.out.println(String.format(Locale.US,"Prepare merging a feature with mz=%f..%f and rt=%f..%f s",
                    r.minMz,r.maxMz,r.minRt,r.maxRt));
        }
    }

    @Override
    public void emptyRect(ProcessedSample sample, Rect r) {
        if (tracked(r.avgMz, r.minRt, r.maxRt)) {
            this.out.println(String.format(Locale.US,"do not find any traces within mz=%f..%f, rt=%f..%f s in sample %s",
                    r.minMz,r.maxMz,r.minRt,r.maxRt,sample.getRun().getName()));
        }
    }

    @Override
    public void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample) {
        ScanPointMapping m = merged.getMapping();
        double rt = m.getRetentionTimeAt(projectedTrace.getProjectedApex());
        double rt0 = m.getRetentionTimeAt(projectedTrace.getProjectedStartId());
        double rt1 = m.getRetentionTimeAt(projectedTrace.getProjectedEndId());
        if (tracked(projectedTrace.getAveragedMz(), rt)) {
            this.out.println(String.format(Locale.US,"merge the trace with m/z = %f and rt = %f..%f s (apex at %f s) from sample %s into the merged sample.",  projectedTrace.getAveragedMz(), rt0,rt1,rt, sample.getRun().getName()));
        }
    }

    @Override
    public void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids) {
        double rtA = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedStartId());
        double rtB = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedEndId());
        if (tracked(projectedTrace.getAveragedMz(), rtA, rtB)) {
            if (ids.length==0) {
                this.out.printf(Locale.US, "assign NO MS/MS to trace m/z = %f, rt = %f..%f s in sample %s%n", projectedTrace.getAveragedMz(), rtA, rtB, sample.getRun().getName());

            } else {
                this.out.printf(Locale.US, "assign MS/MS to trace m/z = %f, rt = %f..%f s in sample %s with MS/MS scan ids are: %s%n", projectedTrace.getAveragedMz(), rtA, rtB, sample.getRun().getName(), Arrays.toString(ids));
            }
        }
    }

    @Override
    public void rejectedForFeatureExtraction(@Nullable Rect r, @Nullable MergedTrace merged) {
        if (merged==null && r==null) return;
        if (merged==null) {
            if (tracked(r.avgMz, r.minRt, r.maxRt)) {
                this.out.println(String.format(Locale.US,"A merged trace with m/z = %f..%f, rt = %f..%f s is rejected because it does not contain any peaks", r.minMz,r.maxMz,r.minRt,r.maxRt));
            }
        } else {
            double rt = merged.retentionTime(merged.apex());
            if (tracked(merged.averagedMz(), rt)) {
                this.out.println(String.format(Locale.US,"A merged trace with m/z = %f, rt = %f s is rejected because it does not contain any peaks", merged.averagedMz(), rt));
            }
        }
    }

    @Override
    public void noFeatureFound(MergedTrace mergedTrace) {
        double rt = mergedTrace.retentionTime(mergedTrace.apex());
        if (tracked(mergedTrace.averagedMz(), rt)) {
            this.out.println(String.format(Locale.US,"A merged trace with m/z = %f, rt = %f s does not contain any feature", mergedTrace.averagedMz(), rt));
        }
    }

    @Override
    public void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace) {
        double rt = mergedSample.getMapping().getRetentionTimeAt(mergedTrace.apex());
        if (tracked(mergedTrace.averagedMz(), rt)) {
            this.out.println(String.format(Locale.US,"Start peak picking for trace with m/z = %f, rt = %f s", mergedTrace.averagedMz(), rt));
        }
    }

    @Override
    public void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features) {
        for (AlignedFeatures f : features) {
            if (tracked(f.getAverageMass(), f.getRetentionTime().getMiddleTime())) {
                LongOpenHashSet set = new LongOpenHashSet((f.getFeatures().get()).stream().mapToLong(AbstractFeature::getRunId).toArray());
                this.out.println(String.format(Locale.US,"Feature picked with m/z = %f and rt = %f..%f s with apex is at %f s. Feature is contained in the following samples: %s", mergedTrace.averagedMz(), f.getRetentionTime().getStartTime(),
                        f.getRetentionTime().getEndTime(),f.getRetentionTime().getMiddleTime(), Arrays.stream(mergedTrace.getSamples()).filter(x->set.contains(x.getRun().getRunId())).map(x->x.getRun().getName()).collect(Collectors.joining(", "))));
            }
        }
    }
}
