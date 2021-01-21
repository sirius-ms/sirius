/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

/**
 * Created by ge28quv on 11/05/17.
 */
public class FragmentsCandidate extends StandardCandidate<FragmentsAndLosses>{

    public static ProcessedInput assignFragmentsToPeaks(Ms2Experiment experiment, Collection<FTree> trees) {
        ProcessedInput input = new Sirius().preprocessForMs2Analysis(experiment);
        for (FTree tree : trees) {
            for (Fragment f : tree) f.setPeakId(-1);
            input.mapTreeToInput(tree);
            for (Fragment f : tree) {
                if (f.getPeakId()<0) {
                    // aritficially add this fragment to the spectrum...
                    AnnotatedPeak annotatedPeak = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class).get(f);
                    ProcessedPeak e = new ProcessedPeak();
                    e.setCollisionEnergy(CollisionEnergy.mergeAll(annotatedPeak.getCollisionEnergies()));
                    e.setIndex(input.getMergedPeaks().size());
                    e.setRelativeIntensity(annotatedPeak.getRelativeIntensity());
                    e.setMass(annotatedPeak.getMass());
                    f.setPeakId(input.getMergedPeaks().size());
                    input.getMergedPeaks().add(e);
                }
            }
        }

        // ensure ordering by mass...
        input.getMergedPeaks().sort((u,v)->Double.compare(u.getMass(),v.getMass()));
        final TIntIntHashMap reindex = new TIntIntHashMap();
        for (int k=0; k < input.getMergedPeaks().size(); ++k) {
            reindex.put(input.getMergedPeaks().get(k).getIndex(), k);
            input.getMergedPeaks().get(k).setIndex(k);
        }
        for (FTree tree : trees) {
            for (Fragment f : tree) {
                f.setPeakId(reindex.get(f.getPeakId()));
            }
        }

        return input;

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

        assert peakToFragments.size()<=numberOfPeaks(experiment.getMs2Spectra())+1;

        ProcessedInput input = assignFragmentsToPeaks(experiment, trees);

        List<FragmentsCandidate> candidates = new ArrayList<>();
        for (FTree tree : trees) {
            FragmentsAndLosses fragmentsAndLosses = getFragments(tree, peakToIdx, experiment, input.getMergedPeaks());
            double score = FTreeMetricsHelper.getSiriusScore(tree);
            MolecularFormula formula = tree.getRoot().getFormula();
            PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);

            FragmentsCandidate candidate = new FragmentsCandidate(fragmentsAndLosses, score, formula, ionType, experiment);

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


    private static Peak getPeak(Fragment fragment, FragmentAnnotation<AnnotatedPeak> annotation, Ms2Experiment experiment){
        AnnotatedPeak annotatedPeak = annotation.get(fragment);
        Peak peak;
        if (annotatedPeak!=null){
            peak = getPeak(annotation.get(fragment));
        } else {
            if (fragment.isRoot() && !experiment.getPrecursorIonType().getInSourceFragmentation().isEmpty() && fragment.getChildren().size()==1){
                //if tree is resolved and ionization has in-source loss (e.g. H2O) use this mz as a proxy for the compounds peak.
                double mzWithInsource = experiment.getPrecursorIonType().addIonAndAdduct(experiment.getIonMass()-experiment.getPrecursorIonType().getModificationMass());
                peak = new SimplePeak(mzWithInsource, 0d);
            } else {
                throw new RuntimeException("no peak annotation found");
            }
        }
        return peak;
    }

    private static Peak getPeak(AnnotatedPeak annotatedPeak){
        final double mass;
        if (annotatedPeak.getOriginalPeaks().length>0){
            double meanMass = 0d;
            for (Peak p : annotatedPeak.getOriginalPeaks()) {
                meanMass += p.getMass();
            }
            meanMass /= annotatedPeak.getOriginalPeaks().length;

            mass = meanMass;
        } else {
            mass = annotatedPeak.getMass();
        }

        double intensity = annotatedPeak.getRelativeIntensity();
        if (Double.isNaN(intensity)) intensity = 0d;

        return new SimplePeak(mass, intensity);
    }

    private static FragmentsAndLosses getFragments(FTree tree, TObjectIntMap<Peak> peakToIdx, Ms2Experiment experiment, List<ProcessedPeak> mergedSpectrum) {
        List<Fragment> fragments = tree.getFragments();

        MolecularFormula root = tree.getRoot().getFormula();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];
        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

        LossAnnotation<Score> lscore = tree.getOrCreateLossAnnotation(Score.class);
        FragmentAnnotation<Score> fscore = tree.getOrCreateFragmentAnnotation(Score.class);

        PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        Ionization ionization = ionType.getIonization();

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
//                final Score fs = fscore.get(f);
//                final Score ls = f.getInDegree()==0?null:lscore.get(f.getIncomingEdge());
//                final double score = (fs==null?0d:fs.sum())+(ls==null?0d:ls.sum());
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()), ionization, (short)idx, peak.getIntensity()/maxIntensity);


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
            if (f.getFormula().equals(root)){
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula(), ionization, (short)idx, 1d);
            } else {
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula(), ionization, (short)idx, peak.getIntensity()/maxIntensity);
            }
        }

        Arrays.sort(lossWithIdx);
        Arrays.sort(fragWithIdx);

        MolecularFormula[] lStrings = new MolecularFormula[lossWithIdx.length];
        short[] lIdx = new short[lossWithIdx.length];

        for (int j = 0; j < lossWithIdx.length; j++) {
            lStrings[j] = lossWithIdx[j].getFormula();
            lIdx[j] = lossWithIdx[j].idx;
        }

        MolecularFormula[] fStrings = new MolecularFormula[fragWithIdx.length];
        short[] fIdx = new short[fragWithIdx.length];

        for (int j = 0; j < fragWithIdx.length; j++) {
            fStrings[j] = fragWithIdx[j].getFormula();
            fIdx[j] = fragWithIdx[j].idx;
        }

        return new FragmentsAndLosses(fragWithIdx, lossWithIdx);
    }

    private static int numberOfPeaks(List<? extends Spectrum<Peak>> spectra){
        int sum = 0;
        for (Spectrum<Peak> spectrum : spectra) {
            sum += spectrum.size();
        }
        return sum;
    }

    private static FragmentsAndLosses getFragmentsWithPeakMapping(FTree tree, Ms2Experiment experiment, ProcessedInput mergedSpectrum) {
        List<Fragment> fragments = tree.getFragments();

        MolecularFormula root = tree.getRoot().getFormula();
        FragmentWithIndex[] lossWithIdx = new FragmentWithIndex[fragments.size() - 1];
        FragmentWithIndex[] fragWithIdx = new FragmentWithIndex[fragments.size()];
        FragmentAnnotation<AnnotatedPeak> annotation = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

        LossAnnotation<Score> lscore = tree.getOrCreateLossAnnotation(Score.class);
        FragmentAnnotation<Score> fscore = tree.getOrCreateFragmentAnnotation(Score.class);

        final double maxIntensity = mergedSpectrum.getMergedPeaks().stream().mapToDouble(x->x.getRelativeIntensity()).max().orElse(1d);

        int i = 0;
        for (Fragment f : fragments) {
            if(!f.getFormula().equals(root)) {
                final int idx = f.getPeakId();
                if (idx<0){
                    System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                    throw new RuntimeException("index < 0");
                }
                else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
                lossWithIdx[i++] = new FragmentWithIndex(root.subtract(f.getFormula()), f.getIonization(), (short)idx, mergedSpectrum.getMergedPeaks().get(f.getPeakId()).getRelativeIntensity()/maxIntensity);

            }
        }

        i = 0;
        for (Fragment f : fragments) {
            final int idx = f.getPeakId();
            if (idx<0){
                System.out.println("formula "+f.getFormula()+" "+f.getFormula().getMass());
                throw new RuntimeException("index < 0");
            }
            else if (idx>Short.MAX_VALUE) throw new RuntimeException("index too big");
            if (f.getFormula().equals(root)){
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula(), f.getIonization(), (short)idx, 1d);
            } else {
                fragWithIdx[i++] = new FragmentWithIndex(f.getFormula(), f.getIonization(), (short)idx, mergedSpectrum.getMergedPeaks().get(f.getPeakId()).getRelativeIntensity()/maxIntensity);
            }
        }

        Arrays.sort(lossWithIdx);
        Arrays.sort(fragWithIdx);

        MolecularFormula[] lStrings = new MolecularFormula[lossWithIdx.length];
        short[] lIdx = new short[lossWithIdx.length];

        for (int j = 0; j < lossWithIdx.length; j++) {
            lStrings[j] = lossWithIdx[j].mf;
            lIdx[j] = lossWithIdx[j].idx;
        }

        MolecularFormula[] fStrings = new MolecularFormula[fragWithIdx.length];
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

    public FragmentWithIndex[] getFragments(){
        return getCandidate().getFragments();
    }

    public FragmentWithIndex[] getLosses(){
        return getCandidate().getLosses();
    }

}
