package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.model.lcms.CorrelationGroup;
import de.unijena.bioinf.model.lcms.IonGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

class Edge {

    public Edge reverse() {
        final Edge e = new Edge(to,from,type,toType,fromType);
        e.cor = cor.invert();
        return e;
    }

    public String description() {
        if (fromType!=null && toType!=null) {

            HashSet<ProcessedSample> commonSamples = new HashSet<>(from.getFeature().getFeatures().keySet());
            commonSamples.retainAll(to.getFeature().getFeatures().keySet());

            return String.format(Locale.US, "%s -> %s (%d peaks, %d samples, r = %.2f, kb = %.2f)", fromType.toString(), toType.toString(), cor.getNumberOfCorrelatedPeaks(), commonSamples.size(), cor.getCorrelation(), cor.getKullbackLeibler());
        } else return "";
    }

    public int numberOfCommonSamples() {
        HashSet<ProcessedSample> commonSamples = new HashSet<>(from.getFeature().getFeatures().keySet());
        commonSamples.retainAll(to.getFeature().getFeatures().keySet());
        return commonSamples.size();
    }

    enum Type {
        ADDUCT, INSOURCE, CORRELATED;
    }

    IonNode from, to;
    Type type;

    CorrelationGroup cor;

    protected PrecursorIonType fromType, toType;

    public Edge(IonNode from, IonNode to, Type type) {
        this(from,to,type,null,null);
    }
    public Edge(IonNode from, IonNode to, Type type,PrecursorIonType fromType, PrecursorIonType toType) {
        this.from = from;
        this.to = to;
        this.fromType = fromType;
        this.toType = toType;
        this.type = type;
    }

    public double deltaMz() {
        return from.mz - to.mz;
    }

    protected IonGroup[] getMatchingIons2() {
        return new IonGroup[]{
                new IonGroup(cor.getLeft(),cor.getLeftSegment(), Collections.emptyList()),
                new IonGroup(cor.getRight(),cor.getRightSegment(), Collections.emptyList()),
        };
    }

    protected IonGroup[] getMatchingIons() {

        AlignedFeatures a = from.getFeature();
        AlignedFeatures b = to.getFeature();

        // find samples which contains both features
        double maxIntens = Double.NEGATIVE_INFINITY;
        ProcessedSample bestSample = null;
        for (ProcessedSample sa : a.getFeatures().keySet()) {
            for (ProcessedSample sb : b.getFeatures().keySet()) {
                if (sa==sb) {
                    final double i = a.getFeatures().get(sa).getIntensity() * b.getFeatures().get(sb).getIntensity();
                    if (i > maxIntens) {
                        bestSample = sa;
                        maxIntens = i;
                    }
                }
            }
        }
        if (bestSample==null) {
            System.err.println("Strange correlation without common sample");
            return new IonGroup[0];
        } else {
            final IonGroup xa = a.getFeatures().get(bestSample);
            final IonGroup xb = b.getFeatures().get(bestSample);
            return new IonGroup[]{xa,xb};
        }

    }

}
