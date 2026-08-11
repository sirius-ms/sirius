package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
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
 * Peak intensities are the highest a fragment reaches inside the feature, not its intensity at the precursor's
 * apex: a channel samples once per cycle and therefore often does not measure at the apex at all. That also keeps
 * the meaning of a peak height the same as in a real MS2 spectrum, which matters because these spectra are
 * compared with DDA ones by cosine further down.
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
        int spectra = 0, peaks = 0, without = 0;
        for (DiaDeconvolution.ChannelResult channel : deconvolution.getChannels()) {
            final List<PrecursorFeature> precursors = channel.getGraph().getPrecursors();
            final List<ContiguousTrace> fragments = channel.getGraph().getFragments();

            // fragments per precursor feature, from the assignments
            final List<IntArrayList> byPrecursor = new ArrayList<>(precursors.size());
            for (int p = 0; p < precursors.size(); ++p) byPrecursor.add(new IntArrayList());
            for (int f = 0; f < fragments.size(); ++f) {
                final FragmentAssignment assignment = channel.assignmentOf(f);
                for (int rank = 0; rank < assignment.size(); ++rank) {
                    if (assignment.probabilityAt(rank) < minProbability) continue;  // sorted, but be explicit
                    byPrecursor.get(assignment.precursorIndexAt(rank)).add(f);
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
                for (int fragmentIndex : assigned) {
                    final ContiguousTrace fragment = fragments.get(fragmentIndex);
                    final double intensity = precursor.getWindow().apexIntensityIn(fragment);
                    if (intensity > 0) buffer.addPeak(fragment.averagedMz(), intensity);
                }
                if (buffer.isEmpty()) {
                    ++without;
                    continue;
                }
                writeSpectrum(sample, channel.getChannel(), precursor, new SimpleSpectrum(buffer));
                ++spectra;
                peaks += buffer.size();
            }
        }
        return new Result(spectra, peaks, without);
    }

    private static void writeSpectrum(ProcessedSample sample, DiaChannel channel, PrecursorFeature precursor,
                                      SimpleSpectrum spectrum) {
        final double precursorMz = precursor.getMz();
        final int parentId = precursor.getSegment().apex;
        // the scan of this channel closest to the feature's apex: the fragments really were measured there, so it
        // is the honest provenance for a spectrum that has no single scan of its own
        final int apexChannelIndex = precursor.getApexChannelIndex();
        final int ms2ScanId = sample.getStorage().getSpectrumStorage()
                .ms2SpectrumHeader(channel.ms2UidAt(apexChannelIndex)).getScanId();
        final IsolationWindow window = channel.isolationWindowFor(precursorMz);

        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(
                SOURCE_ID_PREFIX + ms2ScanId, ms2ScanId, sample.getPolarity(), 2, true,
                channel.getCollisionEnergy(), window, parentId, precursorMz, precursorMz,
                precursor.getRetentionTime());
        final Ms2SpectrumHeader stored = sample.getStorage().getSpectrumStorage().addMs2Spectrum(header, spectrum);
        sample.getStorage().getTraceStorage().assignTraceForMs2(new MsMsTraceReference(
                stored.getUid(), precursor.getTraceUid(), parentId, ms2ScanId));
    }
}
