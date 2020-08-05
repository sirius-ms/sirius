
package de.unijena.bioinf.ftalign.view;

/**
 * Created by kaidu on 08.08.14.
 */
public class Pair {

    private DataElement left, right;
    private double tanimoto;

    public Pair(DataElement left, DataElement right) {
        this.left = left;
        this.right = right;
        this.tanimoto = left.tanimoto(right);
    }

    public DataElement getLeft() {
        return left;
    }

    public void setLeft(DataElement left) {
        this.left = left;
        this.tanimoto = left.tanimoto(right);

    }

    public DataElement getRight() {
        return right;
    }

    public void setRight(DataElement right) {
        this.right = right;
        this.tanimoto = left.tanimoto(right);
    }

    public double getTanimoto() {
        return tanimoto;
    }

    @Override
    public String toString() {
        return left.getName() + " vs. " + right.getName();
    }
}
