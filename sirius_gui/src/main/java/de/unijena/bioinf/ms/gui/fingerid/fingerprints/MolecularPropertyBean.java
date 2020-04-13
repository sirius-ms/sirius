package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

/**
 * This class is immutable, but we have to extend Property support,
 * because the reactive glazed lists do need the property change methods,
 * to register itself even if they do nothing. Otherwise the event lists throw an error.
 */
public class MolecularPropertyBean<P extends MolecularProperty> implements SiriusPCS, Comparable<MolecularPropertyBean<P>> {
    protected final ProbabilityFingerprint underlyingFingerprint;
    protected final int absoluteIndex;
    protected final double fscore;
    protected final int numberOfTrainingExamples;

    public MolecularPropertyBean(ProbabilityFingerprint underlyingFingerprint, int absoluteIndex, double fscore, int numberOfTrainingExamples) {
        this.underlyingFingerprint = underlyingFingerprint;
        this.absoluteIndex = absoluteIndex;
        this.fscore = fscore;
        this.numberOfTrainingExamples = numberOfTrainingExamples;
    }

    public int getNumberOfTrainingExamples() {
        return numberOfTrainingExamples;
    }

    public int getAbsoluteIndex() {
        return absoluteIndex;
    }

    public double getProbability() {
        return underlyingFingerprint.getProbability(absoluteIndex);
    }

    public P getMolecularProperty() {
        return (P) underlyingFingerprint.getFingerprintVersion().getMolecularProperty(absoluteIndex);
    }

    public double getFScore() {
        return fscore;
    }


    @Override
    public int compareTo(MolecularPropertyBean<P> o) {
        return Double.compare(o.getProbability(), getProbability());
    }

    @Override
    public String toString() {
        return absoluteIndex + ": " + getMolecularProperty().toString();
    }

    protected final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this,true);
    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

}
