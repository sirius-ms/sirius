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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;

public class CommonRootLossScorer extends CommonFragmentScorer {
    public CommonRootLossScorer(double alpha, boolean normalizePerInstance, double threshold) {
        super(alpha, normalizePerInstance, threshold);
    }

    public CommonRootLossScorer() {
        this(1.0D, false, 0.0D);
    }

    public void prepare(FragmentsCandidate[][] candidates) {
        throw new NoSuchMethodError("implement new version");
//        this.fragmentsMap = new HashMap(candidates.length, 0.75F);
//        this.normalizationMap = new TObjectDoubleHashMap(candidates.length, 0.75F, 0.0D / 0.0);
//        FragmentsCandidate[][] norm = candidates;
//        int ms2LossSpectra = candidates.length;
//
//        int i;
//        for(int minTreeSizes = 0; minTreeSizes < ms2LossSpectra; ++minTreeSizes) {
//            FragmentsCandidate[] ionType = norm[minTreeSizes];
//            FragmentsCandidate[] ionTransformation = ionType;
//            i = ionType.length;
//
//            for(int ions1 = 0; ions1 < i; ++ions1) {
//                FragmentsCandidate j = ionTransformation[ions1];
//                MolecularFormula ions2 = j.getFormula();
//                List commonL = j.getTree().getFragments();
//                MolecularFormula[] scores = new MolecularFormula[commonL.size() - 1];
//                int candidate = 0;
//                Iterator ion1 = commonL.iterator();
//
//                while(ion1.hasNext()) {
//                    Fragment sp1 = (Fragment)ion1.next();
//                    if(!sp1.getFormula().equals(ions2)) {
//                        scores[candidate++] = ions2.subtract(sp1.getFormula());
//                    }
//                }
//
//                Arrays.sort(scores);
//                this.fragmentsMap.put(j, scores);
//            }
//        }
//
//        double[] var22 = this.normalization(candidates);
//
//        for(ms2LossSpectra = 0; ms2LossSpectra < candidates.length; ++ms2LossSpectra) {
//            Ms2Experiment var24 = candidates[ms2LossSpectra][0].getExperiment();
//            this.normalizationMap.put(var24, var22[ms2LossSpectra]);
//        }
//
//        this.idxMap = new TObjectIntHashMap(candidates.length);
//        this.maybeSimilar = new BitSet[candidates.length];
//        double[][] var23 = new double[candidates.length][];
//        int[] var25 = new int[candidates.length];
//
//        int var35;
//        for(int var26 = 0; var26 < candidates.length; ++var26) {
//            Ms2Experiment var28 = candidates[var26][0].getExperiment();
//            i = 2147483647;
//            double var29 = 0.0D / 0.0;
//            FragmentsCandidate[] var32 = candidates[var26];
//            var35 = var32.length;
//
//            int var36;
//            for(var36 = 0; var36 < var35; ++var36) {
//                FragmentsCandidate var38 = var32[var36];
//                FTree var41 = var38.getTree();
//                i = Math.min(i, var41.numberOfVertices());
//                FragmentAnnotation var43 = var41.getFragmentAnnotationOrThrow(Peak.class);
//                var29 = ((Peak)var43.get(var41.getRoot())).getMass();
//            }
//
//            double[] var33 = new double[((Ms2Spectrum)var28.getMs2Spectra().get(0)).size()];
//
//            for(var35 = 0; var35 < var33.length; ++var35) {
//                var33[var35] = ((Ms2Spectrum)var28.getMs2Spectra().get(0)).getMzAt(var35);
//            }
//
//            Arrays.sort(var33);
//            System.out.println("spec size " + var33.length);
//            System.out.println("min tree size " + i);
//            double[] var37 = new double[var33.length - 1];
//
//            for(var36 = 0; var36 < var37.length - 1; ++var36) {
//                var37[var36] = var29 - var33[var33.length - 2 - var36];
//            }
//
//            var23[var26] = var37;
//            var25[var26] = i - 1;
//            this.idxMap.put(var28, var26);
//            this.maybeSimilar[var26] = new BitSet();
//        }
//
//        final PrecursorIonType[] var27 = new PrecursorIonType[1];
//        Transformation var10000 = new Transformation() {
//            public Peak transform(Peak input) {
//                return new SimplePeak(var27[0].precursorMassToNeutralMass(input.getMass()), input.getIntensity());
//            }
//        };
//
//        for(i = 0; i < var23.length; ++i) {
//            Set var30 = this.collectIons(candidates[i]);
//            System.out.println("ion size: " + var30.size());
//
//            for(int var31 = i + 1; var31 < var23.length; ++var31) {
//                Set var34 = this.collectIons(candidates[i]);
//                var35 = this.scoreCommons(var23[i], var23[var31]);
//                TDoubleArrayList var39 = new TDoubleArrayList();
//                Iterator var40 = var30.iterator();
//
//                label66:
//                while(var40.hasNext()) {
//                    PrecursorIonType var42 = (PrecursorIonType)var40.next();
//                    var27[0] = var42;
//                    double[] var44 = this.mapSpec(var23[i], var42);
//                    Iterator var16 = var34.iterator();
//
//                    while(var16.hasNext()) {
//                        PrecursorIonType ion2 = (PrecursorIonType)var16.next();
//                        var27[0] = ion2;
//                        double[] sp2 = this.mapSpec(var23[var31], ion2);
//                        int commonF = this.scoreCommons(var44, sp2);
//                        double score = (double)(commonF + var35) / (double)var25[i] + (double)(commonF + var35) / (double)var25[var31];
//                        var39.add(score);
//                        if(commonF + var35 >= 1 && score >= this.threshold) {
//                            this.maybeSimilar[i].set(var31);
//                            break label66;
//                        }
//                    }
//                }
//
//                if(Math.random() > 0.99D) {
//                    System.out.println("scores: " + Arrays.toString(var39.toArray()));
//                }
//            }
//        }
//
//
//        int sum = 0;
//        for (BitSet bitSet : this.maybeSimilar) {
//            sum += bitSet.cardinality();
//        }
//        System.out.println("compounds: " + this.maybeSimilar.length + " | maybeSimilar: " + sum);
    }

    protected MutableMs2Spectrum[] parseSpectra(FragmentsCandidate[][] candidates) {
        MutableMs2Spectrum[] spectra = new MutableMs2Spectrum[candidates.length];

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Spectrum s = (Ms2Spectrum)candidates[i][0].getExperiment().getMs2Spectra().get(0);
            MutableMs2Spectrum spec = new MutableMs2Spectrum(s);
            double root = spec.getPrecursorMz();

            assert root > 50.0D;

            for(int j = 0; j < spec.size(); ++j) {
                spec.setMzAt(j, root - spec.getMzAt(j));
            }

            Spectrums.sortSpectrumByMass(spec);
            spectra[i] = spec;
        }

        return spectra;
    }

    public void clean() {
        this.fragmentsMap.clear();
        this.fragmentsMap = null;
    }
}
