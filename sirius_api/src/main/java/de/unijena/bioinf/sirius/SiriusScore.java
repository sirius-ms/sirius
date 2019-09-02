package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTScore;

public class SiriusScore extends FTScore {
    public static SiriusScore NaN = new SiriusScore(Double.NaN);

    public SiriusScore(double score) {
        super(score);
    }

    @Override
    public boolean isLogarithmic() {
        return true;
    }

    @Override
    public String name() {
        return "siriusScore";
    }


}