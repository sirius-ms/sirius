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

package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class CommonFragmentAndLossScorer implements EdgeScorer<FragmentsCandidate> {
    protected TObjectIntHashMap<Ms2Experiment> idxMap;
    protected BitSet[] maybeSimilar;

    protected double threshold;
    protected  double MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 2d; //changed: this is now the value which is just not good enough; 3;//changed from 3 / 5

    //do not recompute when preparing with new threshold
    protected TObjectDoubleHashMap<Ms2Experiment> normalizationMap;
    PeakWithExplanation[][] allFragmentPeaks;
    PeakWithExplanation[][] allLossPeaks;
    double[] norm;
    private double used_minimum_number_matched_peaks_losses = Double.NaN;

    public CommonFragmentAndLossScorer(double threshold) {
        this.threshold = threshold;
    }

    public void prepare(FragmentsCandidate[][] candidates) {
        prepare(candidates, MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES);
    }

    private void prepare(FragmentsCandidate[][] candidates, double minimum_number_matched_peaks_losses) {
        LoggerFactory.getLogger(CommonFragmentAndLossScorer.class).debug("prepare.");


        if (normalizationMap==null || (used_minimum_number_matched_peaks_losses != minimum_number_matched_peaks_losses)
                || !containsAllCompounds(normalizationMap, Arrays.stream(candidates).map(c->c[0].getExperiment()).toArray(s->new Ms2Experiment[s]))){
            //something changed, so recompute all.
            long start = System.currentTimeMillis();
            used_minimum_number_matched_peaks_losses = minimum_number_matched_peaks_losses;
            norm = this.normalization(candidates, minimum_number_matched_peaks_losses);
            this.normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);

            for(int i = 0; i < candidates.length; ++i) {
                Ms2Experiment experiment = candidates[i][0].getExperiment();
                this.normalizationMap.put(experiment, norm[i]);
            }

            this.idxMap = new TObjectIntHashMap(candidates.length);
            this.maybeSimilar = new BitSet[candidates.length];
            allFragmentPeaks = new PeakWithExplanation[candidates.length][];
            allLossPeaks = new PeakWithExplanation[candidates.length][];

            for(int i = 0; i < candidates.length; ++i) {
                Ms2Experiment experiment = candidates[i][0].getExperiment();
                FragmentsCandidate[] currentCandidates = candidates[i];

                PeakWithExplanation[] fragmentPeaks = getPeaksWithExplanations(currentCandidates, true);
                allFragmentPeaks[i] = fragmentPeaks;

                PeakWithExplanation[] lossPeaks = getPeaksWithExplanations(currentCandidates, false);
                allLossPeaks[i] = lossPeaks;

                this.idxMap.put(experiment, i);
                this.maybeSimilar[i] = new BitSet(i+1);

            }

            LoggerFactory.getLogger(CommonFragmentAndLossScorer.class).debug("prepare, computed maps in "+(System.currentTimeMillis()-start));
        }

        long start = System.currentTimeMillis();
        for(int i = 0; i < allFragmentPeaks.length; ++i) {
            for(int j = 0; j < i; ++j) {
                final double commonL = this.scoreCommons(allFragmentPeaks[i], allFragmentPeaks[j]);
                final double commonF = this.scoreCommons(allLossPeaks[i], allLossPeaks[j]);

                final double sumFLMinusMinCount = commonF+commonL-minimum_number_matched_peaks_losses;
                final double score =  ((sumFLMinusMinCount) / norm[i]) + ((sumFLMinusMinCount) / norm[j]);

                if((sumFLMinusMinCount>0) && (score >= this.threshold)){
                    this.maybeSimilar[i].set(j);

                }

            }
        }

        int sum = 0;
        for (BitSet bitSet : this.maybeSimilar) {
            sum += bitSet.cardinality();
        }

        LoggerFactory.getLogger(CommonFragmentAndLossScorer.class).debug("prepare, computed maybeSimilar in "+(System.currentTimeMillis()-start));

        if (GibbsMFCorrectionNetwork.DEBUG) LoggerFactory.getLogger(CommonFragmentAndLossScorer.class).debug("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum + " | threshold was "+threshold);
    }

    private boolean containsAllCompounds(TObjectDoubleHashMap<Ms2Experiment> normalizationMap, Ms2Experiment[] ms2Experiments) {
        for (Ms2Experiment ms2Experiment : ms2Experiments) {
            if (!normalizationMap.containsKey(ms2Experiment)) return false;
        }
        return true;
    }

//    private void prepareData(){
//
//    }



    /**
     *
     * @param currentCandidates
     * @param useFragments true: normal spectrum and fragments, false: inverted spectrum and losses
     * @return
     */
    protected PeakWithExplanation[] getPeaksWithExplanations(FragmentsCandidate[] currentCandidates, final boolean useFragments){
        Set<Ionization> ions = collectIons(currentCandidates);

        int maxIdx = -1;
        for (FragmentsCandidate currentCandidate : currentCandidates) {
            for (FragmentWithIndex scoredFragment : currentCandidate.getCandidate().getFragments()) {
                final short idx = scoredFragment.getIndex();
                if (idx>maxIdx) maxIdx = idx;
            }
        }

        TObjectIntHashMap<Ionization> ionToIdx = new TObjectIntHashMap<>(ions.size());
        int pos = 0;
        for (Ionization ion : ions) {
            ionToIdx.put(ion, pos++);
        }

        // idx to number of peaks
        maxIdx += 1;


        Set<MolecularFormula>[] matchedFragments;
        if (useFragments){
            matchedFragments = new Set[maxIdx*ions.size()];
        }  else {
            matchedFragments = new Set[maxIdx];
        }
        for(int j = 0; j < currentCandidates.length; ++j) {
            FragmentsCandidate c = currentCandidates[j];
            PrecursorIonType currentIon = c.getIonType();
            final FragmentWithIndex[] fragments;
            final short[] indices;
            if (useFragments){
                fragments = c.getFragments();
                for (int i = 0; i < fragments.length; i++) {
                    final MolecularFormula formula = fragments[i].getFormula();
                    final int idx = fragments[i].getIndex()+maxIdx*ionToIdx.get(fragments[i].getIonization());
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                }

            } else {
                fragments = c.getLosses();

                for (int i = 0; i < fragments.length; i++) {
                    final MolecularFormula formula = fragments[i].getFormula();
                    final short idx = fragments[i].getIndex();
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                }

            }

        }


        int numOfRealPeaks = 0;
        for (Set<MolecularFormula> matched : matchedFragments) {
            if (matched!=null) ++numOfRealPeaks;
        }

        PeakWithExplanation[] peaksWithExplanations = new PeakWithExplanation[numOfRealPeaks];

        pos = 0;
        for (int j = 0; j < matchedFragments.length; j++) {
            if (matchedFragments[j]!=null){
                final MolecularFormula[] mfArray = matchedFragments[j].toArray(new MolecularFormula[0]);
                final double mass = meanMass(mfArray);
                peaksWithExplanations[pos++] = new PeakWithExplanation(mfArray, mass, 1d);
            }
        }

        Arrays.sort(peaksWithExplanations);

        return peaksWithExplanations;
    }

    protected double meanMass(MolecularFormula[] formulas){
        FormulaFactory factory = FormulaFactory.getInstance();

        double sum = 0;
        for (MolecularFormula formula : formulas) {
            sum += formula.getMass();
        }
        return sum/formulas.length;
    }

    protected Set<Ionization> collectIons(FragmentsCandidate[] candidates) {
        HashSet<Ionization> ions = new HashSet();
        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate candidate = candidates[i];
            for (FragmentWithIndex fragmentWithIndex : candidate.getFragments()) {
                ions.add(fragmentWithIndex.getIonization());
            }

        }

        return ions;
    }

    public double score(FragmentsCandidate candidate1, FragmentsCandidate candidate2) {
        int i = this.idxMap.get(candidate1.getExperiment());
        int j = this.idxMap.get(candidate2.getExperiment());


        if(i > j) {
            if(!this.maybeSimilar[i].get(j)) {
                return 0.0D;
            }
        } else {
            if(!this.maybeSimilar[j].get(i)) {
                return 0.0D;
            }
        }

        final double commonF = this.scoreCommons(candidate1.getFragments(), candidate2.getFragments());
        final double commonL = this.scoreCommons(candidate1.getLosses(), candidate2.getLosses());

        final double sumFLMinusMinCount = commonF+commonL-MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES;
        if (sumFLMinusMinCount<=0) return 0;

        final double norm1 = this.normalizationMap.get(candidate1.getExperiment());
        final double norm2 = this.normalizationMap.get(candidate2.getExperiment());
        final double score =  ((sumFLMinusMinCount) / norm1) + ((sumFLMinusMinCount) / norm2);

        return score;
    }

    @Override
    public double scoreWithoutThreshold(FragmentsCandidate candidate1, FragmentsCandidate candidate2) {
        final double commonF = this.scoreCommons(candidate1.getFragments(), candidate2.getFragments());
        final double commonL = this.scoreCommons(candidate1.getLosses(), candidate2.getLosses());
        final double norm1 = this.normalizationMap.get(candidate1.getExperiment());
        final double norm2 = this.normalizationMap.get(candidate2.getExperiment());

        final double sumFLMinusMinCount = commonF+commonL-MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES;
        if (sumFLMinusMinCount<=0) return 0;
        final double score =  ((sumFLMinusMinCount) / norm1) + ((sumFLMinusMinCount) / norm2);
        return score;
    }

    @Override
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    public void clean() {
        this.idxMap.clear();
        this.idxMap = null;
        this.maybeSimilar = null;
    }

    public double[] normalization(FragmentsCandidate[][] candidates, double minimum_number_matched_peaks_losses) {
        double[] norm = new double[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate[] compoundCandidates = candidates[i];
            int biggestTreeSize = -1;

            for (FragmentsCandidate compoundCandidate : compoundCandidates) {
                biggestTreeSize = Math.max(biggestTreeSize, compoundCandidate.getFragments().length);
            }

            norm[i] = (double)(2 * biggestTreeSize - 1)-minimum_number_matched_peaks_losses;
        }

        return norm;
    }

    @Override
    public BasicJJob<Object> getPrepareJob(FragmentsCandidate[][] candidates) {
        final double minimum_number_matched_peaks_losses = MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES;
        return new BasicMasterJJob<Object>(JJob.JobType.CPU) {
            @Override
            protected Object compute() throws Exception {
                if (normalizationMap==null || (used_minimum_number_matched_peaks_losses != minimum_number_matched_peaks_losses)
                        || !containsAllCompounds(normalizationMap, Arrays.stream(candidates).map(c->c[0].getExperiment()).toArray(s->new Ms2Experiment[s]))){
                    //something changed, so recompute all.
                    long start = System.currentTimeMillis();
                    used_minimum_number_matched_peaks_losses = minimum_number_matched_peaks_losses;
                    norm = normalization(candidates, minimum_number_matched_peaks_losses);
                    normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);

                    for(int i = 0; i < candidates.length; ++i) {
                        Ms2Experiment experiment = candidates[i][0].getExperiment();
                        normalizationMap.put(experiment, norm[i]);
                    }

                    idxMap = new TObjectIntHashMap(candidates.length);
                    maybeSimilar = new BitSet[candidates.length];
                    allFragmentPeaks = new PeakWithExplanation[candidates.length][];
                    allLossPeaks = new PeakWithExplanation[candidates.length][];

                    System.out.println("STEP 1");

                    submitSubJob(new BasicMasterJJob<Object>(JobType.CPU) {
                        @Override
                        protected Object compute() throws Exception {
                            for(int i = 0; i < candidates.length; ++i) {
                                final int I = i;
                                submitSubJob(
                                        new BasicJJob<Object>() {
                                            @Override
                                            protected Object compute() throws Exception {
                                                Ms2Experiment experiment = candidates[I][0].getExperiment();
                                                FragmentsCandidate[] currentCandidates = candidates[I];

                                                PeakWithExplanation[] fragmentPeaks = getPeaksWithExplanations(currentCandidates, true);
                                                allFragmentPeaks[I] = fragmentPeaks;

                                                PeakWithExplanation[] lossPeaks = getPeaksWithExplanations(currentCandidates, false);
                                                allLossPeaks[I] = lossPeaks;

                                                idxMap.put(experiment, I);
                                                maybeSimilar[I] = new BitSet(I+1);
                                                return "";
                                            }
                                        }
                                );
                            }
                            awaitAllSubJobs();
                            return "";
                        }
                    }).takeResult();

                    System.out.println("STEP 2");

                }

                System.out.println("STEP 3");
                submitSubJob(MatrixUtils.parallelizeSymmetricMatrixComputation(allFragmentPeaks.length, (i,j)->{
                    if(i!=j) {
                        final double commonL = scoreCommons(allFragmentPeaks[i], allFragmentPeaks[j]);
                        final double commonF = scoreCommons(allLossPeaks[i], allLossPeaks[j]);

                        final double extraPenaltyForLargeTrees = (int)Math.min(allFragmentPeaks[i].length*0.1, allFragmentPeaks[j].length*0.1);

                        final double sumFLMinusMinCount = commonF + commonL - minimum_number_matched_peaks_losses;
                        final double score = ((sumFLMinusMinCount) / norm[i]) + ((sumFLMinusMinCount) / norm[j]);

//                        if ((sumFLMinusMinCount > 0) && (score >= threshold)  &&   (commonF+commonL > extraPenaltyForLargeTrees)) {
                        //todo do not penalize large trees for now
                        if ((sumFLMinusMinCount > 0) && (score >= threshold)) {
                            maybeSimilar[i].set(j);
                        }
                    }
                })).takeResult();

                System.out.println("STEP 4");

                {
                    int total=0;
                    for (BitSet b : maybeSimilar)
                        total += b.cardinality();
                    final int N = ((candidates.length*candidates.length)-candidates.length)/2;
                    System.out.printf("heuristic: %d / %d ( %.3f %%)\n", total, N, total*100d/(N));
                }


                return "";
            }
        };
    }

    protected double scoreCommons(PeakWithExplanation[] peaks1, PeakWithExplanation[] peaks2){
        if (peaks1.length==0 || peaks2.length==0) return 0d;
        double commonScore = 0;
        int i = 0;
        int j = 0;
        double mz1 = peaks1[0].mass;
        double mz2 = peaks2[0].mass;

        while(i < peaks1.length && j < peaks2.length) {
            boolean match = hasMatch(peaks1[i].formulas, peaks2[j].formulas);
            int compare = Double.compare(mz1, mz2);
            if(match) {
                commonScore += scoreMatchedPeaks(peaks1[i], peaks2[j]);
                ++i;
                ++j;
                if(i >= peaks1.length || j >= peaks2.length) {
                    break;
                }

                mz1 = peaks1[i].mass;
                mz2 = peaks2[j].mass;
            } else if(compare < 0) {
                ++i;
                if(i >= peaks1.length) {
                    break;
                }

                mz1 = peaks1[i].mass;
            } else {
                ++j;
                if(j >= peaks2.length) {
                    break;
                }

                mz2 = peaks2[j].mass;
            }
        }

        return commonScore;
    }

    protected double scoreMatchedPeaks(PeakWithExplanation peak1, PeakWithExplanation peak2){
        return 1;
    }

    private boolean hasMatch(MolecularFormula[] fragments1, MolecularFormula[] fragments2){
        int i = 0;
        int j = 0;
        while(i < fragments1.length && j < fragments2.length) {
            int compare = fragments1[i].compareTo(fragments2[j]);
            if(compare < 0) {
                ++i;
            } else if(compare > 0) {
                ++j;
            } else {
                return true;
            }
        }
        return false;
    }


    protected double scoreCommons(FragmentWithIndex[] fragments1, FragmentWithIndex[] fragments2) {
        double commonCounter = 0;
        int i = 0;
        int j = 0;

        while(i < fragments1.length && j < fragments2.length) {
            int compare = fragments1[i].compareTo(fragments2[j]);
            if(compare < 0) {
                ++i;
                //todo don't change but leave as is? each fragment can be matched at most once. But if both have both fragments 2 times, you can get both matches
//                while (i+1 < fragments1.length && fragments1[i].getFormula().equals(fragments1[i+1].getFormula())) ++i;
            } else if(compare > 0) {
                ++j;
//                while (j+1 < fragments2.length && fragments2[j].getFormula().equals(fragments2[j+1].getFormula())) ++j;
            } else {
                final double matchScore = scoreMatchedFragments(fragments1[i], fragments2[j]);
                commonCounter += matchScore;
                ++i;
                ++j;
                //todo current hack
//                while (i+1 < fragments1.length && fragments1[i].getFormula().equals(fragments1[i+1].getFormula())) ++i;
//                while (j+1 < fragments2.length && fragments2[j].getFormula().equals(fragments2[j+1].getFormula())) ++j;
            }
        }

        return commonCounter;
    }

    protected double scoreMatchedFragments(FragmentWithIndex fragment1, FragmentWithIndex fragment2){
        return 1;
    }


    static class PeakWithExplanation implements Comparable<PeakWithExplanation>{
        MolecularFormula[] formulas;
        double mass;
        double bestScore;

        public PeakWithExplanation(MolecularFormula[] formulas, double mass, double bestScore) {
            this.formulas = formulas;
            Arrays.sort(this.formulas);
            this.mass = mass;
            this.bestScore = bestScore;
        }


        @Override
        public int compareTo(PeakWithExplanation o) {
            return Double.compare(mass, o.mass);
        }
    }
}
