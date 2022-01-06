package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class CombinatorialSubtreeCalculator extends AbstractFragmentationTreeAnnotator {

    protected double score;
    protected final CombinatorialSubtree subtree;

    public CombinatorialSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.score = Double.NEGATIVE_INFINITY;
        this.subtree = new CombinatorialSubtree(molecule);
    }

    public abstract CombinatorialSubtree computeSubtree() throws Exception;

    /*
     * Compute the mapping by using the calculated subtree.
     * Each node (Fragment object) in the FTree is assigned a list of CombinatorialFragments.
     * If there is a molecular fragment in the subtree with a molecular formula which is also contained in the FTree,
     * this fragment is added to the corresponding FTree node list.
     */
    // TODO: maybe remove hydrogen atoms from the molecular formulas
    @Override
    public HashMap<Fragment, ArrayList<CombinatorialFragment>> computeMapping(){
        // 1.) Compute a HashMap which maps each Molecular Formula the corresponding Fragment of the FTree.
        // 2.) At the same time, the mapping, which maps each Fragment to a List of CombinatorialFragments, can be initialized.
        HashMap<String, Fragment> mf2FtFrag = new HashMap<>();
        for(Fragment ftFrag : this.fTree){
            String mf = ftFrag.getFormula().toString();
            mf2FtFrag.put(mf, ftFrag);
            this.mapping.put(ftFrag, new ArrayList<CombinatorialFragment>());
        }

        // 3.) Compute the mapping:
        for(CombinatorialNode node : this.subtree){
            String fragmentMf = node.fragment.getFormula().toString();
            Fragment ftFrag = mf2FtFrag.get(fragmentMf);
            if(ftFrag != null){
                this.mapping.get(ftFrag).add(node.fragment);
            }
        }

        return this.mapping;
    }

    public CombinatorialSubtree getSubtree(){
        return this.subtree;
    }

    public double getScore(){
        return this.score;
    }


}
