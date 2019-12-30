package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

public class RerootedTree {

    protected final FTree original;
    protected final FTree tree;
    protected final int numberOfRegrafts;

    public RerootedTree(FTree original, FTree tree, int numberOfRegrafts) {
        this.original = original;
        this.tree = tree;
        this.numberOfRegrafts = numberOfRegrafts;
    }
}
