package de.unijena.bioinf.GibbsSampling.model;

public class Connectivity {
    private final int numberOfConnectedCompounds;

    public Connectivity(int numberOfConnectedCompounds) {
        this.numberOfConnectedCompounds = numberOfConnectedCompounds;
    }

    public int getNumberOfConnectedCompounds() {
        return numberOfConnectedCompounds;
    }
}
