package de.unijena.bioinf.model.lcms;

public class CorrelatedIon {

    public final CorrelationGroup correlation;
    public final IonGroup ion;

    public CorrelatedIon(CorrelationGroup correlation, IonGroup ion) {
        this.correlation = correlation;
        this.ion = ion;
    }


}
