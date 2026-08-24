package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.utils.Tracker;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the result of the deconvolution back as ordinary MS2 spectra.
 * <p>
 * Each precursor feature gets one spectrum per channel, built from the fragments assigned to it, registered with
 * {@link de.unijena.bioinf.lcms.spectrum.SpectrumStorage#addMs2Spectrum} and pointed at its MS1 trace by an
 * ordinary {@link MsMsTraceReference}. From there the rest of the pipeline needs no changes at all: merging,
 * feature extraction and the consensus across samples all work off those references, and a spectrum per collision
 * energy is exactly what {@code MergeGreedyStrategy} already expects.
 * <p>
 * Two details of the synthetic header are worth spelling out. Its precursor m/z is the averaged mass of the MS1
 * trace rather than the centre of the isolation window, so downstream code that looks for the precursor in the MS1
 * scan finds the real thing. And its isolation window is the one reported <em>for that precursor</em>, from
 * {@link DiaChannel#isolationWindowFor}, not the acquisition window, which for a multiplexed channel would not
 * even be contiguous.
 * <p>
 * Peak intensities are the share of the fragment that belongs to this precursor, from
 * {@link FragmentIntensityShares}, not the fragment's full height. A fragment that several co-eluting precursors
 * could have produced is split between them, so the spectra together account for the ion current that was measured
 * rather than a multiple of it, and a peak's height says how much of it belongs where it was written.
 * <p>
 * Note that the spectra written here become part of the sample's MS2 spectra, and a synthetic header carries an
 * isolation window of its own. Anything reconstructing the acquisition scheme therefore has to ignore them, which
 * is what {@link #isDeconvoluted} is for: without it a pseudo spectrum folds back into the channel it came from and
 * inflates it, and for a multiplexed channel it invents new single window channels that were never acquired.
 */
public final class DiaPseudoSpectra {

    /**
     * How likely a fragment has to belong to a precursor to appear in its spectrum. Deliberately stricter than
     * the threshold {@link FragmentAssignment} stores at: a peak that belongs here with one per cent probability
     * is noise in a spectrum. Once the probabilities are persisted alongside the peaks this can be lowered and the
     * decision left to whoever reads them.
     */
    public static final double DEFAULT_MIN_PROBABILITY = 0.1;

    /**
     * Marks the source id of a spectrum that was computed rather than acquired. Anything reconstructing the
     * acquisition scheme has to ignore these: a pseudo spectrum carries an isolation window of its own, so it
     * would otherwise be counted as if the instrument had measured it. {@link DiaScanIndexBuilder} skips them by
     * this prefix, which no acquired header can carry - a header without a source id reports {@code scan=<number>}.
     */
    public static final String SOURCE_ID_PREFIX = "deconvoluted=";

    /**
     * @return true if this spectrum was produced by the deconvolution rather than measured by the instrument
     */
    public static boolean isDeconvoluted(Ms2SpectrumHeader header) {
        return header.getSourceId().startsWith(SOURCE_ID_PREFIX);
    }

    public record Result(int spectraWritten, int peaksWritten, int featuresWithoutFragments) {
        public String describe() {
            return String.format(Locale.US, "DIA pseudo spectra: %d written with %d peaks, %d features got none",
                    spectraWritten, peaksWritten, featuresWithoutFragments);
        }
    }

    private DiaPseudoSpectra() {
    }

    public static Result write(ProcessedSample sample, DiaDeconvolution.Result deconvolution) {
        return write(sample, deconvolution, DEFAULT_MIN_PROBABILITY);
    }

    public static Result write(ProcessedSample sample, DiaDeconvolution.Result deconvolution, double minProbability) {
        return write(sample, deconvolution, minProbability, new Tracker.NOOP());
    }

    public static Result write(ProcessedSample sample, DiaDeconvolution.Result deconvolution, double minProbability,
                               Tracker tracker) {
        return write(sample, deconvolution, minProbability, 0d, tracker);
    }

    /**
     * @param minRelativeProbability keep a candidate only if its probability is at least this fraction of the best
     *                               candidate's, which bounds the candidate set by how much worse an explanation it is
     *                               rather than by how many competitors it happens to have. Zero keeps all of them.
     */
    public static Result write(ProcessedSample sample, DiaDeconvolution.Result deconvolution, double minProbability,
                               double minRelativeProbability, Tracker tracker) {
        final boolean tracked = tracker.tracksDia();
        int spectra = 0, peaks = 0, without = 0;
        for (DiaDeconvolution.ChannelResult channel : deconvolution.getChannels()) {
            final List<PrecursorFeature> precursors = channel.getGraph().getPrecursors();
            final List<ContiguousTrace> fragments = channel.getGraph().getFragments();

            // fragments per precursor feature, from the assignments, together with the share of the fragment that
            // belongs to that precursor: a fragment assigned to several of them is split between them rather than
            // written into each of their spectra at its full height
            final List<IntArrayList> byPrecursor = new ArrayList<>(precursors.size());
            final List<DoubleArrayList> intensities = new ArrayList<>(precursors.size());
            for (int p = 0; p < precursors.size(); ++p) {
                byPrecursor.add(new IntArrayList());
                intensities.add(new DoubleArrayList());
            }
            for (int f = 0; f < fragments.size(); ++f) {
                final FragmentAssignment assignment = channel.assignmentOf(f);
                final IntArrayList assignedTo = new IntArrayList(assignment.size());
                final List<PrecursorFeature> candidates = new ArrayList<>(assignment.size());
                final double best = assignment.isEmpty() ? 0d : assignment.probabilityOfMostLikely();
                for (int rank = 0; rank < assignment.size(); ++rank) {
                    if (assignment.probabilityAt(rank) < minProbability) continue;  // sorted, but be explicit
                    if (assignment.probabilityAt(rank) < minRelativeProbability * best) continue;
                    assignedTo.add(assignment.precursorIndexAt(rank));
                    candidates.add(precursors.get(assignment.precursorIndexAt(rank)));
                }
                if (assignedTo.isEmpty()) continue;
                final double[] shares = FragmentIntensityShares.of(channel.getChannel(), candidates,
                        fragments.get(f));
                for (int k = 0; k < assignedTo.size(); ++k) {
                    if (tracked) {
                        tracker.diaPeakWritten(sample, channel.getChannel().describe(),
                                precursors.get(assignedTo.getInt(k)).getMz(), fragments.get(f).averagedMz(),
                                shares[k], assignment.probabilityAt(k), shares[k] > 0,
                                "the intensity factorisation gave this precursor no share of the fragment");
                    }
                    if (shares[k] <= 0) continue;
                    byPrecursor.get(assignedTo.getInt(k)).add(f);
                    intensities.get(assignedTo.getInt(k)).add(shares[k]);
                }
            }

            for (int p = 0; p < precursors.size(); ++p) {
                final IntArrayList assigned = byPrecursor.get(p);
                if (assigned.isEmpty()) {
                    ++without;
                    continue;
                }
                final PrecursorFeature precursor = precursors.get(p);
                final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(assigned.size());
                // what the fragments measured in this window before the factorisation decided how much of it this
                // precursor may claim, which is what the attributed current has to be read against
                double attributed = 0d, measured = 0d, base = 0d;
                for (int k = 0; k < assigned.size(); ++k) {
                    final ContiguousTrace fragment = fragments.get(assigned.getInt(k));
                    final double intensity = intensities.get(p).getDouble(k);
                    buffer.addPeak(fragment.averagedMz(), intensity);
                    attributed += intensity;
                    measured += precursor.getWindow().apexIntensityIn(fragment);
                    base = Math.max(base, intensity);
                }
                if (buffer.isEmpty()) {
                    ++without;
                    continue;
                }
                final SimpleSpectrum written = new SimpleSpectrum(buffer);
                if (tracked) {
                    tracker.diaSpectrumQuality(sample, channel.getChannel().describe(),
                            new Tracker.DiaSpectrumQuality("per sample", precursor.getMz(),
                                    precursor.getRetentionTime(), buffer.size(), attributed, measured, base,
                                    channel.getNoiseLevel(), precursor.getApexIntensity(), Double.NaN,
                                    channel.getMeanLogPValue()[p], channel.getBestLogPValueSum()[p], Double.NaN,
                                    Double.NaN, 1));
                }
                writeSpectrum(sample, channel.getChannel(), precursor, written);
                ++spectra;
                peaks += buffer.size();
                // the other members of this precursor's group, if it speaks for any. They receive the same peaks
                // because the channel cannot say which of them produced them, which is the honest answer rather
                // than a split invented to look like one.
                for (PrecursorFeature member : channel.getCoReported().get(p)) {
                    writeSpectrum(sample, channel.getChannel(), member, written);
                    ++spectra;
                    peaks += buffer.size();
                }
            }
        }
        return new Result(spectra, peaks, without);
    }

    /** package visible so a strategy that builds its spectra differently still registers them identically */
    static void writeSpectrum(ProcessedSample sample, DiaChannel channel, PrecursorFeature precursor,
                                      SimpleSpectrum spectrum) {
        final double precursorMz = precursor.getMz();
        final int parentId = precursor.getSegment().apex;
        // the scan of this channel closest to the feature's apex: the fragments really were measured there, so it
        // is the honest provenance for a spectrum that has no single scan of its own
        final int apexChannelIndex = precursor.getApexChannelIndex();
        final int ms2ScanId = sample.getStorage().getSpectrumStorage()
                .ms2SpectrumHeader(channel.ms2UidAt(apexChannelIndex)).getScanId();
        final IsolationWindow window = channel.isolationWindowFor(precursorMz);

        // marked as data independent, which is what decides later that its peaks may belong to a co-eluting
        // neighbour and that the checks for that failure mode apply
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(
                SOURCE_ID_PREFIX + ms2ScanId, ms2ScanId, sample.getPolarity(), 2, true,
                channel.getCollisionEnergy(), window, parentId, precursorMz, precursorMz,
                precursor.getRetentionTime()).asDataIndependent();
        final Ms2SpectrumHeader stored = sample.getStorage().getSpectrumStorage().addMs2Spectrum(header, spectrum);
        sample.getStorage().getTraceStorage().assignTraceForMs2(new MsMsTraceReference(
                stored.getUid(), precursor.getTraceUid(), parentId, ms2ScanId));
    }
}
