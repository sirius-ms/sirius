package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.Transformation;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.FormulaFactory;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

public class CommonFragmentAndLossScorer implements EdgeScorer<FragmentsCandidate> {
    protected TObjectIntHashMap<Ms2Experiment> idxMap;
    protected BitSet[] maybeSimilar;
    protected TObjectDoubleHashMap<Ms2Experiment> normalizationMap;
    protected double threshold;
    private final Deviation hugeDeviation = new Deviation(100.0D, 0.02D);
    private final int MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 5;

    public CommonFragmentAndLossScorer(double threshold) {
        this.threshold = threshold;
    }

    public void prepare(FragmentsCandidate[][] candidates) {
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

            PeakWithExplanation[] fragmentPeaks = getPeaksWithExplanations(experiment.getMs2Spectra().get(0), currentCandidates, true, experiment);
            allFragmentPeaks[i] = fragmentPeaks;

            Spectrum inverseSpec = Spectrums.getInversedSpectrum(experiment.getMs2Spectra().get(0), experiment.getIonMass());
            PeakWithExplanation[] lossPeaks = getPeaksWithExplanations(inverseSpec, currentCandidates, false, experiment);
            allLossPeaks[i] = lossPeaks;

            this.idxMap.put(experiment, i);
            this.maybeSimilar[i] = new BitSet();

        }


        for(int i = 0; i < allFragmentPeaks.length; ++i) {
            for(int j = i + 1; j < allFragmentPeaks.length; ++j) {
                final int commonL = this.countCommons(allFragmentPeaks[i], allFragmentPeaks[j]);
                final int commonF = this.countCommons(allLossPeaks[i], allLossPeaks[j]);
                final double score = ((double)(commonF + commonL) / norm[i]) + ((double)(commonF + commonL) / norm[j]);

                if((commonF + commonL) >= MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES && (score >= this.threshold)){
                    this.maybeSimilar[i].set(j);

                }

            }
        }

        int sum = 0;
        for (BitSet bitSet : this.maybeSimilar) {
            sum += bitSet.cardinality();
        }
        System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum);
    }



    /**
     *
     * @param spectrum
     * @param currentCandidates
     * @param useFragments true: normal spectrum and fragments, false: inverted spectrum and losses
     * @return
     */
    private PeakWithExplanation[] getPeaksWithExplanations(Spectrum spectrum, FragmentsCandidate[] currentCandidates, final boolean useFragments, Ms2Experiment experiment){
        Set<PrecursorIonType> ions = collectIons(currentCandidates);
        //todo, give somehow

        SimpleMutableSpectrum sortedSpec = new SimpleMutableSpectrum(spectrum); //todo already sorted

        //adding root mass if necessary
        double precursorIon = experiment.getIonMass();
        boolean hasPrecursor = false;
        for (Peak peak : sortedSpec) {
            if (hugeDeviation.inErrorWindow(precursorIon, peak.getMass())){
                hasPrecursor = true;
                break;
            }
        }
        if (!hasPrecursor) sortedSpec.addPeak(precursorIon, -1);

        Spectrums.sortSpectrumByMass(sortedSpec);

        int maxIdx = -1;
        for (FragmentsCandidate currentCandidate : currentCandidates) {
            for (short idx : currentCandidate.getCandidate().getFragIndices()) {
                if (idx>maxIdx) maxIdx = idx;
            }
        }

        TObjectIntHashMap<PrecursorIonType> ionToIdx = new TObjectIntHashMap<>(ions.size());
        int pos = 0;
        for (PrecursorIonType ion : ions) {
            ionToIdx.put(ion, pos++);
        }

        // idx to number of peask
        maxIdx += 1;

        System.out.println("maxIdx"+maxIdx);
        System.out.println(Arrays.toString(currentCandidates[0].getCandidate().getFragIndices()));

        Set<String>[] matchedFragments;
        double[] matchedMasses;
        if (useFragments){
            matchedFragments = new Set[maxIdx*ions.size()];
            matchedMasses = new double[maxIdx*ions.size()];
        }  else {
            matchedFragments = new Set[maxIdx];
            matchedMasses = new double[maxIdx];
        }
        for(int j = 0; j < currentCandidates.length; ++j) {
            FragmentsCandidate c = currentCandidates[j];
            PrecursorIonType currentIon = c.getIonType();
            final String[] formulas;
            final short[] indices;
            if (useFragments){
                formulas = c.getFragments();
                indices = c.getCandidate().getFragIndices();

                for (int i = 0; i < formulas.length; i++) {
                    final String formula = formulas[i];
                    final int idx = indices[i]+maxIdx*ionToIdx.get(currentIon);
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                        matchedMasses[idx] = currentIon.precursorMassToNeutralMass(sortedSpec.getMzAt(indices[i]));
                    }
                    matchedFragments[idx].add(formula);
                }

            } else {
                formulas = c.getLosses();
                indices = c.getCandidate().getLossIndices();

                for (int i = 0; i < formulas.length; i++) {
                    final String formula = formulas[i];
                    final short idx = indices[i];
                    if (matchedFragments[idx]==null){
                        matchedFragments[idx] = new HashSet<>();
                        matchedMasses[idx] = experiment.getIonMass()-sortedSpec.getMzAt(idx);
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
                peaksWithExplanations[pos++] = new PeakWithExplanation(mfArray, mass);
            }
        }

        Arrays.sort(peaksWithExplanations);

        System.out.println("peaksWithExplanations "+peaksWithExplanations.length);

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

    private Set<PrecursorIonType> collectIons(FragmentsCandidate[] candidates) {
        HashSet ions = new HashSet();
        for(int i = 0; i < candidates.length; ++i) {
            FragmentsCandidate candidate = candidates[i];
            ions.add(candidate.getIonType());
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


        final int commonF = this.countCommons(candidate1.getFragments(), candidate2.getFragments());
        final int commonL = this.countCommons(candidate1.getLosses(), candidate2.getLosses());
        final double norm1 = this.normalizationMap.get(candidate1.getExperiment());
        final double norm2 = this.normalizationMap.get(candidate2.getExperiment());
        final double score =  ((double)(commonF + commonL) / norm1) + ((double)(commonF + commonL) / norm2);

        return score;
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

    private int countCommons(PeakWithExplanation[] peaks1, PeakWithExplanation[] peaks2){
        int commonCounter = 0;
        int i = 0;
        int j = 0;
        double mz1 = peaks1[0].mass;
        double mz2 = peaks2[0].mass;

        while(i < peaks1.length && j < peaks2.length) {
            boolean match = hasMatch(peaks1[i].formulas, peaks2[j].formulas);
            int compare = Double.compare(mz1, mz2);
            if(match) {
                ++commonCounter;
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

        return commonCounter;
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


    private int countCommons(Comparable[] fragments1, Comparable[] fragments2) {
        int commonCounter = 0;
        int i = 0;
        int j = 0;

        while(i < fragments1.length && j < fragments2.length) {
            int compare = fragments1[i].compareTo(fragments2[j]);
            if(compare < 0) {
                ++i;
            } else if(compare > 0) {
                ++j;
            } else {
                ++i;
                ++j;
                ++commonCounter;
            }
        }

        return commonCounter;
    }


    class PeakWithExplanation implements Comparable<PeakWithExplanation>{
        String[] formulas;
        double mass;

        public PeakWithExplanation(String[] formulas, double mass) {
            this.formulas = formulas;
            Arrays.sort(this.formulas);
            this.mass = mass;
        }


        @Override
        public int compareTo(PeakWithExplanation o) {
            return Double.compare(mass, o.mass);
        }
    }
}
