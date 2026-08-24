package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;

/**
 * How much of what an isolation window let through was not the precursor.
 * <p>
 * The definition lives here rather than inline in {@link MsMsQuerySpectrum} because two different decisions rest on
 * it: the merge score, which prefers the cleanest measurement of a precursor among the samples, and whether an MS2
 * scan is a targeted measurement at all - see {@link #isTargetedMeasurement}. Two definitions of the same quantity
 * would drift.
 * <p>
 * Peaks are weighted by their distance from the centre of the window, since an ion near the edge is transmitted less
 * than one in the middle. The centre is the mass the instrument was asked to isolate, which for a wide window is
 * nowhere near the precursor that was found in it.
 */
public final class ChimericPollution {

    /**
     * The widest isolation window whose scan can still be one compound's MS/MS.
     * <p>
     * Above this a window is a data independent one: it holds whatever co-eluted in it, so its fragments cannot be
     * attributed to the precursor by the fact of having been isolated with it. Ordinary MS/MS windows are a dalton or
     * two wide, and four leaves room for the wider quadrupole settings without reaching the ten to twenty five
     * daltons a SWATH scheme uses.
     */
    public static final double MAX_TARGETED_WINDOW = 4d;

    /**
     * How much pollution a targeted measurement may carry, as a fraction of the precursor's own intensity.
     * <p>
     * The same boundary {@code CheckMs2Quality} calls "barely other peaks within the isolation window" and still
     * grades GOOD, so the bar means the same thing to a reader here as it does in a quality report.
     */
    public static final double MAX_TARGETED_POLLUTION = 0.2d;

    /** the precursor is looked up at the mass the header claims, so the tolerance only has to cover calibration */
    private static final Deviation PRECURSOR_DEVIATION = new Deviation(10);

    private static final double CARBON_ISOTOPE_DELTA = 1.00335484;

    /** a four dalton window reaches M+3 at most, and one dalton of that is already outside a narrow one */
    private static final int MAX_OWN_ISOTOPES = 4;

    private static final Deviation ISOTOPE_DEVIATION = new Deviation(20);

    private ChimericPollution() {
    }

    /**
     * @param withinWindow      the MS1 peaks the isolation window covers
     * @param precursorIndex    the precursor's own peak, as an index into {@code withinWindow}
     * @param pollution         intensity in the window that is not the precursor, weighted by distance from its centre
     * @param precursorIntensity the precursor's MS1 intensity, which is what makes the pollution a ratio
     * @param exactParentMass   the mass of the peak actually found, not the one the header claimed
     */
    public record Result(SimpleSpectrum withinWindow, int precursorIndex, double pollution,
                         double precursorIntensity, double exactParentMass) {
        /** the pollution as a fraction of the precursor's own intensity; infinite if the precursor has none */
        public double ratio() {
            return precursorIntensity > 0 ? pollution / precursorIntensity : Double.POSITIVE_INFINITY;
        }
    }

    /**
     * @param countOwnIsotopes whether the precursor's own isotope peaks count as pollution. They are not
     *                         contamination of it, but the merge score has always counted them and its numbers are
     *                         calibrated that way, so this is the caller's choice rather than a fixed answer.
     * @return null if the window holds no peak that could be the precursor, in which case there is nothing to relate
     * a pollution to
     */
    public static Result of(Ms2SpectrumHeader header, SimpleSpectrum ms1Spectrum, boolean countOwnIsotopes) {
        final int precursor = Spectrums.mostIntensivePeakWithin(ms1Spectrum, header.getPrecursorMz(),
                PRECURSOR_DEVIATION);
        if (precursor < 0) return null;
        return from(header, ms1Spectrum, precursor, countOwnIsotopes);
    }

    /**
     * The same measurement for a precursor peak the caller has already identified - which {@link MsMsQuerySpectrum}
     * has, since it may have had to search the MS2 for it.
     */
    public static Result from(Ms2SpectrumHeader header, SimpleSpectrum ms1Spectrum, int precursorIndex,
                              boolean countOwnIsotopes) {
        final IsolationWindow window = header.getIsolationWindow().orElse(new IsolationWindow(0, 1d));
        final double left = header.getPrecursorMz() + window.getLeftOffset();
        final double right = header.getPrecursorMz() + window.getRightOffset();

        int from = precursorIndex - 1, to = precursorIndex + 1;
        while (from >= 0 && ms1Spectrum.getMzAt(from) >= left) --from;
        ++from;
        while (to < ms1Spectrum.size() && ms1Spectrum.getMzAt(to) <= right) ++to;
        --to;

        final SimpleSpectrum withinWindow = Spectrums.subspectrum(ms1Spectrum, from, to - from + 1);
        final double centre = header.getTargetedMz() + window.getWindowOffset();
        final double width = window.getWindowWidth();
        final double parentMass = ms1Spectrum.getMzAt(precursorIndex);

        double pollution = 0d;
        for (int k = 0; k < withinWindow.size(); ++k) {
            if (k == precursorIndex - from) continue;
            if (!countOwnIsotopes && isOwnIsotope(parentMass, withinWindow.getMzAt(k))) continue;
            pollution += withinWindow.getIntensityAt(k)
                    * Math.exp(-Math.pow(withinWindow.getMzAt(k) - centre, 2) / (width * width));
        }
        return new Result(withinWindow, precursorIndex - from, pollution,
                ms1Spectrum.getIntensityAt(precursorIndex), parentMass);
    }

    /**
     * Whether this scan is still the MS/MS of one compound, so that its fragments may be attributed to the precursor
     * the instrument aimed at.
     * <p>
     * Asked of the MS2 scans a DIA index did not claim. Dropping all of them loses ordinary measurements - it is what
     * cost a data dependent run with merely wide windows 85 % of its MS2 - and keeping all of them attaches a
     * neighbour's fragments wherever the window was wide. Both conditions are needed and they fail independently: a
     * three dalton window over a co-eluting isobar is as chimeric as a twenty five dalton one.
     * <p>
     * The precursor's own isotopes do not count against it here. They are inside any window from about three daltons
     * up, and at 1.1 % per carbon M+1 alone reaches a fifth of the precursor by eighteen carbons, so counting them
     * would reject clean scans of any ordinary metabolite for being made of themselves.
     */
    public static boolean isTargetedMeasurement(Ms2SpectrumHeader header, SimpleSpectrum ms1Spectrum) {
        if (ms1Spectrum == null) return false;
        final IsolationWindow window = header.getIsolationWindow().orElse(null);
        if (window == null || window.isUndefined() || window.getWindowWidth() > MAX_TARGETED_WINDOW) return false;
        final Result result = of(header, ms1Spectrum, false);
        // no peak where the precursor was said to be: whatever the window transmitted, we cannot say this scan
        // measured that compound
        return result != null && result.ratio() <= MAX_TARGETED_POLLUTION;
    }

    /** whether a peak in the window is one of the precursor's own carbon isotopes, assuming a single charge */
    private static boolean isOwnIsotope(double parentMass, double mz) {
        if (mz <= parentMass) return false;
        for (int k = 1; k <= MAX_OWN_ISOTOPES; ++k) {
            final double expected = parentMass + k * CARBON_ISOTOPE_DELTA;
            if (Math.abs(mz - expected) <= ISOTOPE_DEVIATION.absoluteFor(expected)) return true;
        }
        return false;
    }
}
