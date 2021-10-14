package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.HashMap;
import java.util.List;

public abstract class AbstractFragmentationTreeAnnotator {

    private final FTree fTree;
    private final MolecularGraph molecule;
    private final CombinatorialFragmenterScoring scoring;
    private HashMap<Fragment, List<CombinatorialNode>> mapping;


    public AbstractFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        this.fTree = fTree;
        this.molecule = molecule;
        this.scoring = scoring;
    }

    public abstract HashMap<Fragment, List<CombinatorialNode>> computeMapping();

    public FTree getFTree(){
        return this.fTree;
    }

    public MolecularGraph getMolecule(){
        return this.molecule;
    }

    public CombinatorialFragmenterScoring getScoring(){
        return this.scoring;
    }

    public HashMap<Fragment,List<CombinatorialNode>> getMapping(){
        return this.mapping;
    }
}
