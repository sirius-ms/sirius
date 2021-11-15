package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractFragmentationTreeAnnotator {

    protected final FTree fTree;
    protected final MolecularGraph molecule;
    protected final CombinatorialFragmenterScoring scoring;
    protected HashMap<Fragment, ArrayList<CombinatorialFragment>> mapping;


    public AbstractFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        this.fTree = fTree;
        this.molecule = molecule;
        this.scoring = scoring;
    }

    public abstract HashMap<Fragment, ArrayList<CombinatorialFragment>> computeMapping() throws Exception;

    public FTree getFTree(){
        return this.fTree;
    }

    public MolecularGraph getMolecule(){
        return this.molecule;
    }

    public CombinatorialFragmenterScoring getScoring(){
        return this.scoring;
    }

    public HashMap<Fragment,ArrayList<CombinatorialFragment>> getMapping(){
        return this.mapping;
    }
}
