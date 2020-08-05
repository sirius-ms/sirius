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

package de.unijena.bioinf.sirius.plugins;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.BitSet;
import java.util.List;

/**
 * Gather statistics about the number of explained peaks
 *
 * TODO: currently, isotope peaks are not counted...
 */
public class TreeStatisticPlugin extends SiriusPlugin {


    @Override
    public void initializePlugin(PluginInitializer initializer) {

    }


    /**
     * is called before reducing the graph
     * */
    @Override
    protected void afterGraphScoring(ProcessedInput input, FGraph graph) {
        // count how many colors are contained in the graph
        final BitSet color = new BitSet(input.getMergedPeaks().size());
        for (Fragment f : graph) {
            if (f.getColor()<0) continue; //ignore pseudo root
            color.set(f.getColor());
        }

        //todo not sure what we use the baseIntensity for. Currently this is always 1, since spectra are normalized to highest peaks. baseIntensity is used in afterGraphScoring() and makeTreeStatistics() and is canceled out
        final double baseIntensity = input.getMergedPeaks().stream().mapToDouble(x->x.getRelativeIntensity()).max().orElse(1d);

        double totalIntensityOfExplainablePeaks=0d, totalIntensityOfPeaks=0d;
        for (int k=0; k < input.getMergedPeaks().size(); ++k) {
            final ProcessedPeak peak = input.getMergedPeaks().get(k);
            if (peak.isSynthetic()) continue;
            //the next line is necessary because root always seems to be pseudo root.
            if (input.getParentPeak().equals(peak)) continue; //do not consider root fragment for intensity and peak counting
            if (graph.getRoot().getColor()==k){
                throw new RuntimeException("Fragmentation graph root differs from precursor peak.");
            }
            if (color.get(k)) {
                totalIntensityOfExplainablePeaks += input.getMergedPeaks().get(k).getRelativeIntensity()/baseIntensity;
            }
            totalIntensityOfPeaks += input.getMergedPeaks().get(k).getRelativeIntensity()/baseIntensity;
        }

        graph.addAnnotation(RememberNumberOfAnnotatablePeaks.class, new RememberNumberOfAnnotatablePeaks(totalIntensityOfExplainablePeaks, totalIntensityOfPeaks,baseIntensity));
    }

    @Override
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
        tree.setAnnotation(TreeStatistics.class, makeTreeStatistics(input, graph, tree));
    }

    private TreeStatistics makeTreeStatistics(ProcessedInput input, FGraph graph, FTree tree) {
        if (tree.hasAnnotation(PrecursorIonType.class) && !tree.getAnnotation(PrecursorIonType.class).get().hasNeitherAdductNorInsource()){
            //seems that we only have Ionization as PrecursorIonType at this stage
            //hence the following works to exclude precursor peak from counting
            //this would not work for e.g. [M-H2O+H]+
            throw new RuntimeException("Expected a PrecursorIonType without any adduct or in-source fragments.");
        }
        final RememberNumberOfAnnotatablePeaks x = graph.getAnnotationOrThrow(RememberNumberOfAnnotatablePeaks.class);
        double treeIntensity = 0d;
        final double baseIntensity = x.baseIntensity;
        final FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        int numberOfExplainedPeaks = 0;
        for (Fragment f : tree) {
            if (ano.get(f).isArtificial()) continue;
            if (f.isRoot()) continue;
            numberOfExplainedPeaks += 1;
            treeIntensity += ano.get(f).getRelativeIntensity()/baseIntensity;
        }

        //if total intensity / number of all peaks is 0, these values should still be 0 and not NaN
        return new TreeStatistics(treeIntensity==0d ? 0d : (treeIntensity / x.totalIntensityOfPeaks), treeIntensity==0d ? 0d : (treeIntensity / x.totalIntensityOfExplainablePeaks), numberOfExplainedPeaks==0 ? 0d: (1d * numberOfExplainedPeaks / numberOfPeaksWithoutRootAndArtificial(input, tree.getRoot().getColor())));
    }

    private int numberOfPeaksWithoutRootAndArtificial(ProcessedInput input, int rootColor) {
        int numberOfPeaks = 0;
        final List<ProcessedPeak> mergedPeaks = input.getMergedPeaks();
        for (int i = 0; i < mergedPeaks.size(); i++) {
            if (i == rootColor) continue;
            if (mergedPeaks.get(i).isSynthetic()) continue;
            ++numberOfPeaks;
        }
        return numberOfPeaks;
    }

    private static class RememberNumberOfAnnotatablePeaks implements DataAnnotation  {
        private final double totalIntensityOfExplainablePeaks, totalIntensityOfPeaks, baseIntensity;

        public RememberNumberOfAnnotatablePeaks(double totalIntensityOfExplainablePeaks, double totalIntensityOfPeaks, double baseIntensity) {
            this.totalIntensityOfExplainablePeaks = totalIntensityOfExplainablePeaks;
            this.totalIntensityOfPeaks = totalIntensityOfPeaks;
            this.baseIntensity = baseIntensity;
        }
    }


}
