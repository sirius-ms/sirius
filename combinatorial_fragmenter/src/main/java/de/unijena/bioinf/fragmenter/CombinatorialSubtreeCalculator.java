package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public abstract class CombinatorialSubtreeCalculator extends AbstractFragmentationTreeAnnotator {

    protected double score;
    protected final CombinatorialSubtree subtree;

    public CombinatorialSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.score = Double.NEGATIVE_INFINITY;
        this.subtree = new CombinatorialSubtree(molecule);
    }

    public CombinatorialSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        super(fTree, graph, scoring);
        this.score = Double.NEGATIVE_INFINITY;
        this.subtree = new CombinatorialSubtree(this.getMolecule());
    }

    public abstract CombinatorialSubtree computeSubtree() throws Exception;

    public abstract String getMethodName();

    /**
     * Returns a mapping which assigns each {@link Fragment} (or molecular formula) contained in the fragmentation tree
     * a {@link CombinatorialFragment} whose molecular formula differs only in the number the hydrogen atoms compared to
     * {@link Fragment#getFormula()},
     * and this {@link CombinatorialFragment} is contained in the computed {@link CombinatorialSubtree}.
     *
     * @return a mapping which assigns each {@link Fragment} in {@link FTree} a {@link CombinatorialFragment} contained in the computed subtree
     */
    @Override
    public HashMap<Fragment, ArrayList<CombinatorialFragment>> computeMapping(){
        // 1. Create a mapping which maps the molecular formula of each terminal node to the corresponding fragment which
        // is connected to it.
        HashMap<MolecularFormula, CombinatorialFragment> mf2Frag = new HashMap<>();
        BitSet bitset = new BitSet();
        bitset.set(this.getMolecule().natoms); //'bitset' represent the bitset of a terminal node with number 0

        for(int i = 0; i < this.fTree.numberOfVertices(); i++){ // #terminal nodes <= #vertices in fTree
            CombinatorialNode terminalNode = this.subtree.getNode(bitset);
            if(terminalNode != null){
                CombinatorialFragment fragment = terminalNode.incomingEdges.get(0).source.fragment;
                mf2Frag.put(terminalNode.fragment.getFormula(),fragment);
            }
            this.incrementBitSet(bitset);
        }

        // 2. Iterate through this.fTree and for each vertex (Fragment) get the corresponding CombinatorialFragment
        // by using 'mf2Frag' and put it into the hashmap.
        for(Fragment ftFrag : this.fTree){
            CombinatorialFragment fragment = mf2Frag.get(ftFrag.getFormula());
            if(fragment != null){
                ArrayList<CombinatorialFragment> fragList = new ArrayList<>();
                fragList.add(fragment);
                this.mapping.put(ftFrag, fragList);
            }
        }

        return this.mapping;
    }

    /**
     * This method returns a list which contains for each fragment-terminalNode assignment the number of
     * hydrogen rearrangements.
     *
     * @return list containing the amount of hydrogen rearrangement for each fragment-peak assignment
     */
    public List<Integer> getListWithAmountOfHydrogenRearrangements(){
        ArrayList<Integer> hydrogenRearrangements = new ArrayList<>();
        BitSet bitset = new BitSet();
        bitset.set(this.molecule.natoms); //at this moment, 'bitset' represents the bitset of the first added terminal node

        for(int i = 0; i < this.fTree.numberOfVertices(); i++){ //#terminalNodes <= #vertices in fTree
            CombinatorialNode terminalNode = this.subtree.getNode(bitset);
            if(terminalNode != null){
                CombinatorialNode assignedNode = terminalNode.getIncomingEdges().get(0).source;
                int hydrogenDiff = Math.abs(assignedNode.fragment.hydrogenRearrangements(terminalNode.fragment.getFormula()));
                hydrogenRearrangements.add(hydrogenDiff);
            }
            this.incrementBitSet(bitset);
        }

        return hydrogenRearrangements;
    }

    private void incrementBitSet(BitSet bitSet){
        int idx = 0;
        while(bitSet.get(idx)){
            bitSet.set(idx, false);
            idx++;
        }
        bitSet.set(idx);
    }

    public CombinatorialSubtree getSubtree(){
        return this.subtree;
    }

    public double getScore(){
        return this.score;
    }


}
