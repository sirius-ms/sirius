package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.deconvolution.DiaTraceCorrelation.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A channel samples a peak four to nine times, so a correlation of 0.9 over five points is not evidence of
 * anything - and how much evidence it is depends on how many points there were. Instead of guessing thresholds,
 * the null distribution is measured from decoys and a correlation is converted into a log p-value against it,
 * separately for few and for many points. Same idea as {@code AdductNetwork.PValueStats}.
 */
public class DiaCorrelationPValuesTest {

    private static DiaCorrelationPValues calibrated(double[] decoyCorrelations, int datapoints) {
        DiaCorrelationPValues p = new DiaCorrelationPValues();
        for (double c : decoyCorrelations) p.addDecoy(new Result(c, datapoints));
        p.calibrate();
        return p;
    }

    private static double[] spread(int n, double from, double to) {
        final double[] values = new double[n];
        for (int k = 0; k < n; ++k) values[k] = from + (to - from) * k / (double) (n - 1);
        return values;
    }

    @Test
    public void aCorrelationNoDecoyReachesIsHighlySignificant() {
        DiaCorrelationPValues p = calibrated(spread(200, -0.2, 0.6), 8);

        double good = p.logPvalue(new Result(0.99, 8));
        double mediocre = p.logPvalue(new Result(0.3, 8));

        assertTrue(good < mediocre, "0.99 must beat 0.3: " + good + " vs " + mediocre);
        assertTrue(good < -4, "a correlation no decoy reaches should be strongly significant, got " + good);
        assertTrue(mediocre > -1.5, "a correlation half the decoys reach is weak evidence, got " + mediocre);
    }

    @Test
    public void logPvaluesAreMonotoneInTheCorrelation() {
        DiaCorrelationPValues p = calibrated(spread(500, -0.5, 0.99), 8);

        double previous = 1d;
        for (double c : new double[]{0d, 0.3, 0.5, 0.7, 0.8, 0.9, 0.95, 0.99}) {
            double logP = p.logPvalue(new Result(c, 8));
            assertTrue(logP <= previous, "log p-value must not increase with correlation at " + c);
            previous = logP;
        }
    }

    /**
     * The whole point of the exercise: the same correlation means different things depending on how many points
     * it was computed from.
     */
    @Test
    public void theSameCorrelationIsJudgedByHowManyPointsItCameFrom() {
        DiaCorrelationPValues p = new DiaCorrelationPValues();
        // with few points, decoys reach high correlations easily
        for (double c : spread(200, 0.4, 0.999)) p.addDecoy(new Result(c, 4));
        // with many points they do not
        for (double c : spread(200, -0.3, 0.5)) p.addDecoy(new Result(c, 9));
        p.calibrate();

        double fewPoints = p.logPvalue(new Result(0.95, 4));
        double manyPoints = p.logPvalue(new Result(0.95, 9));

        assertTrue(manyPoints < fewPoints - 2,
                "0.95 over nine points must be much stronger evidence than over four: " + manyPoints + " vs " + fewPoints);
        assertTrue(fewPoints > -3, "over four points a tenth of the decoys reach it, got " + fewPoints);
        assertTrue(manyPoints < -4, "over nine points no decoy reaches it, got " + manyPoints);
    }

    @Test
    public void anUndefinedCorrelationIsNoEvidence() {
        DiaCorrelationPValues p = calibrated(spread(100, 0d, 0.9), 8);
        assertEquals(0d, p.logPvalue(Result.UNDEFINED), 1e-12);
        assertEquals(0d, p.logPvalue(new Result(Double.NaN, 8)), 1e-12);
    }

    @Test
    public void negativeCorrelationIsNoEvidence() {
        DiaCorrelationPValues p = calibrated(spread(200, -0.5, 0.9), 8);
        assertTrue(p.logPvalue(new Result(-0.8, 8)) > -0.5, "an anti-correlated fragment is not evidence");
    }

    @Test
    public void withoutDecoysNothingIsSignificant() {
        DiaCorrelationPValues p = new DiaCorrelationPValues();
        p.calibrate();
        assertEquals(0, p.numberOfDecoys());
        // with no null distribution we cannot claim significance for anything
        assertEquals(0d, p.logPvalue(new Result(0.99, 8)), 1e-12);
    }

    @Test
    public void logPvaluesAreNeverPositive() {
        DiaCorrelationPValues p = calibrated(spread(300, -0.9, 0.999), 8);
        for (double c : spread(50, -1d, 1d)) {
            assertTrue(p.logPvalue(new Result(c, 8)) <= 1e-12, "log of a probability cannot be positive, at " + c);
        }
    }

    @Test
    public void significanceIsBoundedByTheNumberOfDecoys() {
        DiaCorrelationPValues few = calibrated(spread(10, 0d, 0.5), 8);
        DiaCorrelationPValues many = calibrated(spread(1000, 0d, 0.5), 8);

        double withFew = few.logPvalue(new Result(0.999, 8));
        double withMany = many.logPvalue(new Result(0.999, 8));

        assertTrue(withMany < withFew, "more decoys allow a stronger claim: " + withMany + " vs " + withFew);
        assertTrue(withFew >= Math.log(1d / (10 + 1)) - 1e-9,
                "ten decoys cannot support a p-value below 1/11, got " + withFew);
    }

    @Test
    public void lookupBeforeCalibrationIsRejected() {
        DiaCorrelationPValues p = new DiaCorrelationPValues();
        p.addDecoy(new Result(0.5, 8));
        assertThrows(IllegalStateException.class, () -> p.logPvalue(new Result(0.9, 8)));
    }
}
