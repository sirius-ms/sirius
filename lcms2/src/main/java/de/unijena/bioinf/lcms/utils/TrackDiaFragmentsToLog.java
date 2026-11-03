package de.unijena.bioinf.lcms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Follows a few fragment masses of one DIA precursor through every stage that could lose them.
 * <p>
 * The stages are asked in the order a peak passes them - present in a channel scan, followed into a chromatogram,
 * admitted as a candidate of the precursor, judged by co-elution and specificity, given a probability, written into
 * the pseudo spectrum - so the first stage that reports nothing for a mass is where it was lost. That is the whole
 * point of doing it this way rather than with another aggregate: a rate tells you how much is lost, and the answer
 * needed here is where.
 * <p>
 * The masses to watch have to come from outside, and the useful ones come from a comparison against a reference
 * measurement: fragments that another tool found in the same file and that a DDA run of the same compound confirms
 * are known to be in the raw data, so anything that happens to them happens here.
 * <p>
 * Output goes to standard output rather than a logger. This is an instrument to be read while it runs, and the
 * interesting runs are tests.
 */
public class TrackDiaFragmentsToLog extends Tracker.NOOP {

    private final double precursorMz, fromPrecursorMz, toPrecursorMz;
    private final double retentionTimeFrom, retentionTimeTo;
    private final double[] fragmentMasses;
    private final Deviation deviation;

    /** what has been seen, so a mass that is never mentioned again can be told from one that was never there */
    private final int[] scanPeaks, tracesPicked, judgements, assigned, written;
    private final List<String> lines = new ArrayList<>();

    /**
     * @param precursorMz       the precursor whose spectrum is missing the masses
     * @param retentionTime     where it elutes, in seconds
     * @param rtTolerance       how far from that a scan or a trace still belongs to this compound
     * @param fragmentMasses    the masses to follow
     */
    public TrackDiaFragmentsToLog(double precursorMz, double retentionTime, double rtTolerance,
                                  double... fragmentMasses) {
        this(precursorMz, retentionTime, rtTolerance, new Deviation(20, 0.005), fragmentMasses);
    }

    public TrackDiaFragmentsToLog(double precursorMz, double retentionTime, double rtTolerance, Deviation deviation,
                                  double... fragmentMasses) {
        this.precursorMz = precursorMz;
        this.deviation = deviation;
        this.fromPrecursorMz = precursorMz - deviation.absoluteFor(precursorMz);
        this.toPrecursorMz = precursorMz + deviation.absoluteFor(precursorMz);
        this.retentionTimeFrom = retentionTime - rtTolerance;
        this.retentionTimeTo = retentionTime + rtTolerance;
        this.fragmentMasses = fragmentMasses.clone();
        this.scanPeaks = new int[fragmentMasses.length];
        this.tracesPicked = new int[fragmentMasses.length];
        this.judgements = new int[fragmentMasses.length];
        this.assigned = new int[fragmentMasses.length];
        this.written = new int[fragmentMasses.length];
    }

    @Override
    public boolean tracksDia() {
        return true;
    }

    /** which of the watched masses this is, or -1 */
    private int fragmentIndex(double mz) {
        for (int k = 0; k < fragmentMasses.length; ++k) {
            if (Math.abs(mz - fragmentMasses[k]) <= deviation.absoluteFor(fragmentMasses[k])) return k;
        }
        return -1;
    }

    private boolean withinElution(double retentionTime) {
        return retentionTime >= retentionTimeFrom && retentionTime <= retentionTimeTo;
    }

    private boolean isTrackedPrecursor(double mz) {
        return mz >= fromPrecursorMz && mz <= toPrecursorMz;
    }

    @Override
    public void ms2ScanPeak(ProcessedSample sample, String channel, double retentionTime, double mz,
                            float intensity, float noiseLevel) {
        if (!withinElution(retentionTime)) return;
        final int index = fragmentIndex(mz);
        if (index < 0) return;
        ++scanPeaks[index];
        add("scan   %-16s %10.4f  %.1f s  intensity %10.0f  noise %8.0f  %s", channel, mz, retentionTime,
                intensity, noiseLevel, intensity > noiseLevel ? "seeded" : "BELOW NOISE, not seeded");
    }

    @Override
    public void ms2TracePicked(ProcessedSample sample, String channel, double seedMz, double seedRetentionTime,
                               ContiguousTrace trace) {
        if (!withinElution(seedRetentionTime)) return;
        final int index = fragmentIndex(seedMz);
        if (index < 0) return;
        if (trace == null) {
            add("trace  %-16s %10.4f  %.1f s  NOT PICKED, seed could not be followed", channel, seedMz,
                    seedRetentionTime);
            return;
        }
        ++tracesPicked[index];
        add("trace  %-16s %10.4f  %.1f s  %d scan points, apex intensity %.0f", channel, trace.averagedMz(),
                seedRetentionTime, trace.endId() - trace.startId() + 1, trace.apexIntensity());
    }

    @Override
    public void diaFragmentJudged(ProcessedSample sample, String channel, DiaFragmentJudgement judgement) {
        if (!isTrackedPrecursor(judgement.precursorMz())
                || !withinElution(judgement.precursorRetentionTime())) return;
        final int index = fragmentIndex(judgement.fragmentMz());
        if (index < 0) return;
        ++judgements[index];
        if (judgement.probability() > 0) ++assigned[index];
        add("judged %-16s %10.4f  precursor %.4f at %.1f s (intensity %.0f), %d points, r = %.3f, log p = %.2f, "
                        + "specificity = %.3f, weight = %.4f, %d competing -> probability %.4f, share %.4f: %s",
                channel, judgement.fragmentMz(), judgement.precursorMz(), judgement.precursorRetentionTime(),
                judgement.precursorIntensity(), judgement.datapoints(), judgement.correlation(),
                judgement.logPValue(), judgement.specificity(), judgement.weight(),
                judgement.competingPrecursors(), judgement.probability(), judgement.intensityShare(),
                judgement.outcome());
    }

    @Override
    public void diaFragmentCompetition(ProcessedSample sample, String channel,
                                       DiaFragmentCompetition competition) {
        final int index = fragmentIndex(competition.fragmentMz());
        if (index < 0 || !withinElution(competition.fragmentRetentionTime())) return;
        add("field  %-16s %10.4f  %d scan points, %d candidates, total weight %.1f (unassigned %.1f): "
                        + "strongest %.4f at m/z %.4f; weight from candidates above half the strongest intensity "
                        + "%.1f (%d of them), above a tenth %.1f (%d), below a tenth %.1f",
                channel, competition.fragmentMz(), competition.scanPoints(), competition.candidates(),
                competition.totalWeight(), competition.unassignedWeight(), competition.strongestWeight(),
                competition.strongestMz(), competition.weightAboveHalf(), competition.countAboveHalf(),
                competition.weightAboveTenth(), competition.countAboveTenth(), competition.weightBelowTenth());
    }

    @Override
    public void diaPeakWritten(ProcessedSample sample, String channel, double precursorMz, double fragmentMz,
                               double intensity, double probability, boolean written, String outcome) {
        if (!isTrackedPrecursor(precursorMz)) return;
        final int index = fragmentIndex(fragmentMz);
        if (index < 0) return;
        if (written) ++this.written[index];
        add("write  %-16s %10.4f  precursor %.4f  intensity %10.0f  probability %.4f  %s", channel, fragmentMz,
                precursorMz, intensity, probability, written ? "WRITTEN" : "dropped: " + outcome);
    }

    private void add(String format, Object... arguments) {
        lines.add(String.format(Locale.US, format, arguments));
    }

    /**
     * Everything seen so far, and a table of how far each mass got.
     * <p>
     * The table is the answer and the lines are the evidence for it. A mass with scan peaks and no trace was lost in
     * picking, one with a trace and no judgement was never a candidate of this precursor, one judged but not assigned
     * lost to the significance floor or to a competitor, and one assigned but not written lost at the probability cut.
     */
    public String describe() {
        final StringBuilder out = new StringBuilder(String.format(Locale.US,
                "%n=== tracking precursor %.4f between %.1f and %.1f s ===%n", precursorMz, retentionTimeFrom,
                retentionTimeTo));
        for (String line : lines) out.append("  ").append(line).append('\n');
        out.append(String.format(Locale.US, "%n  %12s %10s %8s %10s %9s %8s%n", "fragment", "scan peaks", "traces",
                "judgements", "assigned", "written"));
        for (int k = 0; k < fragmentMasses.length; ++k) {
            out.append(String.format(Locale.US, "  %12.4f %10d %8d %10d %9d %8d%n", fragmentMasses[k], scanPeaks[k],
                    tracesPicked[k], judgements[k], assigned[k], written[k]));
        }
        return out.toString();
    }
}
