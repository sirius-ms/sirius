package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.sirius.ResultScore;

public class ZodiacScore extends ResultScore {
    public static ZodiacScore NaN = new ZodiacScore(Double.NaN);

    public ZodiacScore(double score) {
        super(score);
    }

    @Override
    public String name() {
        return "zodiacScore";
    }
}
