package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTLoss;

/**
 * @author Kai Dührkop
 */
public class Loss implements FTLoss<Fragment> {

    private final Fragment head, tail;
    private final MolecularFormula loss;
    private double score;

    Loss(Fragment head, Fragment tail) {
        this(head, tail, head.getDecomposition().getFormula().subtract(tail.getDecomposition().getFormula()), 0d);
    }

    Loss(Fragment head, Fragment tail, MolecularFormula loss, double score) {
        this.head = head;
        this.tail = tail;
        this.loss = loss;
        this.score = score;
    }

    @Override
    public boolean equals(Object l) {
        if (this == l) return true;
        if (l == null) return false;
        if (!(l instanceof Loss)) return false;
        final Loss ls = (Loss)l;
        return head.equals(ls.head) && tail.equals(ls.tail);
    }

    @Override
    public int hashCode() {
        return head.hashCode() ^ tail.hashCode();
    }

    public MolecularFormula getLoss() {
        return loss;
    }

    public double getWeight() {
        return score;
    }

    public void setWeight(double weight) {
        this.score = weight;
    }

    public Fragment getHead() {
        return head;
    }

    public Fragment getTail() {
        return tail;
    }

    @Override
    public MolecularFormula getFormula() {
        return loss;
    }

    public String toString() {
        return head + " -> " + tail + " [" + getLoss() + ":" + score + "]";
    }

}
