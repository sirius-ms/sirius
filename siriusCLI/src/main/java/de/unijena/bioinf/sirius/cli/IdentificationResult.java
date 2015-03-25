package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.util.List;

/**
 * Created by kaidu on 21.02.2015.
 */
public class IdentificationResult {

    List<FTree> optTrees;
    FTree optTree;

    public IdentificationResult(List<FTree> optTrees) {
        this.optTrees = optTrees;
        this.optTree = optTrees.isEmpty() ? null : optTrees.get(0);
    }
}
