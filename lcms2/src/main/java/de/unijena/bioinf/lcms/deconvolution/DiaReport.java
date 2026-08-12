package de.unijena.bioinf.lcms.deconvolution;

import org.jetbrains.annotations.Nullable;

/**
 * What the DIA deconvolution of one sample did. Kept so that it can be inspected after the fact instead of only
 * appearing in a log: these are the numbers that say whether the cost and size estimates a design was built on hold
 * on real data, and they are what the comparison against a DDA measurement reports alongside its own.
 * <p>
 * Deliberately says nothing about <em>how</em> the assignment was made. There is more than one defensible way to
 * decide which fragment belongs to which precursor - see {@link DiaAssignmentStrategy} - and which one is better is a
 * question for measurement rather than for argument, so this carries only what every strategy can answer: how many
 * spectra were written, how many peaks they hold, how many precursors got none, and a free-form description for
 * whatever else a particular strategy wants to report.
 *
 * @param index            the acquisition scheme that was recovered
 * @param spectraWritten   pseudo spectra registered on the sample
 * @param peaksWritten     peaks in them
 * @param withoutSpectrum  precursor features that came out with no spectrum at all
 * @param detail           the strategy's own account of what it did, ending in a newline, or null
 */
public record DiaReport(DiaScanIndex index, int spectraWritten, int peaksWritten, int withoutSpectrum,
                        @Nullable String detail) {

    /** the report of a sample whose acquisition scheme was recovered but which could not be assigned at all */
    public static DiaReport nothingAssigned(DiaScanIndex index, @Nullable String detail) {
        return new DiaReport(index, 0, 0, 0, detail);
    }

    public String describe() {
        final StringBuilder buf = new StringBuilder(index.describe());
        if (detail != null) buf.append(detail);
        return buf.toString();
    }
}
