package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

/**
 * Created by ge28quv on 11/05/17.
 */
public class FragmentsCandidate extends StandardCandidate<FragmentsAndLosses>{
//    protected MolecularFormula formula;
//    protected PrecursorIonType ionType;

    public static FragmentsCandidate newInstance(FTree tree, Ms2Experiment experiment){

        FragmentsAndLosses fragmentsAndLosses = getFragments(tree, experiment);
        double score = (tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
        MolecularFormula formula = tree.getRoot().getFormula();
        PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

        FragmentsCandidate candidate = new FragmentsCandidate(fragmentsAndLosses, score, formula, ionType, experiment);

//        candidate.ionType = ionType;
//        candidate.formula = formula;
        candidate.addAnnotation(MolecularFormula.class, formula);
        candidate.addAnnotation(PrecursorIonType.class, ionType);
        candidate.addAnnotation(FTree.class, tree);
        return candidate;
    }

    public static List<FragmentsCandidate> createAllCandidateInstances(Collection<FTree> trees, Ms2Experiment experiment){

        Map<Peak, List<Fragment>> peakToFragments = new HashMap<>();


        for (FTree tree : trees) {
            final List<Fragment> fragments = tree.getFragments();

            final FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            for (Fragment fragment : fragments) {
                Peak peak = getPeak(fragment, annotation, experiment);
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

        assert peakToFragments.size()<=numberOfPeaks(experiment.getMs2Spectra());

        List<FragmentsCandidate> candidates = new ArrayList<>();
        for (FTree tree : trees) {
            FragmentsAndLosses fragmentsAndLosses = getFragments(tree, peakToIdx, experiment);
            double score = (tree.getAnnotationOrThrow(TreeScoring.class)).getOverallScore();
            MolecularFormula formula = tree.getRoot().getFormula();
            PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

            FragmentsCandidate candidate = new FragmentsCandidate(fragmentsAndLosses, score, formula, ionType, experiment);

//            candidate.ionType = ionType;
//            candidate.formula = formula;
            candidate.addAnnotation(MolecularFormula.class, formula);
            candidate.addAnnotation(PrecursorIonType.class, ionType);
            candidate.addAnnotation(FTree.class, tree);
            UnconsideredCandidatesUpperBound unconsideredCandidatesUpperBound = tree.getAnnotationOrNull(UnconsideredCandidatesUpperBound.class);
            if (unconsideredCandidatesUpperBound !=null) {
                candidate.addAnnotation(UnconsideredCandidatesUpperBound.class, unconsideredCandidatesUpperBound);
            }


            candidates.add(candidate);
        }



        return candidates;
    }

    private static int numberOfPeaks(List<? extends Spectrum<Peak>> spectra){
        int sum = 0;
        for (Spectrum<Peak> spectrum : spectra) {
            sum += spectrum.size();
        }
        return sum;
    }

    private static Peak getPeak(Fragment fragment, FragmentAnnotation<AnnotatedPeak> annotation, Ms2Experiment experiment){
        AnnotatedPeak annotatedPeak = annotation.get(fragment);
        Peak peak;
        if (annotatedPeak!=null){
            peak = getPeak(annotation.get(fragment));
        } else {
            if (fragment.isRoot() && !experiment.getPrecursorIonType().getInSourceFragmentation().isEmpty() && fragment.getChildren().size()==1){
                //if tree is resolved and ionization has in-source loss (e.g. H2O) use this mz as a proxy for the compounds peak.
                double mzWithInsource = experiment.getPrecursorIonType().addIonAndAdduct(experiment.getIonMass()-experiment.getPrecursorIonType().getModificationMass());
                peak = new Peak(mzWithInsource, 0d);
            } else {
                throw new RuntimeException("no peak annotation found");
            }
        }
        return peak;
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

//            return new Peak(meanMass, meanIntensity);
            //changed
            return new Peak(meanMass, annotatedPeak.getRelativeIntensity());
        } else {
            return new Peak(annotatedPeak.getMass(), annotatedPeak.getSumedIntensity());
        }
    }

    private static FragmentsAndLosses getFragments(FTree tree, TObjectIntMap<Peak> peakToIdx, Ms2Experiment experiment) {
        List<Fragment> fragments = tree.getFragments();

        MolecularFormula root = tree.getRoot().getFormula();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];
        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

        LossAnnotation<Score> lscore = tree.getOrCreateLossAnnotation(Score.class);
        FragmentAnnotation<Score> fscore = tree.getOrCreateFragmentAnnotation(Score.class);


        double maxIntensity = 0;
        for (Peak peak : peakToIdx.keySet()) {
            maxIntensity = Math.max(maxIntensity, peak.getIntensity());
        }

        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
                final Peak peak = getPeak(f, annotation, experiment);
                final int idx = peakToIdx.get(peak);
                if (idx<0){
                    System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                    throw new RuntimeException("index < 0");
                }
                else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
                final Score fs = fscore.get(f);
                final Score ls = f.getInDegree()==0?null:lscore.get(f.getIncomingEdge());
                final double score = (fs==null?0d:fs.sum())+(ls==null?0d:ls.sum());
                //changed
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), f.getIonization(), (short)idx, peak.getIntensity()/maxIntensity);
//                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), f.getIonization(), (short)idx, score);

            }
        }

        i = 0;
        for (Fragment f : fragments) {
            final Peak peak = getPeak(f, annotation, experiment);
            final int idx = peakToIdx.get(peak);
            if (idx<0){
                System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                throw new RuntimeException("index < 0");
            }
            else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
            //todo is root??
            final Score fs = fscore.get(f);
            final Score ls = f.getInDegree()==0?null:lscore.get(f.getIncomingEdge());
            final double score = (fs==null?0d:fs.sum())+(ls==null?0d:ls.sum());
            //changed
            if (f.getFormula().equals(root)){
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), f.getIonization(), (short)idx, 1d);
            } else {
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), f.getIonization(), (short)idx, peak.getIntensity()/maxIntensity);
            }

//            fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), f.getIonization(), (short)idx, score);

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

        return new FragmentsAndLosses(fragWithIdx, lossWithIdx);
    }


    protected FragmentsCandidate(FragmentsAndLosses fragmentsAndLosses, double score, MolecularFormula formula, PrecursorIonType ionType, Ms2Experiment experiment) {
        super(fragmentsAndLosses, score, formula, ionType, experiment);
    }


    private static FragmentsAndLosses getFragments(FTree tree, Ms2Experiment experiment) {
        MolecularFormula root = tree.getRoot().getFormula();
        List<Fragment> fragments = tree.getFragments();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];

        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        FragmentAnnotation<Peak> annoPeak = tree.getFragmentAnnotationOrThrow(Peak.class);

        SimpleMutableSpectrum sortedSpec = new SimpleMutableSpectrum(experiment.getMs2Spectra().get(0));



        PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

        //todo rather use all trees at once, this is a working hack

        Deviation deviation = new Deviation(10,0.001);


                //adding root mass if necessary
//        double precursorIon = experiment.getIonMass();
//        double theoPrecursorMass = ionType.addIonAndAdduct(root.getMass());
        double theoPrecursorMass = tree.getRoot().getIonization().addToMass(root.getMass());
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
//                    mass = ionType.addIonAndAdduct(f.getFormula().getMass());
                    mass = f.getIonization().addToMass(f.getFormula().getMass());
                } else {
                    mass = annotatedPeak.getOriginalPeaks()[0].getMass();
                }
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
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()).formatByHill(), f.getIonization(), (short)idx, f.getIncomingEdge().getWeight());

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
            fragWithIdx[i++] = new FragmentWithIndex(f.getFormula().formatByHill(), f.getIonization(), (short)idx, f.getIncomingEdge().getWeight());

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

        return new FragmentsAndLosses(fragWithIdx, lossWithIdx);
    }


    public FragmentWithIndex[] getFragments(){
        return getCandidate().getFragments();
    }

    public FragmentWithIndex[] getLosses(){
        return getCandidate().getLosses();
    }

}
