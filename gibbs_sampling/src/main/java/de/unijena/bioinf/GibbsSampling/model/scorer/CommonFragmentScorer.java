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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import de.unijena.bioinf.jjobs.BasicJJob;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommonFragmentScorer implements EdgeScorer<FragmentsCandidate> {
    protected static final int MIN_COMMON = 0;
    protected final double alpha;
    protected final boolean normalizePerInstance;
    protected Map<FragmentsCandidate, MolecularFormula[]> fragmentsMap;
    protected TObjectDoubleHashMap<Ms2Experiment> normalizationMap;
    protected double threshold;
    protected TObjectIntHashMap<Ms2Experiment> idxMap;
    protected BitSet[] maybeSimilar;
    protected final Deviation hugeDeviation;
    protected final int MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES;

    public CommonFragmentScorer(double alpha, boolean normalizePerInstance, double threshold) {
        this.hugeDeviation = new Deviation(50.0D, 0.01D);
        this.MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 1;
        this.alpha = alpha;
        this.normalizePerInstance = normalizePerInstance;
        this.threshold = threshold;
    }

    public CommonFragmentScorer(double alpha) {
        this(alpha, false, 0.0D);
    }

    public void prepare(FragmentsCandidate[][] candidates) {
        throw new NoSuchMethodError("implement new version");
//        this.fragmentsMap = new HashMap(candidates.length, 0.75F);
//        this.normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);
//        FragmentsCandidate[][] norm = candidates;
//        int ms2Spectra = candidates.length;
//
//        int i;
//        int i1;
//        for(int minTreeSizes = 0; minTreeSizes < ms2Spectra; ++minTreeSizes) {
//            FragmentsCandidate[] ionType = norm[minTreeSizes];
//            FragmentsCandidate[] ionTransformation = ionType;
//            i = ionType.length;
//
//            for(int ions1 = 0; ions1 < i; ++ions1) {
//                FragmentsCandidate j = ionTransformation[ions1];
//                List ions2 = j.getTree().getFragments();
//                MolecularFormula[] scores = new MolecularFormula[ions2.size()];
//                i1 = 0;
//
//                Fragment sp1;
//                for(Iterator ion1 = ions2.iterator(); ion1.hasNext(); scores[i1++] = sp1.getFormula()) {
//                    sp1 = (Fragment)ion1.next();
//                }
//
//                Arrays.sort(scores);
//                this.fragmentsMap.put(j, scores);
//            }
//        }
//
//        double[] var21 = this.normalization(candidates);
//
//        for(ms2Spectra = 0; ms2Spectra < candidates.length; ++ms2Spectra) {
//            Ms2Experiment var23 = candidates[ms2Spectra][0].getExperiment();
//            this.normalizationMap.put(var23, var21[ms2Spectra]);
//        }
//
//        this.idxMap = new TObjectIntHashMap(candidates.length);
//        this.maybeSimilar = new BitSet[candidates.length];
//        double[][] var22 = new double[candidates.length][];
//        int[] var24 = new int[candidates.length];
//
//        for(int var25 = 0; var25 < candidates.length; ++var25) {
//            Ms2Experiment var27 = candidates[var25][0].getExperiment();
//            i = 2147483647;
//            double var28 = 0.0D / 0.0;
//            FragmentsCandidate[] var31 = candidates[var25];
//            int var34 = var31.length;
//
//            for(i1 = 0; i1 < var34; ++i1) {
//                FragmentsCandidate var36 = var31[i1];
//                FTree var39 = var36.getTree();
//                i = Math.min(i, var39.numberOfVertices());
//                FragmentAnnotation annotation = var39.getFragmentAnnotationOrThrow(Peak.class);
//                var28 = ((Peak)annotation.get(var39.getRoot())).getMass();
//            }
//
//            double[] var32 = new double[((Ms2Spectrum)var27.getMs2Spectra().get(0)).size()];
//
//            for(var34 = 0; var34 < var32.length; ++var34) {
//                var32[var34] = ((Ms2Spectrum)var27.getMs2Spectra().get(0)).getMzAt(var34);
//            }
//
//            Arrays.sort(var32);
//            System.out.println("spec size " + var32.length);
//            System.out.println("min tree size " + i);
//            var22[var25] = var32;
//            var24[var25] = i;
//            this.idxMap.put(var27, var25);
//            this.maybeSimilar[var25] = new BitSet();
//        }
//
//        final PrecursorIonType[] var26 = new PrecursorIonType[1];
//        Transformation var10000 = new Transformation() {
//            public Peak transform(Peak input) {
//                return new SimplePeak(var26[0].precursorMassToNeutralMass(input.getMass()), input.getIntensity());
//            }
//        };
//
//        for(i = 0; i < var22.length; ++i) {
//            Set var29 = this.collectIons(candidates[i]);
//            System.out.println("ion size: " + var29.size());
//
//            for(int var30 = i + 1; var30 < var22.length; ++var30) {
//                Set var33 = this.collectIons(candidates[i]);
//                TDoubleArrayList var35 = new TDoubleArrayList();
//                Iterator var37 = var29.iterator();
//
//                label60:
//                while(var37.hasNext()) {
//                    PrecursorIonType var38 = (PrecursorIonType)var37.next();
//                    var26[0] = var38;
//                    double[] var40 = this.mapSpec(var22[i], var38);
//                    Iterator var41 = var33.iterator();
//
//                    while(var41.hasNext()) {
//                        PrecursorIonType ion2 = (PrecursorIonType)var41.next();
//                        var26[0] = ion2;
//                        double[] sp2 = this.mapSpec(var22[var30], ion2);
//                        int commonF = this.scoreCommons(var40, sp2);
//                        double score = (double)commonF / (double)var24[i] + (double)commonF / (double)var24[var30];
//                        var35.add(score);
//                        if(commonF >= 1 && score >= this.threshold) {
//                            this.maybeSimilar[i].set(var30);
//                            break label60;
//                        }
//                    }
//                }
//
//                if(Math.random() > 0.99D) {
//                    System.out.println("scores: " + Arrays.toString(var35.toArray()));
//                }
//            }
//        }
//
//        int sum = 0;
//        for (BitSet bitSet : this.maybeSimilar) {
//            sum += bitSet.cardinality();
//        }
//        System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum);
    }

    protected double[] mapSpec(double[] spec, PrecursorIonType ionType) {
        double[] s = new double[spec.length];

        for(int i = 0; i < s.length; ++i) {
            s[i] = ionType.precursorMassToNeutralMass(spec[i]);
        }

        return s;
    }

    protected Set<PrecursorIonType> collectIons(FragmentsCandidate[] candidates) {
        HashSet ions = new HashSet();
        FragmentsCandidate[] var3 = candidates;
        int var4 = candidates.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            FragmentsCandidate candidate = var3[var5];
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

        MolecularFormula[] fragments1 = (MolecularFormula[])this.fragmentsMap.get(candidate1);
        MolecularFormula[] fragments2 = (MolecularFormula[])this.fragmentsMap.get(candidate2);
        int commonCounter = this.countCommons((Comparable[])fragments1, (Comparable[])fragments2);
        return commonCounter < 0?0.0D:(this.normalizePerInstance?this.alpha * (1.0D * (double)commonCounter / (double)fragments1.length + 1.0D * (double)commonCounter / (double)fragments2.length):this.alpha * (double)commonCounter / this.normalizationMap.get(candidate2.getExperiment()));
    }

    @Override
    public double scoreWithoutThreshold(FragmentsCandidate candidate1, FragmentsCandidate candidate2) {
        int i = this.idxMap.get(candidate1.getExperiment());
        int j = this.idxMap.get(candidate2.getExperiment());
        if(i < j) {
            if(!this.maybeSimilar[i].get(j)) {
                return 0.0D;
            }
        } else if(!this.maybeSimilar[j].get(i)) {
            return 0.0D;
        }

        MolecularFormula[] fragments1 = (MolecularFormula[])this.fragmentsMap.get(candidate1);
        MolecularFormula[] fragments2 = (MolecularFormula[])this.fragmentsMap.get(candidate2);
        int commonCounter = this.countCommons((Comparable[])fragments1, (Comparable[])fragments2);
        return commonCounter < 0?0.0D:(this.normalizePerInstance?this.alpha * (1.0D * (double)commonCounter / (double)fragments1.length + 1.0D * (double)commonCounter / (double)fragments2.length):this.alpha * (double)commonCounter / this.normalizationMap.get(candidate2.getExperiment()));
    }

    @Override
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    public int getNumberOfCommon(FragmentsCandidate candidate1, FragmentsCandidate candidate2) {
        MolecularFormula[] fragments1 = (MolecularFormula[])this.fragmentsMap.get(candidate1);
        MolecularFormula[] fragments2 = (MolecularFormula[])this.fragmentsMap.get(candidate2);
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

    public double[] normalization(FragmentsCandidate[][] candidates, double minimum_number_matched_peaks_losses) {
        int[][] maxMatches = this.getMaximumMatchablePeaks(candidates);
        double[] norm = new double[candidates.length];

        for(int i = 0; i < norm.length; ++i) {
            norm[i] = (double)this.sum(maxMatches[i]);
        }

        return norm;
    }

    @Override
    public BasicJJob<Object> getPrepareJob(FragmentsCandidate[][] var1) {
        return new BasicJJob<Object>() {
            @Override
            protected Object compute() throws Exception {
                prepare(var1);
                return true;
            }
        };
    }

    protected int sum(int[] array) {
        int s = 0;
        int[] var3 = array;
        int var4 = array.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            int i = var3[var5];
            s += i;
        }

        return s;
    }

    protected int[][] getMaximumMatchablePeaks(FragmentsCandidate[][] candidates) {
        Deviation deviation = new Deviation(10.0D);
        int[][] maxMatches = new int[candidates.length][candidates.length];
        MutableMs2Spectrum[] spectra = this.parseSpectra(candidates);

        for(int i = 0; i < candidates.length; ++i) {
            MutableMs2Spectrum specl = spectra[i];

            for(int j = i + 1; j < candidates.length; ++j) {
                MutableMs2Spectrum specr = spectra[j];
                int k = 0;
                int l = 0;
                int nl = specl.size();
                int nr = specr.size();
                int matches = 0;

                while(k < nl && l < nr) {
                    double min;
                    double max;
                    if(specl.getMzAt(k) < specr.getMzAt(l)) {
                        min = specl.getMzAt(k);
                        max = specr.getMzAt(l);
                    } else {
                        min = specr.getMzAt(l);
                        max = specl.getMzAt(k);
                    }

                    if(deviation.inErrorWindow(max, min)) {
                        ++matches;
                        ++k;
                        ++l;
                    } else if(specl.getMzAt(k) <= specr.getMzAt(l)) {
                        ++k;
                    } else {
                        ++l;
                    }
                }

                maxMatches[i][j] = maxMatches[j][i] = matches;
            }
        }

        return maxMatches;
    }

    protected MutableMs2Spectrum[] parseSpectra(FragmentsCandidate[][] candidates) {
        MutableMs2Spectrum[] spectra = new MutableMs2Spectrum[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Spectrum s = (Ms2Spectrum)candidates[i][0].getExperiment().getMs2Spectra().get(0);
            MutableMs2Spectrum spec = new MutableMs2Spectrum(s);
            Spectrums.sortSpectrumByMass(spec);
            spectra[i] = spec;
        }

        return spectra;
    }

    public void clean() {
        this.fragmentsMap.clear();
        this.fragmentsMap = null;
        this.idxMap.clear();
        this.idxMap = null;
        this.maybeSimilar = null;
    }

    protected int countCommons(double[] spectrum1, double[] spectrum2) {
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

    protected int countCommons(Comparable[] fragments1, Comparable[] fragments2) {
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
