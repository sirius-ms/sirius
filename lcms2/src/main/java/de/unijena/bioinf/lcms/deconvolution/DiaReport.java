package de.unijena.bioinf.lcms.deconvolution;

import org.jetbrains.annotations.Nullable;

/**
 * What the DIA deconvolution of one sample did. Kept so that it can be inspected after the fact instead of only
 * appearing in a log: these are the numbers that say whether the cost and size estimates the design was built on
 * hold on real data.
 *
 * @param index      the acquisition scheme that was recovered
 * @param assignment the fragment assignment, or null if the sample could not be calibrated
 * @param spectra    the spectra that were written, or null if none were
 */
public record DiaReport(DiaScanIndex index, @Nullable DiaDeconvolution.Result assignment,
                        @Nullable DiaPseudoSpectra.Result spectra) {

    public String describe() {
        final StringBuilder buf = new StringBuilder(index.describe());
        if (assignment != null) buf.append(assignment.describe());
        if (spectra != null) buf.append(spectra.describe()).append('\n');
        return buf.toString();
    }
}
