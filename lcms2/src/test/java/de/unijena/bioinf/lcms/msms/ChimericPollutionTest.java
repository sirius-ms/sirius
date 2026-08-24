package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What is in an isolation window besides the precursor, and whether a scan is still the targeted measurement of one
 * compound.
 * <p>
 * The second question is asked of the MS2 scans a DIA index did not claim: their window may be a wide one, whose
 * fragments belong to everything co-eluting in it, or a narrow one, which is an ordinary MS/MS measurement and has to
 * be assigned as such rather than dropped.
 */
public class ChimericPollutionTest {

    private static final double PRECURSOR = 400.2;

    private static Ms2SpectrumHeader header(double windowWidth) {
        return new Ms2SpectrumHeader(1, 2, "scan=2", 1, 2, true, new CollisionEnergy(35),
                new IsolationWindow(0d, windowWidth), 3, PRECURSOR, PRECURSOR, 100d);
    }

    private static SimpleSpectrum ms1(double[] mz, double[] intensities) {
        return new SimpleSpectrum(mz, intensities);
    }

    private static final SimpleSpectrum EMPTY = new SimpleSpectrum(new double[0], new double[0]);

    @Test
    public void everythingInTheWindowButThePrecursorIsPollution() {
        final SimpleSpectrum ms1 = ms1(new double[]{399.8, PRECURSOR}, new double[]{30d, 100d});
        final double pollution = ChimericPollution.of(header(2d), ms1, true).pollution();
        // weighted by distance from the window centre: 30 * exp(-0.4^2 / 2^2)
        assertEquals(30d * Math.exp(-0.16 / 4d), pollution, 1e-9);
    }

    @Test
    public void aPeakOutsideTheWindowIsNotPollution() {
        final SimpleSpectrum ms1 = ms1(new double[]{PRECURSOR, 405.0}, new double[]{100d, 500d});
        assertEquals(0d, ChimericPollution.of(header(2d), ms1, true).pollution(), 1e-9);
    }

    @Test
    public void theRatioIsThePollutionRelativeToThePrecursorsOwnIntensity() {
        final SimpleSpectrum ms1 = ms1(new double[]{PRECURSOR, 400.4}, new double[]{100d, 50d});
        final ChimericPollution.Result result = ChimericPollution.of(header(2d), ms1, true);
        assertEquals(100d, result.precursorIntensity(), 1e-9);
        assertEquals(result.pollution() / 100d, result.ratio(), 1e-12);
    }

    @Test
    public void aWideWindowIsNotATargetedMeasurement() {
        final SimpleSpectrum ms1 = ms1(new double[]{PRECURSOR}, new double[]{100d});
        assertFalse(ChimericPollution.isTargetedMeasurement(header(25d), ms1),
                "a SWATH window holds everything co-eluting in it, however clean the precursor looks");
    }

    @Test
    public void aNarrowCleanWindowIsATargetedMeasurement() {
        final SimpleSpectrum ms1 = ms1(new double[]{PRECURSOR, 405.0}, new double[]{100d, 900d});
        assertTrue(ChimericPollution.isTargetedMeasurement(header(2d), ms1));
    }

    @Test
    public void aNarrowWindowWithACoElutingIsobarIsNotATargetedMeasurement() {
        final SimpleSpectrum ms1 = ms1(new double[]{399.9, PRECURSOR}, new double[]{40d, 100d});
        assertFalse(ChimericPollution.isTargetedMeasurement(header(2d), ms1),
                "40 % of the precursor's intensity is another compound's fragments in the spectrum");
    }

    /**
     * A four dalton window contains the precursor's own M+1, and at 1.1 % per carbon that is 20 % of the precursor
     * for a compound with eighteen carbons - so counting it would reject clean scans of any ordinary metabolite.
     */
    @Test
    public void aPrecursorsOwnIsotopesAreNotContaminationOfItself() {
        final double m1 = PRECURSOR + 1.00335484, m2 = PRECURSOR + 2 * 1.00335484;
        final SimpleSpectrum ms1 = ms1(new double[]{PRECURSOR, m1, m2}, new double[]{100d, 27d, 4d});
        assertTrue(ChimericPollution.of(header(4d), ms1, true).ratio() > MAX_POLLUTION_OF_A_TARGETED_SCAN,
                "counting them, a twenty five carbon compound looks polluted");
        assertTrue(ChimericPollution.isTargetedMeasurement(header(4d), ms1),
                "not counting them, the same scan is the clean targeted measurement it is");
    }

    @Test
    public void aScanWhosePrecursorIsNotInTheMs1IsNotATargetedMeasurement() {
        final SimpleSpectrum ms1 = ms1(new double[]{300.0, 500.0}, new double[]{100d, 100d});
        assertFalse(ChimericPollution.isTargetedMeasurement(header(2d), ms1),
                "and it may not throw either");
    }

    @Test
    public void anEmptyMs1IsNotATargetedMeasurement() {
        assertFalse(ChimericPollution.isTargetedMeasurement(header(2d), EMPTY));
    }

    private static final double MAX_POLLUTION_OF_A_TARGETED_SCAN = ChimericPollution.MAX_TARGETED_POLLUTION;
}
