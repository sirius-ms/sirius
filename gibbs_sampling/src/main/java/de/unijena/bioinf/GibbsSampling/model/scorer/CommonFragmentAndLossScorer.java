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
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

public class CommonFragmentAndLossScorer implements EdgeScorer<FragmentsCandidate> {
//    protected Map<Candidate, String[]> fragmentsMap;
//    protected Map<Candidate, String[]> lossMap;
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

        for(int ms2Spectra = 0; ms2Spectra < candidates.length; ++ms2Spectra) {
            Ms2Experiment ms2LossSpectra = candidates[ms2Spectra][0].getExperiment();
            this.normalizationMap.put(ms2LossSpectra, norm[ms2Spectra]);
        }
//
//        this.fragmentsMap = new HashMap();
//        this.lossMap = new HashMap();
//
//
//        FragmentsCandidate[][] var23 = candidates;
//        int var25 = candidates.length;
//
//        for(int minTreeSizes = 0; minTreeSizes < var25; ++minTreeSizes) {
//            FragmentsCandidate[] ionType = var23[minTreeSizes];
//            FragmentsCandidate[] ionTransformation = ionType;
//            int i = ionType.length;
//
//            for(int ions1 = 0; ions1 < i; ++ions1) {
//                FragmentsCandidate j = ionTransformation[ions1];
//                FTree ions2 = j.getAnnotation(FTree.class);
//                if (ions2.numberOfVertices()!=j.getFragments().length){
//                    System.out.println("number of fragments differs");
//                }
//                MolecularFormula commonL = ions2.getRoot().getFormula();
//                List scores = ions2.getFragments();
//                String[] candidate = new String[scores.size() - 1];
//                int ion1 = 0;
//                Iterator sp1 = scores.iterator();
//
//                Fragment fragment;
//                while(sp1.hasNext()) {
//                    fragment = (Fragment)sp1.next();
//                    if(!fragment.getFormula().equals(commonL)) {
//                        candidate[ion1++] = commonL.subtract(fragment.getFormula()).formatByHill();
//                    }
//                }
//
//                Arrays.sort(candidate);
//
//                if (!compare(candidate, j.getLosses())){
//                    System.out.println("fragments differ");
//                }
//
//                this.lossMap.put(j, candidate);
//                candidate = new String[scores.size()];
//                ion1 = 0;
//
//                for(sp1 = scores.iterator(); sp1.hasNext(); candidate[ion1++] = fragment.getFormula().formatByHill()) {
//                    fragment = (Fragment)sp1.next();
//                }
//
//                Arrays.sort(candidate);
//
//                if (!compare(candidate, j.getFragments())){
//                    System.out.println("fragments differ");
//                } else {
////                    System.out.println("no difference");
//                }
//                this.fragmentsMap.put(j, candidate);
//            }
//        }


//        int var25 = candidates.length;

//
//        for(int i = 0; i < var25; ++i) {
//            Candidate[] currentCandidates = candidates[i];
//
//            for(int j = 0; j < currentCandidates.length; ++j) {
////                Candidate cand = currentCandidates[j];
////                FTree ions2 = cand.getTree();
////                MolecularFormula commonL = ions2.getRoot().getFormula();
////                List scores = ions2.getFragments();
////                String[] candidate = new String[scores.size() - 1];
//
//
//               ....create fragment and loss tables
//            }
//        }

        this.idxMap = new TObjectIntHashMap(candidates.length);
        this.maybeSimilar = new BitSet[candidates.length];
//        double[][] allFragmentMasses = new double[candidates.length][];
//        double[][] allLossMasses = new double[candidates.length][];
        PeakWithExplanation[][] allFragmentPeaks = new PeakWithExplanation[candidates.length][];
        PeakWithExplanation[][] allLossPeaks = new PeakWithExplanation[candidates.length][];
        int[] allNormalizations = new int[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Experiment experiment = candidates[i][0].getExperiment();
            int minVertices = Integer.MAX_VALUE;
            double rootMass = Double.NaN;
            FragmentsCandidate[] currentCandidates = candidates[i];

            double rootSum = 0d;
            for(int j = 0; j < currentCandidates.length; ++j) {
                FragmentsCandidate c = currentCandidates[j];

                minVertices = Math.min(minVertices, c.getFragments().length);
                rootSum += c.getIonType().neutralMassToPrecursorMass(c.getFormula().getIntMass());
            }

//            rootMass = experiment.getIonMass(); //todo probably a bad mass estimation for the precursor?
            rootMass = rootSum/currentCandidates.length;


////            double[] allPeakMasses = Spectrums.copyMasses(experiment.getMs2Spectra().get(0));
//            SimpleMutableSpectrum sortedSpec = new SimpleMutableSpectrum(experiment.getMs2Spectra().get(0)); //todo already sorted
//            Spectrums.sortSpectrumByMass(sortedSpec);
//
//            List<String>[] matchedFragments = new List[sortedSpec.size()];
//            TDoubleArrayList[] matchedMasses = new TDoubleArrayList[sortedSpec.size()];
//            for(int j = 0; j < currentCandidates.length; ++j) {
//                FragmentsCandidate c = currentCandidates[j];
//                String[] formulas = c.getFragments();
//                for (String formula : formulas) {
//                    MolecularFormula mf = MolecularFormula.parse(formula);
//                    int pos = Spectrums.binarySearch(sortedSpec, mf.getMass(), hugeDeviation);
//                    if (pos<0) throw new NoSuchElementException("Did not find a corresponding peak for the fragment");
//                    if (matchedFragments[pos]==null){
//                        matchedFragments[pos] = new ArrayList<>();
//                        matchedMasses[pos] = new TDoubleArrayList();
//                    }
//                    matchedFragments[pos].add(formula);
//                    matchedMasses[pos].add(mf.getMass());
//                }
//
//                rootSum += c.getIonType().neutralMassToPrecursorMass(c.getFormula().getIntMass());
//            }
//
//
//            int numOfRealPeaks = 0;
//            for (List<String> matched : matchedFragments) {
//                if (matched!=null) ++numOfRealPeaks;
//            }
////            String[][] matchedFragmentsFinal = new String[numOfRealPeaks][];
//            PeakWithExplanation[] peaksWithExplanations = new PeakWithExplanation[numOfRealPeaks];
//
//            int pos = 0;
//            for (int j = 0; j < matchedFragments.length; j++) {
//                if (matchedFragments[j]!=null) peaksWithExplanations[pos++] =new PeakWithExplanation(matchedFragments[j].toArray(new String[0]), matchedMasses[j].sum()/matchedMasses[j].size());
//            }

            PeakWithExplanation[] fragmentPeaks = getPeaksWithExplanations(experiment.getMs2Spectra().get(0), currentCandidates, true, experiment);

            Spectrum inverseSpec = Spectrums.getInversedSpectrum(experiment.getMs2Spectra().get(0), experiment.getIonMass());
            PeakWithExplanation[] lossPeaks = getPeaksWithExplanations(inverseSpec, currentCandidates, false, experiment);


//            double[] fragmentMasses = new double[experiment.getMs2Spectra().get(0).size()];
//
//            for(int j = 0; j < fragmentMasses.length; ++j) {
//                fragmentMasses[j] = experiment.getMs2Spectra().get(0).getMzAt(j);
//            }
//
//            Arrays.sort(fragmentMasses);
//            double[] lossMasses = new double[fragmentMasses.length - 1];
//
//            for(int j = 0; j < lossMasses.length - 1; ++j) {
//                lossMasses[j] = rootMass - fragmentMasses[fragmentMasses.length - 2 - j];
//            }

//            allFragmentMasses[i] = fragmentMasses;
//            allLossMasses[i] = lossMasses;
            allFragmentPeaks[i] = fragmentPeaks;
            allLossPeaks[i] = lossPeaks;

            allNormalizations[i] = 2 * minVertices - 1; //todo normalize by MAX
            this.idxMap.put(experiment, i);
            this.maybeSimilar[i] = new BitSet();
        }


        for(int i = 0; i < allFragmentPeaks.length; ++i) {
            Set<PrecursorIonType> possibleIons1 = this.collectIons(candidates[i]);
            System.out.println("ion size: " + possibleIons1.size());

            outer:
            for(int j = i + 1; j < allFragmentPeaks.length; ++j) {
//                Set<PrecursorIonType> possibleIons2 = this.collectIons(candidates[i]);
                int commonL = this.countCommons(allFragmentPeaks[i], allFragmentPeaks[j]);

//                for (PrecursorIonType ion1 : possibleIons1) {
//                    double[] neutralSpec = this.mapSpec(allFragmentMasses[i], ion1);
//                    for (PrecursorIonType ion2 : possibleIons2) {
//                        double[] sp2 = this.mapSpec(allFragmentMasses[j], ion2);
//                        int commonF = this.countCommons(neutralSpec, sp2);
//                        double score = (double)(commonF + commonL) / norm[i] + (double)(commonF + commonL) / norm[j];
//                        if(commonF + commonL >= MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES && score >= this.threshold) {
//                            this.maybeSimilar[i].set(j);
//                            continue outer;
//                        }
//                    }
//                }

                int commonF = this.countCommons(allLossPeaks[i], allLossPeaks[j]);
                double score = (double)(commonF + commonL) / norm[i] + (double)(commonF + commonL) / norm[j];
                if(commonF + commonL >= MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES && score >= this.threshold){
                    this.maybeSimilar[i].set(j);
                }

//                //todo changed just to test
////                if(commonF + commonL >= MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES){
//                    this.maybeSimilar[i].set(j);
////                }
            }
        }

        int sum = 0;
        for (BitSet bitSet : this.maybeSimilar) {
            sum += bitSet.cardinality();
        }
        System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum);
    }


//    private boolean compare(Object[] a1, Object[] a2) {
//        for (int i = 0; i < a1.length; i++) {
//            if (!a1[i].equals(a2[i])){
//                System.out.println("arrays differ ");
//                System.out.println(Arrays.toString(a1));
//                System.out.println(Arrays.toString(a2));
//                return false;
//            }
//        }
//        return true;
//    }


    private final PrecursorIonType NO_ION = PrecursorIonType.getPrecursorIonType(new IonMode(0d, 1, "")); //on ion

    /**
     *
     * @param spectrum
     * @param currentCandidates
     * @param useFragments true: normal spectrum and fragments, false: inverted spectrum and losses
     * @return
     */
    private PeakWithExplanation[] getPeaksWithExplanations(Spectrum spectrum, FragmentsCandidate[] currentCandidates, final boolean useFragments, Ms2Experiment experiment){
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



        List<String>[] matchedFragments = new List[sortedSpec.size()];
        double[] matchedMasses = new double[sortedSpec.size()];
        for(int j = 0; j < currentCandidates.length; ++j) {
            FragmentsCandidate c = currentCandidates[j];
            final PrecursorIonType ionType;
            final String[] formulas;
            if (useFragments){
                formulas = c.getFragments();
                ionType = c.getIonType();
            } else {
                formulas = c.getLosses();
                ionType = NO_ION;
            }
            for (String formula : formulas) {
                MolecularFormula mf = MolecularFormula.parse(formula);
                double ionMass = ionType.neutralMassToPrecursorMass(mf.getMass());
                int pos = Spectrums.binarySearch(sortedSpec, ionMass, hugeDeviation);
                if (pos<0){
                    System.out.println("useFragments "+useFragments);
                    System.out.println("spektrum "+Arrays.toString(Spectrums.copyMasses(sortedSpec)));
                    System.out.println("ion "+ionType.toString());
                    System.out.println(mf.formatByHill()+" "+mf.getMass()+" "+ionType.neutralMassToPrecursorMass(mf.getMass()));
                    throw new NoSuchElementException("Did not find a corresponding peak for the fragment");
                }
                if (matchedFragments[pos]==null){
                    matchedFragments[pos] = new ArrayList<>();
                    matchedMasses[pos] = sortedSpec.getMzAt(pos);
                }
                matchedFragments[pos].add(formula);
            }

        }


        int numOfRealPeaks = 0;
        for (List<String> matched : matchedFragments) {
            if (matched!=null) ++numOfRealPeaks;
        }

        PeakWithExplanation[] peaksWithExplanations = new PeakWithExplanation[numOfRealPeaks];

        int pos = 0;
        for (int j = 0; j < matchedFragments.length; j++) {
            if (matchedFragments[j]!=null) peaksWithExplanations[pos++] = new PeakWithExplanation(matchedFragments[j].toArray(new String[0]), matchedMasses[j]);
        }

        return peaksWithExplanations;
    }

    private double[] mapSpec(double[] spec, PrecursorIonType ionType) {
        double[] s = new double[spec.length];

        for(int i = 0; i < s.length; ++i) {
            s[i] = ionType.precursorMassToNeutralMass(spec[i]);
        }

        return s;
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
        } else if(!this.maybeSimilar[j].get(i)) {
            return 0.0D;
        }

//        String[] fragments1 = (String[])this.fragmentsMap.get(candidate1);
//        String[] fragments2 = (String[])this.fragmentsMap.get(candidate2);
//        String[] losses1 = (String[])this.lossMap.get(candidate1);
//        String[] losses2 = (String[])this.lossMap.get(candidate2);
        int commonF = this.countCommons(candidate1.getFragments(), candidate2.getFragments());
        int commonL = this.countCommons(candidate1.getLosses(), candidate2.getLosses());
        return (double)(commonF + commonL) / this.normalizationMap.get(candidate1.getExperiment()) + (double)(commonF + commonL) / this.normalizationMap.get(candidate2.getExperiment());
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
            FragmentsCandidate[] var6 = compoundCandidates;
            int var7 = compoundCandidates.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                FragmentsCandidate compoundCandidate = var6[var8];
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

    private int countCommons(double[] spectrum1, double[] spectrum2) {
        int commonCounter = 0;
        int i = 0;
        int j = 0;
        double mz1 = spectrum1[0];
        double mz2 = spectrum2[0];

        while(i < spectrum1.length && j < spectrum2.length) {
            boolean match = this.hugeDeviation.inErrorWindow(mz1, mz2);
            int compare = Double.compare(mz1, mz2);
            if(match) {
                ++commonCounter;
                ++i;
                ++j;
                if(i >= spectrum1.length || j >= spectrum2.length) {
                    break;
                }

                mz1 = spectrum1[i];
                mz2 = spectrum2[j];
            } else if(compare < 0) {
                ++i;
                if(i >= spectrum1.length) {
                    break;
                }

                mz1 = spectrum1[i];
            } else {
                ++j;
                if(j >= spectrum2.length) {
                    break;
                }

                mz2 = spectrum2[j];
            }
        }

        return commonCounter;
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


    class PeakWithExplanation {
        String[] formulas;
        double mass;

        public PeakWithExplanation(String[] formulas, double mass) {
            this.formulas = formulas;
            Arrays.sort(this.formulas);
            this.mass = mass;
        }
    }
}
