package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.utils.Tracker;

import java.util.List;

/**
 * Decides which fragments of a DIA measurement belong to which precursor, and writes the result back as ordinary
 * MS2 spectra.
 * <p>
 * An interface because there is more than one defensible way to make that decision and the difference between them is
 * not something to settle by argument. The correlation of a precursor with a fragment rests on the four to nine scan
 * points a channel spends on a peak, which is the weakest data in the experiment, so two approaches can be equally
 * reasonable and differ substantially in what they produce. The comparison against a DDA measurement of the same
 * sample scores any number of implementations over one common set of compounds, which is how the choice gets made.
 * <p>
 * Both phases are optional in the sense that a strategy decides what to do in them, but the <em>position</em> of each
 * is fixed by what the pipeline can offer at that point:
 * <ul>
 *     <li>{@link #assign} runs per sample before alignment, where the only feature boundaries available are the
 *     preliminary ones from trace picking. It is also the only place that may <em>create</em> spectra: merging copies
 *     the MS2 references into the projected traces, so anything registered later is never read.</li>
 *     <li>{@link #refine} runs after merging and before features are imported, where the final feature boundaries
 *     exist and every sample can be judged at once, but only the <em>peaks</em> of existing spectra can change.</li>
 * </ul>
 */
public interface DiaAssignmentStrategy {

    /** a DIA sample together with the acquisition scheme reconstructed for it */
    record SampleIndex(ProcessedSample sample, DiaScanIndex index) {
    }

    /**
     * Assigns the fragments of one sample and registers the spectra on it. Called before the masses of interest are
     * extracted, so that a precursor whose deconvolution found co-eluting fragments is treated as confident.
     *
     * @param tracker follows a peak of interest through the stages, see {@link Tracker#tracksDia()}
     * @return what was done, for inspection and for the log
     */
    DiaReport assign(ProcessedSample sample, DiaScanIndex index, Tracker tracker);

    /**
     * Revises the spectra of {@link #assign} now that the final feature boundaries are known and all samples are
     * available. May only replace the peaks of spectra that already exist.
     *
     * @param features      the final features, from {@link DiaFinalPrecursors}
     * @param massDeviation how far the same mass may differ between samples
     * @param rtTolerance   how far the same apex may differ between samples, in seconds
     * @return an account of what was done, ending in a newline, or null if the strategy does not use this phase
     */
    default String refine(List<SampleIndex> samples, List<DiaFinalPrecursors.Aligned> features,
                          Deviation massDeviation, double rtTolerance, Tracker tracker) {
        return null;
    }
}
