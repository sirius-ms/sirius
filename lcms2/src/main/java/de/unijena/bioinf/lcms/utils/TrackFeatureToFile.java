package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.align.AlignedMoI;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Rect;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.Range;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

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
            this.out.println(String.format(Locale.US,"trace picked with m/z = %f, rt = %f, in sample %s. Trace has the following segments: %s", mz, rt, sample.getRun().getName(), Arrays.toString(trace.getSegments())));
        }
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
            this.out.println(String.format(Locale.US,"MOI accepted with  m/z = %f, rt = %f, confidence = %f in sample %s", mz, retentionTime, moi.getConfidence(),  sample.getRun().getName()));
        }
    }

    @Override
    public void moiRejected(double mz, double retentionTime, ProcessedSample sample, ContiguousTrace trace, MoI moi) {
        if (tracked(mz, retentionTime)) {
            this.out.println(String.format(Locale.US,"MOI REJECTED with  m/z = %f, rt = %f, confidence = %f in sample %s", mz, retentionTime, moi.getConfidence(),  sample.getRun().getName()));
        }
    }

    @Override
    public void alignMois(ProcessedSample rightSample, MoI left, MoI right) {
        if (tracked(left.getMz(), left.getRetentionTime())) {
            this.out.println(String.format(Locale.US,"ALIGN two MOIS with m/z %f, rt left = %f, rt right = %f in sample %s", left.getMz(), left.getRetentionTime(), right.getRetentionTime(), rightSample.getRun().getName()));
        }
    }

    @Override
    public void unalignedMoI(ProcessedSample s, MoI moI) {
        if (tracked(moI.getMz(), moI.getRetentionTime())) {
            this.out.println(String.format(Locale.US,"Cannot align MOI with m/z %f and rt %f in sample %s", moI.getMz(), moI.getRetentionTime(), moI.getRetentionTime(), s.getRun().getName()));
        }
    }

    @Override
    public void moiDeleted(MoI moi) {
        if (tracked(moi.getMz(), moi.getRetentionTime())) {
            if (moi instanceof AlignedMoI) {
                AlignedMoI amo = (AlignedMoI) moi;
                this.out.println(String.format(Locale.US,"DELETE MOI with m/z %f and rt %f which was aligned in %d samples", moi.getMz(), moi.getRetentionTime(), amo.getAligned().length));
            } else {
                this.out.println(String.format(Locale.US,"DELETE singleton MOI with m/z %f and rt %f", moi.getMz(), moi.getRetentionTime()));
            }
        }
    }

    @Override
    public void createRect(ProcessedSample sample, Rect r) {
        if (tracked(r.avgMz, r.minRt, r.maxRt)) {
            this.out.println(String.format(Locale.US,"create rect mz=%f..%f, rt=%f..%f",
                    r.minMz,r.maxMz,r.minRt,r.maxRt));
        }
    }

    @Override
    public void emptyRect(ProcessedSample sample, Rect r) {
        if (tracked(r.avgMz, r.minRt, r.maxRt)) {
            this.out.println(String.format(Locale.US,"do not find any traces in rect mz=%f..%f, rt=%f..%f in sample %s",
                    r.minMz,r.maxMz,r.minRt,r.maxRt,sample.getRun().getName()));
        }
    }

    @Override
    public void mergedTrace(ProcessedSample merged, ProcessedSample sample, Rect r, ProjectedTrace projectedTrace, MoI[] moisForSample) {
        double rt = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedApex());
        if (tracked(projectedTrace.getAveragedMz(), rt)) {
            this.out.println(String.format(Locale.US,"merge trace m/z = %f, rt = %f", projectedTrace.getAveragedMz(), rt));
        }
    }

    @Override
    public void assignMs2ToMergedTrace(ProcessedSample sample, ContiguousTrace[] sourceTraces, ProcessedSample merged, ProjectedTrace projectedTrace, MsMsTraceReference[] ids) {
        double rtA = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedStartId());
        double rtB = merged.getMapping().getRetentionTimeAt(projectedTrace.getProjectedEndId());
        if (tracked(projectedTrace.getAveragedMz(), rtA, rtB)) {
            this.out.println(String.format(Locale.US,"merge trace m/z = %f, rt = %f..%f in sample %s ASSIGN MS/MS: %s", projectedTrace.getAveragedMz(), rtA, rtB, sample.getRun().getName(), ids.toString()));
        }
    }

    @Override
    public void rejectedForFeatureExtraction(@Nullable Rect r, @Nullable MergedTrace merged) {
        if (merged==null && r==null) return;
        if (merged==null) {
            if (tracked(r.avgMz, r.minRt, r.maxRt)) {
                this.out.println(String.format(Locale.US,"trace REJECTED  with trace is null but rect is m/z = %f..%f, rt = %f..%f", r.minMz,r.maxMz,r.minRt,r.maxRt));
            }
        } else {
            double rt = merged.retentionTime(merged.apex());
            if (tracked(merged.averagedMz(), rt)) {
                this.out.println(String.format(Locale.US,"trace REJECTED  with m/z = %f, rt = %f", merged.averagedMz(), rt));
            }
        }
    }

    @Override
    public void noFeatureFound(MergedTrace mergedTrace) {
        double rt = mergedTrace.retentionTime(mergedTrace.apex());
        if (tracked(mergedTrace.averagedMz(), rt)) {
            this.out.println(String.format(Locale.US,"NO FEATURE FOUND for trace with m/z = %f, rt = %f", mergedTrace.averagedMz(), rt));
        }
    }

    @Override
    public void startExtractingCompounds(ProcessedSample mergedSample, MergedTrace mergedTrace) {
        double rt = mergedSample.getMapping().getRetentionTimeAt(mergedTrace.apex());
        if (tracked(mergedTrace.averagedMz(), rt)) {
            this.out.println(String.format(Locale.US,"Extract compounds for m/z = %f, rt = %f", mergedTrace.averagedMz(), rt));
        }
    }

    @Override
    public void importFeatures(MergedTrace mergedTrace, AlignedFeatures[] features) {
        double rt = mergedTrace.retentionTime(mergedTrace.apex());
        if (tracked(mergedTrace.averagedMz(), rt)) {
            this.out.println(String.format(Locale.US,"import trace with m/z = %f, rt = %f", mergedTrace.averagedMz(), rt));
        }
    }
}
