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
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

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


    public static List<FragmentsCandidate> createAllCandidateInstances(Collection<FTree> trees, Ms2Experiment experiment){

        Map<Peak, List<Fragment>> peakToFragments = new HashMap<>();


        for (FTree tree : trees) {
            final List<Fragment> fragments = tree.getFragments();
//            final FragmentAnnotation<Peak> annotation = tree.getFragmentAnnotationOrThrow(Peak.class);
            final FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            for (Fragment fragment : fragments) {
//                Peak peak = annotation.get(fragment);
//                List<Fragment> fragmentsOfPeak;
//                if (peakToFragments.containsKey(peak)){
//                    fragmentsOfPeak = peakToFragments.get(peak);
//                } else {
//                    fragmentsOfPeak = new ArrayList<>();
//                    peakToFragments.put(peak, fragmentsOfPeak);
//                }
//
//                fragmentsOfPeak.add(fragment);


                Peak peak = getPeak(annotation.get(fragment));
//                AnnotatedPeak annotatedPeak = annotation.get(fragment);
//                Peak peak;
//                if (annotatedPeak.getOriginalPeaks().length>0){
//                    double meanMass = 0d;
//                    double meanIntensity = 0d;
//                    for (Peak p : annotatedPeak.getOriginalPeaks()) {
//                        meanMass += p.getMass();
//                        meanIntensity += p.getIntensity();
//                    }
//                    meanMass /= annotatedPeak.getOriginalPeaks().length;
//                    meanIntensity /= annotatedPeak.getOriginalPeaks().length;
//
//                    peak = new Peak(meanMass, meanIntensity);
//                } else {
//                    peak = new Peak(annotatedPeak.getMass(), annotatedPeak.getSumedIntensity());
//                }

                List<Fragment> fragmentsOfPeak;
                if (peakToFragments.containsKey(peak)){
                    fragmentsOfPeak = peakToFragments.get(peak);
                } else {
                    fragmentsOfPeak = new ArrayList<>();
                    peakToFragments.put(peak, fragmentsOfPeak);
                }

                fragmentsOfPeak.add(fragment);

            }
        }




        List<Peak> peaks = new ArrayList<>(peakToFragments.keySet());
        Collections.sort(peaks);
        TObjectIntMap<Peak> peakToIdx = new TObjectIntHashMap<>(peaks.size(), 0.75f, -1);
        int i = 0;
        for (Peak peak : peaks) {
            peakToIdx.put(peak, i++);
        }


        System.out.println("annotatedPeaks "+peakToFragments.size()+" vs spectrum "+experiment.getMs2Spectra().get(0).size());
        if (peakToFragments.size()>experiment.getMs2Spectra().get(0).size())
        {
            throw new RuntimeException("too many annotated peaks");
        }

//        FragmentsCandidate[] candidates = new FragmentsCandidate[trees.size()];
        List<FragmentsCandidate> candidates = new ArrayList<>();
//        i = 0;
        for (FTree tree : trees) {
            FragmentsAndLosses fragmentsAndLosses = getFragments(tree, peakToIdx);
            double score = (tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
            MolecularFormula formula = tree.getRoot().getFormula();
            PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

            FragmentsCandidate candidate = new FragmentsCandidate(fragmentsAndLosses, score, formula, ionType, experiment);

            candidate.ionType = ionType;
            candidate.formula = formula;
            candidate.addAnnotation(MolecularFormula.class, formula);
            candidate.addAnnotation(PrecursorIonType.class, ionType);

//            candidates[i++] = candidate;
            candidates.add(candidate);
        }



        return candidates;
    }

    private static Peak getPeak(AnnotatedPeak annotatedPeak){
        if (annotatedPeak.getOriginalPeaks().length>0){
            double meanMass = 0d;
            double meanIntensity = 0d;
            for (Peak p : annotatedPeak.getOriginalPeaks()) {
                meanMass += p.getMass();
                meanIntensity += p.getIntensity();
            }
            meanMass /= annotatedPeak.getOriginalPeaks().length;
            meanIntensity /= annotatedPeak.getOriginalPeaks().length;

            return new Peak(meanMass, meanIntensity);
        } else {
            return new Peak(annotatedPeak.getMass(), annotatedPeak.getSumedIntensity());
        }
    }

    private static FragmentsAndLosses getFragments(FTree tree, TObjectIntMap<Peak> peakToIdx) {
        List<Fragment> fragments = tree.getFragments();

        MolecularFormula root = tree.getRoot().getFormula();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];
        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);


        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
//                final Peak peak = annotation.get(f);
                final Peak peak = getPeak(annotation.get(f));
                final int idx = peakToIdx.get(peak);
                if (idx<0){
//                    System.out.println(Arrays.toString(Spectrums.copyMasses(sortedSpec)));
                    System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                    throw new RuntimeException("index < 0");
                }
                else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), (short)idx);

            }
        }

        i = 0;
        for (Fragment f : fragments) {
//            final Peak peak = annotation.get(f);
            final Peak peak = getPeak(annotation.get(f));
            final int idx = peakToIdx.get(peak);
            if (idx<0){
//                System.out.println(Arrays.toString(Spectrums.copyMasses(sortedSpec)));

                System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                throw new RuntimeException("index < 0");
            }
            else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
            fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), (short)idx);

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
        FragmentAnnotation<Peak> annoPeak = tree.getFragmentAnnotationOrThrow(Peak.class);

        SimpleMutableSpectrum sortedSpec = new SimpleMutableSpectrum(experiment.getMs2Spectra().get(0));



        PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

        //todo rather use all trees at once, this is a working hack

        Deviation deviation = new Deviation(10,0.001);


                //adding root mass if necessary
//        double precursorIon = experiment.getIonMass();
        double theoPrecursorMass = ionType.addIonAndAdduct(root.getMass());
        boolean hasPrecursor = false;
        for (Peak peak : sortedSpec) {
            if (deviation.inErrorWindow(theoPrecursorMass, peak.getMass())){
                hasPrecursor = true;
                break;
            }
        }
        if (!hasPrecursor) sortedSpec.addPeak(theoPrecursorMass, -1);


        Spectrums.sortSpectrumByMass(sortedSpec);

        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
                final AnnotatedPeak annotatedPeak = annotation.get(f);
                double mass;
                if (annotatedPeak.getOriginalPeaks().length==0){
                    mass = ionType.addIonAndAdduct(f.getFormula().getMass());
                } else {
                    mass = annotatedPeak.getOriginalPeaks()[0].getMass();
                }
//                final Peak peak = annoPeak.get(f);
//                final int idx = (Spectrums.binarySearch(sortedSpec, peak.getMass(), deviation));
                final int idx = (Spectrums.binarySearch(sortedSpec, mass, deviation));
                //todo why so large deviations???
                if (idx<0){
                    System.out.println("name "+experiment.getName());
                    System.out.println(Arrays.toString(Spectrums.copyMasses(sortedSpec)));
//                    System.out.println("anno mass "+peak.getMass());
                    System.out.println("anno mass "+mass);
                    System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                    throw new RuntimeException("index < 0");
                }
                else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), (short)idx);

//                lStrings[i] = root.subtract(f.getFormula()).formatByHill();
//                lIdx[i] = (short)f.getColor();
            }
        }

        i = 0;
        for (Fragment f : fragments) {
//            final Peak annotatedPeak = annoPeak.get(f);
            final AnnotatedPeak annotatedPeak = annotation.get(f);
            double mass;
            if (annotatedPeak.getOriginalPeaks().length==0){
                mass = ionType.addIonAndAdduct(f.getFormula().getMass());
            } else {
                mass = annotatedPeak.getOriginalPeaks()[0].getMass();
            }
            final int idx = (Spectrums.binarySearch(sortedSpec, mass, deviation));
//            if (idx<0) throw new RuntimeException("index < 0");
            if (idx<0){
                System.out.println("nameX "+experiment.getName());
                System.out.println("original "+annotatedPeak.getOriginalPeaks().length);
                System.out.println(Arrays.toString(Spectrums.copyMasses(sortedSpec)));
//                    System.out.println("anno mass "+peak.getMass());
                System.out.println("anno mass "+mass);
                System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                throw new RuntimeException("index < 0");
            }
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
