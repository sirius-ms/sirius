package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation;
import de.unijena.bioinf.lcms.utils.Tracker;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The assignment reduced to its simplest defensible form: fixed thresholds, no competition between precursors, and
 * every fragment given to each precursor that accepts it at its full height.
 * <p>
 * The third of the strategies, and the one against which the other two have to justify their machinery. It asks of
 * each precursor and each fragment separately whether their elution profiles agree well enough over enough scan
 * points, and if they do the fragment is a peak of that precursor's spectrum. Nothing is normalised, nothing is
 * divided, and a fragment that two co-eluting precursors both accept appears in both spectra unchanged.
 * <p>
 * Its selectivity comes almost entirely from three absolute numbers rather than from anything measured on the sample:
 * a precursor has to be intense, a fragment has to be intense, and their correlation has to clear a fixed bar over a
 * fixed number of points. That is a real strategy rather than a straw man, and it has two properties the others do
 * not. Because it never divides a fragment, its intensities are the measured ones; and because it never lets one
 * precursor's claim weaken another's, a crowded isolation window costs it nothing - which is exactly where
 * {@link CorrelationAssignmentStrategy} loses fragments to a share that hundreds of candidates have diluted.
 * <p>
 * What it cannot do is say anything about ambiguity. Two precursors that co-elute in one window receive identical
 * spectra and no record that they are indistinguishable, so a reader has no way to tell a compound's own fragments
 * from its neighbour's. {@link GroupedAssignmentStrategy} exists to answer that; this one does not try.
 * <p>
 * There is no second stage. Stage two revisits an assignment over the final feature boundaries with all samples
 * available, and this strategy has no soft decisions to revisit - a fragment either cleared the bar or it did not.
 * So {@code refine} is left at the interface's default and the spectra written per sample are the final ones.
 */
public class UncontestedAssignmentStrategy implements DiaAssignmentStrategy {

    /**
     * How intense a precursor has to be before it is given a spectrum at all.
     * <p>
     * The single most consequential number here. It is not a detectability threshold - measured on a SWATH file
     * through {@link DiaPrecursorSurvival}, fragments become visible about thirty times lower than this - so it does
     * not decide what can be seen, but what is worth attempting. Restricting the attempt to intense precursors is
     * what keeps a strategy without competition from filling a crowded window with duplicates.
     */
    @Getter @Setter
    private double minPrecursorIntensity = 50_000d;

    /** how intense a fragment's apex has to be within the precursor's window */
    @Getter @Setter
    private double minFragmentIntensity = 5_000d;

    /**
     * The fraction of {@link #minFragmentIntensity} a scan point has to reach for a fragment chromatogram to be
     * followed through it. Picking needs a level below the apex requirement or a peak could never be entered.
     */
    @Getter @Setter
    private double pickingFraction = 0.2d;

    /** how well the two profiles have to agree, as a plain correlation and not a calibrated one */
    @Getter @Setter
    private double minCorrelation = 0.8d;

    /** over how many of the channel's scan points that correlation has to be computed */
    @Getter @Setter
    private int minCorrelatedPoints = 5;

    /**
     * The fragments each precursor of a graph accepts.
     * <p>
     * Separated from everything to do with storage so that the rule itself can be tested on a graph built by hand.
     * Each pair is judged alone: there is no reference to what any other precursor claimed, which is the whole of
     * what distinguishes this strategy.
     *
     * @return per precursor of the graph, the indices of the fragments it accepts
     */
    static List<IntArrayList> select(DiaAssignmentGraph graph, double minCorrelation, int minCorrelatedPoints,
                                     double minFragmentIntensity) {
        final List<IntArrayList> accepted = new ArrayList<>(graph.getPrecursors().size());
        for (int p = 0; p < graph.getPrecursors().size(); ++p) accepted.add(new IntArrayList());
        for (DiaAssignmentGraph.PrecursorEdge edge : graph.getPrecursorEdges()) {
            final DiaTraceCorrelation.Result correlation = edge.correlation();
            if (!Double.isFinite(correlation.correlation())) continue;
            if (correlation.datapoints() < minCorrelatedPoints) continue;
            if (correlation.correlation() < minCorrelation) continue;
            final PrecursorFeature precursor = graph.getPrecursors().get(edge.precursorIndex());
            final ContiguousTrace fragment = graph.getFragments().get(edge.fragmentIndex());
            if (precursor.getWindow().apexIntensityIn(fragment) < minFragmentIntensity) continue;
            accepted.get(edge.precursorIndex()).add(edge.fragmentIndex());
        }
        return accepted;
    }

    @Override
    public DiaReport assign(ProcessedSample sample, DiaScanIndex index, Tracker tracker) {
        int spectra = 0, peaks = 0, without = 0, precursorsConsidered = 0, precursorsTooWeak = 0;
        final StringBuilder report = new StringBuilder();
        for (DiaChannel channel : index.getChannels()) {
            // the precursors worth attempting, which is where this strategy's selectivity lives
            final List<PrecursorFeature> all = PrecursorFeature.of(channel, sample.getStorage().getTraceStorage());
            final List<PrecursorFeature> precursors = new ArrayList<>(all.size());
            for (PrecursorFeature feature : all) {
                if (feature.getApexIntensity() >= minPrecursorIntensity) precursors.add(feature);
                else ++precursorsTooWeak;
            }
            precursorsConsidered += precursors.size();
            if (precursors.isEmpty()) continue;

            // and the fragments, followed across at least as many points as the correlation will need
            final float pickingLevel = (float) (pickingFraction * minFragmentIntensity);
            final DiaChannelFragments.Result picked = DiaChannelFragments.pickAll(sample, channel, pickingLevel,
                    new NoSegmentation(), minCorrelatedPoints, tracker);
            final List<ContiguousTrace> fragments = new ArrayList<>(picked.traces().size());
            for (ContiguousTrace trace : picked.traces()) {
                if (trace.apexIntensity() >= minFragmentIntensity) fragments.add(trace);
            }
            if (fragments.isEmpty()) continue;

            final DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel, precursors, fragments);
            final List<IntArrayList> accepted = select(graph, minCorrelation, minCorrelatedPoints,
                    minFragmentIntensity);
            for (int p = 0; p < precursors.size(); ++p) {
                final IntArrayList mine = accepted.get(p);
                if (mine.isEmpty()) {
                    ++without;
                    continue;
                }
                final PrecursorFeature precursor = precursors.get(p);
                final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(mine.size());
                for (int fragmentIndex : mine) {
                    final ContiguousTrace fragment = fragments.get(fragmentIndex);
                    // the measured height in the window, not a share of it: nothing was divided
                    buffer.addPeak(fragment.averagedMz(), precursor.getWindow().apexIntensityIn(fragment));
                }
                DiaPseudoSpectra.writeSpectrum(sample, channel, precursor, new SimpleSpectrum(buffer));
                ++spectra;
                peaks += buffer.size();
            }
        }
        sample.getStorage().commit();
        report.append(String.format(Locale.US, "uncontested assignment: %d precursors attempted, %d too weak, "
                        + "correlation at least %.2f over at least %d points, fragments at least %.0f%n",
                precursorsConsidered, precursorsTooWeak, minCorrelation, minCorrelatedPoints, minFragmentIntensity));
        return new DiaReport(index, spectra, peaks, without, report.toString());
    }
}
