package de.unijena.bioinf.lcms.trace;

import java.io.Serializable;
import java.util.Arrays;

public class TraceNode implements Serializable {

    protected int uid;
    protected int[] isotopeTraces;
    protected int[] adductTraces;
    protected float confidenceScore;

    protected int[] apexes;

    protected static byte INIT=0, APEXES=1,ISOTOPES=2,ADDUCTS=3;

    protected byte adductSearched = 0;

    public TraceNode(int uid, float confidenceScore) {
        this.uid = uid;
        this.isotopeTraces = new int[0];
        this.adductTraces = new int[0];
        this.confidenceScore = confidenceScore;
    }

    public int[] getApexes() {
        return apexes;
    }

    public void setApexes(int[] apexes) {
        this.apexes = apexes;
    }

    public void addIsotope(ContiguousTrace trace) {
        this.isotopeTraces = Arrays.copyOf(isotopeTraces, isotopeTraces.length+1);
        isotopeTraces[isotopeTraces.length-1] = trace.uniqueId();
    }
    public void addAdduct(ContiguousTrace trace) {
        this.adductTraces = Arrays.copyOf(adductTraces, adductTraces.length+1);
        adductTraces[adductTraces.length-1] = trace.uniqueId();
    }

    public double getConfidence() {
        return confidenceScore;
    }
}
