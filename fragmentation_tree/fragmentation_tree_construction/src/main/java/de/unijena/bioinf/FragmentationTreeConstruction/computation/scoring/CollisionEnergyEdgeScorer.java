
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1. fragment appears at lower collision energies than parent {@literal ->} alpha (10%) probability
 * 2. fragment appears at higher collision energies than parent
 *      - for three collision energies c1 {@literal <} c2 {@literal <} c3 where the parent appears lastly at c1 and the fragment
 *        appears firstly at c3, alpha (10%) probability
 *      - if fragment appears direct after parent, give beta (80%)probability
 * otherwise 100% probability
 */
@Called("Collision Energy")
public class CollisionEnergyEdgeScorer implements PeakPairScorer {

    private double alpha, beta, logAlpha, logBeta;

    public CollisionEnergyEdgeScorer() {
        this(0.1, 0.8);
    }

    public CollisionEnergyEdgeScorer(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
        this.logAlpha = Math.log(alpha);
        this.logBeta = Math.log(beta);
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
        this.logAlpha = Math.log(alpha);
    }

    public void setBeta(double beta) {
        this.beta = beta;
        this.logBeta = Math.log(beta);
    }
    /*
    public Object prepare(ProcessedInput input, FragmentationGraph graph) {
        final int n = input.getMergedPeaks().size();
        final int[] minEnergy = new int[n], maxEnergy = new int[n];
        for (int i=0; i < input.getMergedPeaks().size(); ++i) assert input.getMergedPeaks().get(i).getIndex() == i;
        Arrays.fill(minEnergy, -1); Arrays.fill(maxEnergy, -1);
        final Ms2Spectrum[] spectra = input.getOriginalInput().getMs2Spectra().toArray(new Ms2Spectrum[0]);
        final List<ProcessedPeak>[] peaksPerSpectra = new List[spectra.length];
        for (int i=0; i < peaksPerSpectra.length; ++i) {
            final int k = i;
            peaksPerSpectra[i] = Lists.select(input.getMergedPeaks(), new Predicate<ProcessedPeak>() {
               @Override
               public boolean apply(ProcessedPeak value) {
                   for (MS2Peak p : value.getOriginalPeaks()) {
                       if (p.getSpectrum() == spectra[k]) return true;
                   }
                   return false;
               }
           });
        }
        Arrays.sort(spectra, Ms2Spectrum.getEnergyComparator());
        final double [] energies = new double[spectra.length];
        int energySize = 0;
        for (int i=0; i < spectra.length; ++i) {
            final Ms2Spectrum s = spectra[i];
            final double energy = Math.abs(s.getCollisionEnergy());
            if (energySize==0 || energies[energySize-1] < energy) energies[energySize++] = energy;
            for (ProcessedPeak peak : peaksPerSpectra[i]) {
                final int j = peak.getIndex();
                final int lastIndex = maxEnergy[j];
                if (lastIndex < 0) {
                    minEnergy[j] = maxEnergy[j] = energySize-1;
                } else {
                    maxEnergy[j] = energySize-1;
                }
            }
        }
        return new int[][]{minEnergy, maxEnergy};
    }
    */
    /*
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final int[][] prec = (int[][])precomputed;
        final int[] min = prec[0], max = prec[1];
        final int parent = loss.getHead().getPeak().getIndex();
        final int fragment = loss.getTail().getPeak().getIndex();
        // 1. fragment appears at lower collision energies than parent -> alpha (10%) probability
        if (min[fragment] < min[parent]) return logAlpha;
        // 2. fragment appears at higher collision energies than parent
        if (max[parent] < min[fragment]) {
            final int intermediates = max[parent] - min[fragment] - 1;
            //      - for three collision energies c1 < c2 < c3 where the parent appears lastly at c1 and the fragment
            //        appears firstly at c3, alpha (10%) probability
            if (intermediates >= 1 ) {
                return logAlpha;
            } else if (intermediates == 0) {
            // if fragment appears direct after parent, give beta (80%)probability
                return logBeta;
            }
        }
        // otherwise 100% probability
        return 0d;
    }
    */

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        final List<? extends Ms2Spectrum> spectra = input.getExperimentInformation().getMs2Spectra();
        // map the different collision energies to indizes
        // for example: you have the energies [10, 20, 30, 40, 60] and you map each
        // energy to an index: 10 -> 0, 20 -> 1, 30 -> 2, 40 -> 3, 60 -> 4.
        final double[] energies;
        {
            final TDoubleArrayList energyList = new TDoubleArrayList(spectra.size());
            final double[] points = new double[spectra.size()*2];
            int k=0;
            for (Ms2Spectrum ms : spectra) {
                final CollisionEnergy e = ms.getCollisionEnergy();
                if (e==null) {
                    // cannot score collision energy if these is not contained in input
                    return;
                }
                points[k++] = e.getMinEnergy();
                points[k++] = e.getMaxEnergy();
            }
            Arrays.sort(points);
            energyList.add(points[0]);
            for (int i=1; i < points.length; ++i) {
                if (points[i] != points[i-1]) {
                    energyList.add(points[i]);
                }
            }
            energies = energyList.toArray();
        }
        // next: transform each energy to its indizes
        // so 10 becomes 0, 20-30 becomes 1-2 and so on
        final int[] energyIndizesMin = new int[spectra.size()];
        final int[] energyIndizesMax = new int[spectra.size()];
        for (int i=0; i < spectra.size(); ++i) {
            final CollisionEnergy ce = spectra.get(i).getCollisionEnergy();
            energyIndizesMin[i] = Arrays.binarySearch(energies, ce.getMinEnergy());
            energyIndizesMax[i] = Arrays.binarySearch(energies, ce.getMaxEnergy());
            assert energyIndizesMin[i] >= 0;
            assert energyIndizesMax[i] >= 0;
        }

        // now map each peak to its collision energy intervall
        final int n = input.getMergedPeaks().size();
        final int[] minEnergy = new int[n], maxEnergy = new int[n];
        for (int i=0; i < input.getMergedPeaks().size(); ++i) assert input.getMergedPeaks().get(i).getIndex() == i;
        Arrays.fill(minEnergy, Integer.MAX_VALUE); Arrays.fill(maxEnergy, Integer.MIN_VALUE);
        final List<ProcessedPeak>[] peaksPerSpectra = new List[spectra.size()];
        for (int i=0; i < peaksPerSpectra.length; ++i) {
            peaksPerSpectra[i] = new ArrayList<ProcessedPeak>();
            final int k = i;
            for (ProcessedPeak value : input.getMergedPeaks()) {
                for (MS2Peak p : value.getOriginalPeaks()) {
                    if (p.getSpectrum().getCollisionEnergy().equals(spectra.get(k).getCollisionEnergy())) {
                        peaksPerSpectra[i].add(value);
                        break;
                    }
                }
            }
        }
        for (int i=0; i < spectra.size(); ++i) {
            for (ProcessedPeak peak : peaksPerSpectra[i]) {
                final int j = peak.getIndex();
                minEnergy[j] = Math.min(minEnergy[j], energyIndizesMin[i]);
                maxEnergy[j] = Math.max(maxEnergy[j], energyIndizesMax[i]);
            }
        }

        // now the scoring of each pair of peaks
        for (int parent = 0; parent < peaks.size(); ++parent) {
            // if the parent fragment is synthetic, you can't give a score
            final ProcessedPeak parentFragment = peaks.get(parent);
            if (parentFragment.isSynthetic()) {
                continue;
            }
            if (minEnergy[parent]==maxEnergy[parent] && minEnergy[parent]==0) continue;
            for (int fragment = 0; fragment < parent; ++fragment) {
                if (minEnergy[fragment]==maxEnergy[fragment] && minEnergy[fragment]==0) continue;
                // you don't have to score pairs where the parent is smaller than the fragment, because
                // we don't allow this in later steps --> so matrix is not symmetric
                if (parentFragment.getMass() <= peaks.get(fragment).getMass())  {
                    scores[parent][fragment] += Double.NEGATIVE_INFINITY;
                    assert false;
                }
                // 1. fragment appears at lower collision energies than parent -> alpha (10%) probability
                if (minEnergy[fragment] < minEnergy[parent]) {
                    scores[parent][fragment] += logAlpha;
                    // 2. fragment appears at higher collision energies than parent
                } else if (maxEnergy[parent] < minEnergy[fragment]) {
                    final int intermediates = maxEnergy[parent] - minEnergy[fragment] - 1;
                    //      - for three collision energies c1 < c2 < c3 where the parent appears lastly at c1 and the fragment
                    //        appears firstly at c3, alpha (10%) probability
                    if (intermediates >= 1) {
                        scores[parent][fragment] += logAlpha;
                    } else if (intermediates == 0) {
                        // if fragment appears direct after parent, give beta (80%)probability
                        scores[parent][fragment] += logBeta;
                    } else {
                        // give 100% probability
                    }
                }
            }
        }
    }
    /*
    @Override
    public void scoreOld(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        final int n = input.getMergedPeaks().size();
        final int[] minEnergy = new int[n], maxEnergy = new int[n];
        for (int i = 0; i < input.getMergedPeaks().size(); ++i) assert input.getMergedPeaks().get(i).getIndex() == i;
        Arrays.fill(minEnergy, -1);
        Arrays.fill(maxEnergy, -1);
        final Ms2Spectrum[] spectra = input.getOriginalInput().getMs2Spectra().toArray(new Ms2Spectrum[0]);
        final List<ProcessedPeak>[] peaksPerSpectra = new List[spectra.length];
        for (int i = 0; i < peaksPerSpectra.length; ++i) {
            final int k = i;
            peaksPerSpectra[i] = Lists.select(input.getMergedPeaks(), new Predicate<ProcessedPeak>() {
                @Override
                public boolean apply(ProcessedPeak value) {
                    for (MS2Peak p : value.getOriginalPeaks()) {
                        if (p.getSpectrum() == spectra[k]) return true;
                    }
                    return false;
                }
            });
        }
        Arrays.sort(spectra, Ms2Spectrum.getEnergyComparator());
        final double[] energies = new double[spectra.length];
        int energySize = 0;
        for (int i = 0; i < spectra.length; ++i) {
            final Ms2Spectrum s = spectra[i];
            final CollisionEnergy energy = s.getCollisionEnergy();
            if (energySize == 0 || energies[energySize - 1] < energy) energies[energySize++] = energy;
            for (ProcessedPeak peak : peaksPerSpectra[i]) {
                final int j = peak.getIndex();
                final int lastIndex = maxEnergy[j];
                if (lastIndex < 0) {
                    minEnergy[j] = maxEnergy[j] = energySize - 1;
                } else {
                    maxEnergy[j] = energySize - 1;
                }
            }
        }
        for (int parent = 0; parent < peaks.size(); ++parent) {
            for (int fragment = 0; fragment < peaks.size(); ++fragment) {
                if (peaks.get(parent).getMz() <= peaks.get(fragment).getMz())
                    scores[parent][fragment] += Double.NEGATIVE_INFINITY;
                // 1. fragment appears at lower collision energies than parent -> alpha (10%) probability
                if (minEnergy[fragment] < minEnergy[parent]) {
                    scores[parent][fragment] += logAlpha;
                    // 2. fragment appears at higher collision energies than parent
                } else if (maxEnergy[parent] < minEnergy[fragment]) {
                    final int intermediates = maxEnergy[parent] - minEnergy[fragment] - 1;
                    //      - for three collision energies c1 < c2 < c3 where the parent appears lastly at c1 and the fragment
                    //        appears firstly at c3, alpha (10%) probability
                    if (intermediates >= 1) {
                        scores[parent][fragment] += logAlpha;
                    } else if (intermediates == 0) {
                        // if fragment appears direct after parent, give beta (80%)probability
                        scores[parent][fragment] += logBeta;
                    } else {
                        // give 100% probability
                    }
                }
            }
        }
    }


        for (int parent = 0; parent < peaks.size(); ++parent) {
            final ProcessedPeak p = peaks.get(parent);
            for (int fragment = 0; fragment < peaks.size(); ++fragment) {
                final ProcessedPeak f = peaks.get(fragment);
                if (p.getMz() <= f.getMz())
                    scores[parent][fragment] += Double.NEGATIVE_INFINITY;
                // 1. fragment appears at lower collision energies than parent -> alpha (10%) probability
                if (f.getCollisionEnergy().getMinEnergy() < p.getCollisionEnergy().getMinEnergy()) {
                    scores[parent][fragment] += logAlpha;
                    // 2. fragment appears at higher collision energies than parent
                } else if (p.getCollisionEnergy().getMaxEnergy() < f.getCollisionEnergy().getMinEnergy()[fragment]) {
                    final int intermediates = maxEnergy[parent] - minEnergy[fragment] - 1;
                    //      - for three collision energies c1 < c2 < c3 where the parent appears lastly at c1 and the fragment
                    //        appears firstly at c3, alpha (10%) probability
                    if (intermediates >= 1) {
                        scores[parent][fragment] += logAlpha;
                    } else if (intermediates == 0) {
                        // if fragment appears direct after parent, give beta (80%)probability
                        scores[parent][fragment] += logBeta;
                    } else {
                        // give 100% probability
                    }
                }
            }
        }
    }
    */

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        setAlpha(document.getDoubleFromDictionary(dictionary, "alpha"));
        setBeta(document.getDoubleFromDictionary(dictionary, "beta"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "alpha", alpha);
        document.addToDictionary(dictionary, "beta", beta);
    }
}
