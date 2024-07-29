package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class AbstractFragmentationTreeAnnotator {

    protected final FTree fTree;
    protected final MolecularGraph molecule;
    protected final CombinatorialFragmenterScoring scoring;
    protected CombinatorialGraph graph;

    protected final HashMap<Fragment, ArrayList<CombinatorialFragment>> mapping;


    public AbstractFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        this.fTree = fTree;
        this.molecule = molecule;
        this.scoring = scoring;
        this.mapping = new HashMap<>();
    }

    public AbstractFragmentationTreeAnnotator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        this(fTree, graph.root.fragment.parent, scoring);
        this.graph = graph;
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

    public CombinatorialGraph getCombinatorialGraph(){
        return this.graph;
    }
}
