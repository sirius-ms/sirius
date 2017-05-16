package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ge28quv on 11/05/17.
 */
public class FragmentsCandidate extends StandardCandidate<FragmentsAndLosses>{
    protected MolecularFormula formula;
    protected PrecursorIonType ionType;

    public static FragmentsCandidate newInstance(FTree tree, Ms2Experiment experiment){

        FragmentsAndLosses fragmentsAndLosses = getFragments(tree);
        double score = (tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
        MolecularFormula formula = tree.getRoot().getFormula();
        PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

        FragmentsCandidate candidate = new FragmentsCandidate(fragmentsAndLosses, score, formula, ionType, experiment);

        candidate.ionType = ionType;
        candidate.formula = formula;
        candidate.addAnnotation(MolecularFormula.class, formula);
        candidate.addAnnotation(PrecursorIonType.class, ionType);
        return candidate;
    }

    private FragmentsCandidate(FragmentsAndLosses fragmentsAndLosses, double score, MolecularFormula formula, PrecursorIonType ionType, Ms2Experiment experiment) {
        super(fragmentsAndLosses, score, formula, ionType, experiment);
    }

    private static FragmentsAndLosses getFragments(FTree tree) {
        MolecularFormula root = tree.getRoot().getFormula();
        List<Fragment> fragments = tree.getFragments();
        String[] lStrings = new String[fragments.size() - 1];
        String[] fStrings = new String[fragments.size()];

        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
                lStrings[i++] = root.subtract(f.getFormula()).formatByHill();
            }
        }

        i = 0;
        for (Fragment f : fragments) {
            fStrings[i++] = f.getFormula().formatByHill();
        }

        Arrays.sort(lStrings);
        Arrays.sort(fStrings);

        return new FragmentsAndLosses(fStrings, lStrings);
    }

//    private void setInfo(FTree tree){
//        this.formula = tree.getRoot().getFormula();
//        this.ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
////        this.score = ((TreeScoring)tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
//    }


    public String[] getFragments(){
        return getCandidate().getFragments();
    }

    public String[] getLosses(){
        return getCandidate().getLosses();
    }


}
