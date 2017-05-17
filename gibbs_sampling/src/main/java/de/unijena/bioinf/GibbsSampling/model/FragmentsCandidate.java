package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ge28quv on 11/05/17.
 */
public class FragmentsCandidate extends StandardCandidate<FragmentsAndLosses>{
    protected MolecularFormula formula;
    protected PrecursorIonType ionType;

    public static FragmentsCandidate newInstance(FTree tree, Ms2Experiment experiment){

        FragmentsAndLosses fragmentsAndLosses = getFragments(tree, experiment);
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


    private static FragmentsAndLosses getFragments(FTree tree, Ms2Experiment experiment) {
        MolecularFormula root = tree.getRoot().getFormula();
        List<Fragment> fragments = tree.getFragments();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];
//        short[] fIdx = new short[fragments.size()];
//        short[] lIdx = new short[fragments.size()-1];

        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

        SimpleMutableSpectrum sortedSpec = new SimpleMutableSpectrum(experiment.getMs2Spectra().get(0));



        Deviation deviation = new Deviation(100);


                //adding root mass if necessary
        double precursorIon = experiment.getIonMass();
        boolean hasPrecursor = false;
        for (Peak peak : sortedSpec) {
            if (deviation.inErrorWindow(precursorIon, peak.getMass())){
                hasPrecursor = true;
                break;
            }
        }
        if (!hasPrecursor) sortedSpec.addPeak(precursorIon, -1);


        Spectrums.sortSpectrumByMass(sortedSpec);

        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
                final AnnotatedPeak annotatedPeak = annotation.get(f);
                final int idx = (Spectrums.binarySearch(sortedSpec, annotatedPeak.getMass(), deviation));
                if (idx<0) throw new RuntimeException("index < 0");
                else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), (short)idx);

//                lStrings[i] = root.subtract(f.getFormula()).formatByHill();
//                lIdx[i] = (short)f.getColor();
            }
        }

        i = 0;
        for (Fragment f : fragments) {
            final AnnotatedPeak annotatedPeak = annotation.get(f);
            final int idx = (Spectrums.binarySearch(sortedSpec, annotatedPeak.getMass(), deviation));
            if (idx<0) throw new RuntimeException("index < 0");
            else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
            fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), (short)idx);

//            fStrings[i++] = f.getFormula().formatByHill();
//            fIdx[i] = (short)f.getColor();
        }

        Arrays.sort(lossWithIdx);
        Arrays.sort(fragWithIdx);

        String[] lStrings = new String[lossWithIdx.length];
        short[] lIdx = new short[lossWithIdx.length];

        for (int j = 0; j < lossWithIdx.length; j++) {
            lStrings[j] = lossWithIdx[j].mf;
            lIdx[j] = lossWithIdx[j].idx;
        }

        String[] fStrings = new String[fragWithIdx.length];
        short[] fIdx = new short[fragWithIdx.length];

        for (int j = 0; j < fragWithIdx.length; j++) {
            fStrings[j] = fragWithIdx[j].mf;
            fIdx[j] = fragWithIdx[j].idx;
        }

        return new FragmentsAndLosses(fStrings, fIdx, lStrings, lIdx);
    }


//    private static FragmentsAndLosses getFragments(FTree tree) {
//        MolecularFormula root = tree.getRoot().getFormula();
//        List<Fragment> fragments = tree.getFragments();
//        String[] lStrings = new String[fragments.size() - 1];
//        String[] fStrings = new String[fragments.size()];
//        short[] fIdx = new short[fragments.size()];
//        short[] lIdx = new short[fragments.size()-1];
//        int i = 0;
//        for (Fragment f : fragments) {
//            if(!f.getFormula().equals(root)) {
//                lStrings[i] = root.subtract(f.getFormula()).formatByHill();
//                lIdx[i] = (short)f.getColor();
//            }
//        }
//
//        i = 0;
//        for (Fragment f : fragments) {
//            fStrings[i++] = f.getFormula().formatByHill();
//            fIdx[i] = (short)f.getColor();
//        }
//
//        Arrays.sort(lStrings);
//        Arrays.sort(fStrings);
//
//        return new FragmentsAndLosses(fStrings, lStrings);
//    }

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
