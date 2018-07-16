package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.GibbsSampling.model.*;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

public class CommonFragmentAndLossScorer implements EdgeScorer<FragmentsCandidate> {
    protected TObjectIntHashMap<Ms2Experiment> idxMap;
    protected BitSet[] maybeSimilar;
    protected TObjectDoubleHashMap<Ms2Experiment> normalizationMap;
    protected double threshold;
    protected  double MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 3;//changed from 3 / 5
    public CommonFragmentAndLossScorer(double threshold) {
        this.threshold = threshold;
    }

    public void prepare(FragmentsCandidate[][] candidates) {
        prepare(candidates, MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES);
    }

    private void prepare(FragmentsCandidate[][] candidates, double minimum_numer_matched_peaks_losses) {
        double[] norm = this.normalization(candidates);
        this.normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Experiment experiment = candidates[i][0].getExperiment();
            this.normalizationMap.put(experiment, norm[i]);
        }

        this.idxMap = new TObjectIntHashMap(candidates.length);
        this.maybeSimilar = new BitSet[candidates.length];
        PeakWithExplanation[][] allFragmentPeaks = new PeakWithExplanation[candidates.length][];
        PeakWithExplanation[][] allLossPeaks = new PeakWithExplanation[candidates.length][];

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Experiment experiment = candidates[i][0].getExperiment();
            FragmentsCandidate[] currentCandidates = candidates[i];

            PeakWithExplanation[] fragmentPeaks = getPeaksWithExplanations(currentCandidates, true);
            allFragmentPeaks[i] = fragmentPeaks;

            PeakWithExplanation[] lossPeaks = getPeaksWithExplanations(currentCandidates, false);
            allLossPeaks[i] = lossPeaks;

            this.idxMap.put(experiment, i);
            this.maybeSimilar[i] = new BitSet();

        }


        for(int i = 0; i < allFragmentPeaks.length; ++i) {
            for(int j = i + 1; j < allFragmentPeaks.length; ++j) {
                final double commonL = this.scoreCommons(allFragmentPeaks[i], allFragmentPeaks[j]);
                final double commonF = this.scoreCommons(allLossPeaks[i], allLossPeaks[j]);
                final double score = ((commonF + commonL) / norm[i]) + ((commonF + commonL) / norm[j]);

                if((commonF + commonL) >= minimum_numer_matched_peaks_losses && (score >= this.threshold)){
                    this.maybeSimilar[i].set(j);

                }

            }
        }

        int sum = 0;
        for (BitSet bitSet : this.maybeSimilar) {
            sum += bitSet.cardinality();
        }
        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum + " | threshold was "+threshold);
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
    private PeakWithExplanation[] getPeaksWithExplanations(FragmentsCandidate[] currentCandidates, final boolean useFragments){
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


        Set<String>[] matchedFragments;
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
                    final String formula = fragments[i].getFormula();
                    final int idx = fragments[i].getIndex()+maxIdx*ionToIdx.get(fragments[i].getIonization());
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                }

            } else {
                fragments = c.getLosses();

                for (int i = 0; i < fragments.length; i++) {
                    final String formula = fragments[i].getFormula();
                    final short idx = fragments[i].getIndex();
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                    }
                    matchedFragments[idx].add(formula);
                }

            }

        }


        int numOfRealPeaks = 0;
        for (Set<String> matched : matchedFragments) {
            if (matched!=null) ++numOfRealPeaks;
        }

        PeakWithExplanation[] peaksWithExplanations = new PeakWithExplanation[numOfRealPeaks];

        pos = 0;
        for (int j = 0; j < matchedFragments.length; j++) {
            if (matchedFragments[j]!=null){
                final String[] mfArray = matchedFragments[j].toArray(new String[0]);
                final double mass = meanMass(mfArray);
                peaksWithExplanations[pos++] = new PeakWithExplanation(mfArray, mass, 1d);
            }
        }

        Arrays.sort(peaksWithExplanations);

        return peaksWithExplanations;
    }

    private double meanMass(String[] formulas){
        FormulaFactory factory = FormulaFactory.getInstance();

        double sum = 0;
        for (String formula : formulas) {
            sum += factory.getFormula(formula).getMass();
        }
        return sum/formulas.length;
    }

    private Set<Ionization> collectIons(FragmentsCandidate[] candidates) {
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


        if(i < j) {
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

        if (commonF+commonL<MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES) return 0;

        final double norm1 = this.normalizationMap.get(candidate1.getExperiment());
        final double norm2 = this.normalizationMap.get(candidate2.getExperiment());
        final double score =  ((commonF + commonL) / norm1) + ((commonF + commonL) / norm2);

        return score;
    }

    @Override
    public double scoreWithoutThreshold(FragmentsCandidate candidate1, FragmentsCandidate candidate2) {
        final double commonF = this.scoreCommons(candidate1.getFragments(), candidate2.getFragments());
        final double commonL = this.scoreCommons(candidate1.getLosses(), candidate2.getLosses());
        final double norm1 = this.normalizationMap.get(candidate1.getExperiment());
        final double norm2 = this.normalizationMap.get(candidate2.getExperiment());

        if (commonF+commonL<MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES) return 0;//todo changed!!!!!!!!!!

        final double score =  ((commonF + commonL) / norm1) + ((commonF + commonL) / norm2);

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

    public double[] normalization(FragmentsCandidate[][] candidates) {
        double[] norm = new double[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate[] compoundCandidates = candidates[i];
            int biggestTreeSize = -1;

            for (FragmentsCandidate compoundCandidate : compoundCandidates) {
                biggestTreeSize = Math.max(biggestTreeSize, compoundCandidate.getFragments().length);
            }

            norm[i] = (double)(2 * biggestTreeSize - 1);
        }

        return norm;
    }

    protected double scoreCommons(PeakWithExplanation[] peaks1, PeakWithExplanation[] peaks2){
        if (peaks1.length==0 || peaks2.length==0) return 0d;
        int commonScore = 0;
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

    private boolean hasMatch(String[] fragments1, String[] fragments2){
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
        int commonCounter = 0;
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


    class PeakWithExplanation implements Comparable<PeakWithExplanation>{
        String[] formulas;
        double mass;
        double bestScore;

        public PeakWithExplanation(String[] formulas, double mass, double bestScore) {
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
