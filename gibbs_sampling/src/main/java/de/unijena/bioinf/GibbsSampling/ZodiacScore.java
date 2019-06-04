package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.sirius.ResultAnnotation;

public class ZodiacScore implements ResultAnnotation  {

    public final double score;

    public ZodiacScore(double score) {
        this.score = score;
    }
}
