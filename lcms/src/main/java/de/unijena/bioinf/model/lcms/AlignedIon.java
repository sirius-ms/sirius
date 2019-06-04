package de.unijena.bioinf.model.lcms;

public class AlignedIon {

    protected final IonGroup left, right;
    protected final double score;

    public AlignedIon(IonGroup left, IonGroup right, double score) {
        this.left = left;
        this.right = right;
        this.score = score;
    }

    public IonGroup getLeft() {
        return left;
    }

    public IonGroup getRight() {
        return right;
    }

    public double getScore() {
        return score;
    }
}
