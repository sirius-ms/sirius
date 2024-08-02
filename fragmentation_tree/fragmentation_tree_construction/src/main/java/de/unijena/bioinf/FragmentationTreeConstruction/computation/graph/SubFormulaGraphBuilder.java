
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Kai Dührkop
 */
public class SubFormulaGraphBuilder implements GraphBuilder {

    @Override
    public FGraph initializeEmptyGraph(ProcessedInput input) {
        final FGraph graph = new FGraph();
        //graph.addFragmentAnnotation(ProcessedPeak.class);
        //graph.getOrCreateAnnotation(ScoredFormulaMap.class);
        graph.setAnnotation(PrecursorIonType.class, input.getExperimentInformation().getPrecursorIonType());

        return graph;
    }

    @Override
    public FGraph addRoot(FGraph graph, ProcessedPeak peak, Iterable<Decomposition> pmds) {
        final FragmentAnnotation<Decomposition> decomposition = graph.getOrCreateFragmentAnnotation(Decomposition.class);

        final FragmentAnnotation<Peak> peakAno = graph.getOrCreateFragmentAnnotation(Peak.class);

        for (Decomposition m : pmds) {
            final Fragment f = graph.addRootVertex(m.getCandidate(), m.getIon());
            peakAno.set(f, peak);
            f.setPeakId(peak.getIndex());
            f.setColor(peak.getIndex());
            decomposition.set(f, m);
        }
        // set pseudo root
        decomposition.set(graph.getRoot(), new Decomposition(MolecularFormula.emptyFormula(), graph.getAnnotationOrThrow(PrecursorIonType.class).getIonization(), 0d));
        return graph;
    }

    @Override
    public FGraph fillGraph(ProcessedInput input, FGraph graph, final Set<Ionization> allowedIonModes, LossValidator validator) {
        //final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        //final ScoredFormulaMap scoring = graph.getAnnotationOrThrow(ScoredFormulaMap.class);
        final FragmentAnnotation<Decomposition> decomposition = graph.getOrCreateFragmentAnnotation(Decomposition.class);
        final PeakAnnotation<DecompositionList> decompList = input.getPeakAnnotationOrThrow(DecompositionList.class);

        final FragmentAnnotation<Peak> peakAno = graph.getOrCreateFragmentAnnotation(Peak.class);

        // TODO: funktioniert nicht mit verschiedenen IonModes....
        MolecularFormula pmd;
        {
            final Iterator<Fragment> roots = graph.getFragmentsWithoutRoot().iterator();
            pmd = roots.next().getFormula();
            while (roots.hasNext()) {
                pmd = pmd.union(roots.next().getFormula());
            }
        }

        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        for (int i = peaks.size() - 1; i >= 0; --i) {
            final ProcessedPeak peak = peaks.get(i);
            final int pi = peak.getIndex();
            for (Decomposition decomp : decompList.get(peak).getDecompositions()) {
                if (!allowedIonModes.contains(decomp.getIon()))
                    continue;
                final MolecularFormula formula = decomp.getCandidate();
                final boolean hasEdge = formula.getMass() < pmd.getMass() && pmd.isSubtractable(formula);
                if (hasEdge) {
                    Fragment newFragment = null;
                    for (Fragment f : graph) {
//                        if (f.isRoot() || peakAno.get(f).getIndex() == pi || !ion.get(f).equals(decomposition.getIon())) continue;
                        if (f.isRoot() || f.getColor() == pi) continue;
                        final MolecularFormula fragmentFormula = f.getFormula();
                        assert (peaks.get(f.getColor()).getMass() > peak.getMass());
                        if (!fragmentFormula.isEmpty() && fragmentFormula.isSubtractable(formula)) {
                            if (newFragment == null) {
                                newFragment = graph.addFragment(decomp.getCandidate(), decomp.getIon());
                                peakAno.set(newFragment, peak);
                                newFragment.setColor(peak.getIndex());
                                newFragment.setPeakId(peak.getIndex());
                                decomposition.set(newFragment, decomp);
                            }
                            if (!validator.isForbidden(input, graph, f, newFragment))
                                graph.addLoss(f, newFragment);
                        }
                    }
                }
            }
        }
        return graph;
    }

    /*

    @Override
    public FGraph buildGraph(ProcessedInput input, ScoredMolecularFormula pmd) {
        final ProcessedPeak parentPeak = input.getParentPeak();
        final FGraph graph = new FGraph();
        graph.setAnnotation(ProcessedInput.class, input);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.addFragmentAnnotation(ProcessedPeak.class);
        final ScoredFormulaMap scoring = graph.getOrCreateAnnotation(ScoredFormulaMap.class);
        final Fragment root = graph.addRootVertex(pmd.getCandidate());
        scoring.put(pmd.getCandidate(), pmd.getScore());

        final PeakAnnotation<DecompositionList> decompList = input.getPeakAnnotationOrThrow(DecompositionList.class);

        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        int n = peaks.indexOf(parentPeak);
        for (int i = n-1; i >= 0; --i) {
            final ProcessedPeak peak = peaks.get(i);
            final int pi = peak.getIndex();
            for (ScoredMolecularFormula decomposition : decompList.get(peak).getDecompositions()) {
                final MolecularFormula formula = decomposition.getCandidate();
                final boolean hasEdge = pmd.getCandidate().isSubtractable(formula);
                if (hasEdge) {
                    final Fragment newFragment = graph.addFragment(decomposition.getCandidate());
                    peakAno.set(newFragment, peak);
                    scoring.put(decomposition.getCandidate(), decomposition.getScore());

                    graph.addLoss(root, newFragment);
                    for (Fragment f : graph) {
                        if (peakAno.get(f).getIndex() == pi) continue;
                        final MolecularFormula fragmentFormula = f.getCandidate();
                        assert  (peakAno.get(f).getMz() > peak.getMz());
                        if (fragmentFormula.isSubtractable(formula)) {
                            graph.addLoss(f, newFragment);
                        }
                    }
                }
            }
        }
        return graph;
    }
    */
}
