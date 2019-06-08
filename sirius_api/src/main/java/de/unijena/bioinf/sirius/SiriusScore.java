package de.unijena.bioinf.sirius;

public class SiriusScore extends ResultScore {
    public static SiriusScore NaN = new SiriusScore(Double.NaN);

    public SiriusScore(double score) {
        super(score);
    }

    @Override
    public String name() {
        return "siriusScore";
    }


}