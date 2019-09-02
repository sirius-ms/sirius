package de.unijena.bioinf.ChemistryBase.ms.ft;

public class ZodiacScore extends FTScore {
    public static ZodiacScore NaN = new ZodiacScore(Double.NaN);


    public ZodiacScore(double probability) {
        super(probability);
    }

    public double getProbability() {
        return score();
    }

    @Override
    public boolean isLogarithmic() {
        return false;
    }

    @Override
    public String name() {
        return "zodiacScore";
    }
}
