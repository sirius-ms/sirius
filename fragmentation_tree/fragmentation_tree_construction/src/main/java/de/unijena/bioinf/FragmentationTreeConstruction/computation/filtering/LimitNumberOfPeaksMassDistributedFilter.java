/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;

import java.util.*;

// - divide mass range in 4 blocks (each with 1/3 of the mass range, except the first low mass block which
//   is additionally splitted into two blocks of 1/6 of the mass range)
// - now select the n most intensive peaks such that
//   - 1/5 n of the most intensive peaks in the spectrum are selected
//   - 1/4 n of the most intensive peaks peaks in each block are selected
// This processor should take care that peaks in the low mass range are not deleted just because
// peaks in high mass range have larger intensities

//
public class LimitNumberOfPeaksMassDistributedFilter implements PostProcessor, Initializable {

    protected DecomposerCache cache;

    private double[] masses;
    private int[] limits;

    private static double[] DEFAULT_MASSES = new double[]{300, 500, 700};
    private static int[] DEFAULT_LIMITS = new int[]{60, 80, 100};

    public DecomposerCache getCache() {
        if (cache == null) cache = new DecomposerCache(3);
        return cache;
    }


    @Override
    public void initialize(FragmentationPatternAnalysis analysis) {
        this.cache = analysis.getDecomposerCache();
    }

    public LimitNumberOfPeaksMassDistributedFilter() {
        this(DEFAULT_LIMITS, DEFAULT_MASSES);
    }

    public LimitNumberOfPeaksMassDistributedFilter(int[] limits, double[] masses) {
        if (limits.length!=masses.length) throw new IllegalArgumentException();
        this.limits = limits.clone();
        this.masses = masses.clone();
    }

    public int getLimit(double mass) {
        for (int i=0; i < masses.length; ++i) {
            if (mass < masses[i]) {
                if (i==0) return limits[0];
                else return (int)Math.floor(limits[i-1] + (limits[i]-limits[i-1])*((mass-masses[i-1])/(masses[i]-masses[i-1])));
            }
        }
        return limits[limits.length-1];
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        final int limit = getLimit(input.getExperimentInformation().getIonMass());
        // remove peaks without decomposition
        /*{
            final MassToFormulaDecomposer decomposer = getCache().getDecomposer(input.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet());
            final ListIterator<ProcessedPeak> iter = input.getMergedPeaks().listIterator();
            eachPeak:
            while (iter.hasNext()) {
                final Iterator<MolecularFormula> fiter = decomposer.formulaIterator(iter.next().getUnmodifiedMass(), input.getMeasurementProfile().getAllowedMassDeviation(), input.getMeasurementProfile().getFormulaConstraints());
                if (fiter.hasNext() && fiter.next() != null) continue eachPeak;
                iter.remove();
            }
        }
        */
        final BitSet keepPeaks = new BitSet(input.getMergedPeaks().size());
        // divide spectrum in four parts
        // 2/3 - 1
        // 1/3 - 2/3
        // 1/6-2/6
        // 0 - 1/6
        final double parentmass = input.getExperimentInformation().getIonMass();
        final double blocksize = parentmass/6d;
        final int numberOfPeaksPerBlock = limit/8;

        final List<ProcessedPeak> orderedByIntensity = new ArrayList<>(input.getMergedPeaks());
        Collections.sort(orderedByIntensity, new ProcessedPeak.RelativeIntensityComparator());
        Collections.reverse(orderedByIntensity);
        final double maxMass = parentmass-1;
        int selected=0;
        selected += keep(orderedByIntensity, keepPeaks, 0, maxMass, 4*numberOfPeaksPerBlock);
        selected += keep(orderedByIntensity, keepPeaks, 0, blocksize, numberOfPeaksPerBlock);
        selected += keep(orderedByIntensity, keepPeaks, blocksize, 2*blocksize, numberOfPeaksPerBlock);
        selected += keep(orderedByIntensity, keepPeaks, 2*blocksize, 4*blocksize, numberOfPeaksPerBlock);
        selected += keep(orderedByIntensity, keepPeaks, 4*blocksize, 6*blocksize, numberOfPeaksPerBlock);
        keep(orderedByIntensity, keepPeaks, 0, maxMass, Math.max(0, limit-selected));

        final ListIterator<ProcessedPeak> iter = orderedByIntensity.listIterator();
        int index=0;
        while (iter.hasNext()) {
            iter.next();
            if (!keepPeaks.get(index++)) {
                iter.remove();
            }
        }
        Collections.sort(orderedByIntensity, new ProcessedPeak.MassComparator());
        input.setMergedPeaks(orderedByIntensity);
        return input;
    }

    private int keep(List<ProcessedPeak> orderedByIntensity, BitSet keepPeaks, double from, double to, int numberOfPeaks) {
        if (numberOfPeaks<=0)  return 0;
        int index = 0;
        int total = numberOfPeaks;
        for (ProcessedPeak peak : orderedByIntensity) {
            if (peak.getMass() >= from && peak.getMass() < to && !keepPeaks.get(index)) {
                keepPeaks.set(index, true);
                if (--numberOfPeaks <= 0) break;
            }
            ++index;
        }
        return total-numberOfPeaks;
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        ArrayList<double[]> pairs = new ArrayList<>();
        L xs = document.getListFromDictionary(dictionary, "limits");
        for (int i=0, n = document.sizeOfList(xs); i < n; ++i) {
            L v = document.getListFromList(xs, i);
            if (document.sizeOfList(v)==1)
                pairs.add(new double[]{Double.POSITIVE_INFINITY, document.getIntFromList(v, 0)});
            else
                pairs.add(new double[]{document.getDoubleFromList(v, 0), document.getIntFromList(v, 1)});
        }
        Collections.sort(pairs, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return Double.compare(o1[0], o2[0]);
            }
        });
        this.limits = new int[pairs.size()];
        this.masses = new double[pairs.size()];
        for (int i=0; i < pairs.size(); ++i) {
            limits[i] = (int)pairs.get(i)[1];
            masses[i] = pairs.get(i)[0];
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L pairs = document.newList();
        for (int i=0; i < masses.length; ++i) {
            L pair = document.newList();
            if (Double.isInfinite(masses[i])) {
                document.addToList(pair, limits[i]);
            } else {
                document.addToList(pair, masses[i]);
                document.addToList(pair, limits[i]);
            }
            document.addListToList(pairs, pair);
        }
        document.addListToDictionary(dictionary, "limits", pairs);
    }
}
