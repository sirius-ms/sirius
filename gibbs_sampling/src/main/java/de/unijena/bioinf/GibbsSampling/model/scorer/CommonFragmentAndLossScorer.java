package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.Transformation;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.MFCandidate;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonFragmentAndLossScorer implements EdgeScorer {
    protected Map<MFCandidate, String[]> fragmentsMap;
    protected Map<MFCandidate, String[]> lossMap;
    protected TObjectIntHashMap<Ms2Experiment> idxMap;
    protected BitSet[] maybeSimilar;
    protected TObjectDoubleHashMap<Ms2Experiment> normalizationMap;
    protected double threshold;
    private final Deviation hugeDeviation = new Deviation(50.0D, 0.01D);
    private final int MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 5;

    public CommonFragmentAndLossScorer(double threshold) {
        this.threshold = threshold;
    }

    public void prepare(MFCandidate[][] candidates) {
        double[] norm = this.normalization(candidates);
        this.normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);

        for(int ms2Spectra = 0; ms2Spectra < candidates.length; ++ms2Spectra) {
            Ms2Experiment ms2LossSpectra = candidates[ms2Spectra][0].getExperiment();
            this.normalizationMap.put(ms2LossSpectra, norm[ms2Spectra]);
        }

        this.fragmentsMap = new HashMap();
        this.lossMap = new HashMap();
        MFCandidate[][] var23 = candidates;
        int var25 = candidates.length;

        int i;
        for(int minTreeSizes = 0; minTreeSizes < var25; ++minTreeSizes) {
            MFCandidate[] ionType = var23[minTreeSizes];
            MFCandidate[] ionTransformation = ionType;
            i = ionType.length;

            for(int ions1 = 0; ions1 < i; ++ions1) {
                MFCandidate j = ionTransformation[ions1];
                FTree ions2 = j.getTree();
                MolecularFormula commonL = ions2.getRoot().getFormula();
                List scores = ions2.getFragments();
                String[] candidate = new String[scores.size() - 1];
                int ion1 = 0;
                Iterator sp1 = scores.iterator();

                Fragment fragment;
                while(sp1.hasNext()) {
                    fragment = (Fragment)sp1.next();
                    if(!fragment.getFormula().equals(commonL)) {
                        candidate[ion1++] = commonL.subtract(fragment.getFormula()).formatByHill();
                    }
                }

                Arrays.sort(candidate);
                this.lossMap.put(j, candidate);
                candidate = new String[scores.size()];
                ion1 = 0;

                for(sp1 = scores.iterator(); sp1.hasNext(); candidate[ion1++] = fragment.getFormula().formatByHill()) {
                    fragment = (Fragment)sp1.next();
                }

                Arrays.sort(candidate);
                this.fragmentsMap.put(j, candidate);
            }
        }

        this.idxMap = new TObjectIntHashMap(candidates.length);
        this.maybeSimilar = new BitSet[candidates.length];
        double[][] var24 = new double[candidates.length][];
        double[][] var26 = new double[candidates.length][];
        int[] var27 = new int[candidates.length];

        int var37;
        for(int var28 = 0; var28 < candidates.length; ++var28) {
            Ms2Experiment var30 = candidates[var28][0].getExperiment();
            i = 2147483647;
            double var31 = 0.0D / 0.0;
            MFCandidate[] var34 = candidates[var28];
            var37 = var34.length;

            int var38;
            for(var38 = 0; var38 < var37; ++var38) {
                MFCandidate var40 = var34[var38];
                FTree var43 = var40.getTree();
                i = Math.min(i, var43.numberOfVertices());
                FragmentAnnotation var44 = var43.getFragmentAnnotationOrThrow(Peak.class);
                var31 = ((Peak)var44.get(var43.getRoot())).getMass();
            }

            double[] var35 = new double[((Ms2Spectrum)var30.getMs2Spectra().get(0)).size()];

            for(var37 = 0; var37 < var35.length; ++var37) {
                var35[var37] = ((Ms2Spectrum)var30.getMs2Spectra().get(0)).getMzAt(var37);
            }

            Arrays.sort(var35);
            double[] var39 = new double[var35.length - 1];

            for(var38 = 0; var38 < var39.length - 1; ++var38) {
                var39[var38] = var31 - var35[var35.length - 2 - var38];
            }

            var24[var28] = var35;
            var26[var28] = var39;
            var27[var28] = 2 * i - 1;
            this.idxMap.put(var30, var28);
            this.maybeSimilar[var28] = new BitSet();
        }

        final PrecursorIonType[] var29 = new PrecursorIonType[1];
        Transformation var10000 = new Transformation() {
            public Peak transform(Peak input) {
                return new Peak(var29[0].precursorMassToNeutralMass(input.getMass()), input.getIntensity());
            }
        };

        for(i = 0; i < var24.length; ++i) {
            Set var32 = this.collectIons(candidates[i]);
            System.out.println("ion size: " + var32.size());

            label68:
            for(int var33 = i + 1; var33 < var24.length; ++var33) {
                Set var36 = this.collectIons(candidates[i]);
                var37 = this.countCommons(var26[i], var26[var33]);
                TDoubleArrayList var41 = new TDoubleArrayList();
                Iterator var42 = var32.iterator();

                while(var42.hasNext()) {
                    PrecursorIonType var45 = (PrecursorIonType)var42.next();
                    var29[0] = var45;
                    double[] var46 = this.mapSpec(var24[i], var45);
                    Iterator var47 = var36.iterator();

                    while(var47.hasNext()) {
                        PrecursorIonType ion2 = (PrecursorIonType)var47.next();
                        var29[0] = ion2;
                        double[] sp2 = this.mapSpec(var24[var33], ion2);
                        int commonF = this.countCommons(var46, sp2);
                        double score = (double)(commonF + var37) / norm[i] + (double)(commonF + var37) / norm[var33];
                        var41.add(score);
                        if(commonF + var37 >= 5 && score >= this.threshold) {
                            this.maybeSimilar[i].set(var33);
                            continue label68;
                        }
                    }
                }
            }
        }

        int sum = 0;
        for (BitSet bitSet : this.maybeSimilar) {
            sum += bitSet.cardinality();
        }
        System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum);
    }

    private double[] mapSpec(double[] spec, PrecursorIonType ionType) {
        double[] s = new double[spec.length];

        for(int i = 0; i < s.length; ++i) {
            s[i] = ionType.precursorMassToNeutralMass(spec[i]);
        }

        return s;
    }

    private Set<PrecursorIonType> collectIons(MFCandidate[] candidates) {
        HashSet ions = new HashSet();
        MFCandidate[] var3 = candidates;
        int var4 = candidates.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            MFCandidate candidate = var3[var5];
            ions.add(candidate.getIonType());
        }

        return ions;
    }

    public double score(MFCandidate candidate1, MFCandidate candidate2) {
        int i = this.idxMap.get(candidate1.getExperiment());
        int j = this.idxMap.get(candidate2.getExperiment());
        if(i < j) {
            if(!this.maybeSimilar[i].get(j)) {
                return 0.0D;
            }
        } else if(!this.maybeSimilar[j].get(i)) {
            return 0.0D;
        }

        String[] fragments1 = (String[])this.fragmentsMap.get(candidate1);
        String[] fragments2 = (String[])this.fragmentsMap.get(candidate2);
        String[] losses1 = (String[])this.lossMap.get(candidate1);
        String[] losses2 = (String[])this.lossMap.get(candidate2);
        int commonF = this.countCommons((Comparable[])fragments1, (Comparable[])fragments2);
        int commonL = this.countCommons((Comparable[])losses1, (Comparable[])losses2);
        return (double)(commonF + commonL) / this.normalizationMap.get(candidate1.getExperiment()) + (double)(commonF + commonL) / this.normalizationMap.get(candidate2.getExperiment());
    }

    public void clean() {
        this.fragmentsMap.clear();
        this.fragmentsMap = null;
        this.lossMap.clear();
        this.lossMap = null;
        this.idxMap.clear();
        this.idxMap = null;
        this.maybeSimilar = null;
    }

    public double[] normalization(MFCandidate[][] candidates) {
        double[] norm = new double[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            MFCandidate[] compoundCandidates = candidates[i];
            int biggestTreeSize = -1;
            MFCandidate[] var6 = compoundCandidates;
            int var7 = compoundCandidates.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                MFCandidate compoundCandidate = var6[var8];
                biggestTreeSize = Math.max(biggestTreeSize, compoundCandidate.getTree().numberOfVertices());
            }

            norm[i] = (double)(2 * biggestTreeSize - 1);
        }

        return norm;
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
}
