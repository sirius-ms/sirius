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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.IonizedMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdductSwitches;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class SubFormulaGraphBuilder implements GraphBuilder {

    @Override
    public FGraph initializeEmptyGraph(ProcessedInput input) {
        final FGraph graph = new FGraph();
        graph.addAnnotation(ProcessedInput.class, input);
        graph.addFragmentAnnotation(ProcessedPeak.class);
        graph.getOrCreateAnnotation(ScoredFormulaMap.class);
        graph.addAnnotation(Ionization.class, input.getExperimentInformation().getPrecursorIonType().getIonization());
        graph.addAnnotation(PrecursorIonType.class, input.getExperimentInformation().getPrecursorIonType());
        return graph;
    }

    @Override
    public FGraph addRoot(FGraph graph, ProcessedPeak peak, Iterable<Decomposition> pmds) {
        final FragmentAnnotation<Ionization> ion = graph.addFragmentAnnotation(Ionization.class);
        final ScoredFormulaMap scoring = graph.getOrCreateAnnotation(ScoredFormulaMap.class);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);

        for (Decomposition m : pmds) {
            final Fragment f = graph.addRootVertex(m.getCandidate(), m.getIon());
            peakAno.set(f, peak);
            f.setColor(peak.getIndex());
            ion.set(f, m.getIon());
            scoring.put(new IonizedMolecularFormula(f.getFormula(), f.getIonization()), m.getScore());
        }
        return graph;
    }

    @Override
    public FGraph fillGraph(FGraph graph) {
        //todo adduct-switch: what about this Ionization annotation, here? Useless, since we now can have multiple?
        final FragmentAnnotation<Ionization> ion = graph.getFragmentAnnotationOrThrow(Ionization.class);
        final HashSet<Ionization> allIons = new HashSet<>();

        final PossibleAdductSwitches possibleAdductSwitches = graph.getAnnotationOrThrow(ProcessedInput.class).getAnnotation(PossibleAdductSwitches.class, null);
        if (possibleAdductSwitches==null){
            for (Fragment f : graph.getRoot().getChildren())
                allIons.add(ion.get(f));
        } else {
            for (Fragment f: graph.getFragments()) {
                allIons.addAll(possibleAdductSwitches.getPossibleIonizations(ion.get(f)));
            }
        }

        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final ScoredFormulaMap scoring = graph.getAnnotationOrThrow(ScoredFormulaMap.class);

        final PeakAnnotation<DecompositionList> decompList =
                graph.getAnnotationOrThrow(ProcessedInput.class).getPeakAnnotationOrThrow(DecompositionList.class);


        MolecularFormula pmd;
        {
            final Iterator<Fragment> roots = graph.getFragmentsWithoutRoot().iterator();
            pmd = roots.next().getFormula();
            while (roots.hasNext()) {
                pmd = pmd.union(roots.next().getFormula());
            }
        }

        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(graph.getAnnotationOrThrow(ProcessedInput.class).getMergedPeaks());
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        for (int i = peaks.size() - 1; i >= 0; --i) {
            final ProcessedPeak peak = peaks.get(i);
            final int pi = peak.getIndex();
            for (Decomposition decomposition : decompList.get(peak).getDecompositions()) {
                if (!allIons.contains(decomposition.getIon())) continue;
                final MolecularFormula formula = decomposition.getCandidate();
                final boolean hasEdge = formula.getMass() < pmd.getMass() && pmd.isSubtractable(formula);
                if (hasEdge) {
                    Fragment newFragment = null;
                    for (Fragment f : graph) {
//                        if (f.isRoot() || peakAno.get(f).getIndex() == pi || !ion.get(f).equals(decomposition.getIon())) continue;
                        if (f.isRoot() || peakAno.get(f).getIndex() == pi) continue;
                        if (possibleAdductSwitches==null){
                            if (!ion.get(f).equals(decomposition.getIon())) continue;
                        } else {
                            List<Ionization> allowedSwitches = possibleAdductSwitches.getPossibleIonizations(f.getIonization());
                            if (!allowedSwitches.contains(decomposition.getIon())) continue;
                        }
                        final MolecularFormula fragmentFormula = f.getFormula();
                        assert (peakAno.get(f).getMz() > peak.getMz());
                        if (fragmentFormula.getMass() > formula.getMass() && fragmentFormula.isSubtractable(formula)) {
                            if (newFragment == null) {
                                newFragment = graph.addFragment(decomposition.getCandidate(), decomposition.getIon());
                                ion.set(newFragment, decomposition.getIon());
                                peakAno.set(newFragment, peak);
                                newFragment.setColor(peak.getIndex());
                                scoring.put(new IonizedMolecularFormula(decomposition.getCandidate(), decomposition.getIon()), decomposition.getScore());
                            }
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
        graph.addAnnotation(ProcessedInput.class, input);
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
