/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IntergraphMapping;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.model.PredefinedPeak;
import de.unijena.bioinf.elgordo.AnnotatedLipidSpectrum;
import de.unijena.bioinf.elgordo.LipidAnnotation;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.elgordo.MassToLipid;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ElGordoPlugin extends SiriusPlugin  {

    @Override
    public void initializePlugin(PluginInitializer initializer) {

    }

    @Override
    protected void transferAnotationsFromInputToGraph(ProcessedInput input, FGraph graph) {
        input.getAnnotation(LipidSpecies.class).ifPresent(species->graph.setAnnotation(LipidSpecies.class, species));
    }

    @Override
    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {
        graph.getAnnotation(LipidSpecies.class).ifPresent(species->tree.setAnnotation(LipidSpecies.class, species));
    }

    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        super.beforeDecomposing(input);
        final List<MassToLipid.LipidCandidate> lipidCandidates = new MassToLipid(input.getAnnotation(MS1MassDeviation.class).map(x->x.allowedMassDeviation).orElseGet(()->new Deviation(20))).analyzePrecursor(input.getExperimentInformation().getIonMass());
        final Deviation ms2dev = input.getAnnotation(MS2MassDeviation.class).map(x -> x.allowedMassDeviation).orElseGet(() -> new Deviation(20));
        final MassToLipid m2l = new MassToLipid(ms2dev);
        final Spectrum<ProcessedPeak> peaklist = Spectrums.wrap(input.getMergedPeaks());
        final SimpleSpectrum ms2 = new SimpleSpectrum(peaklist);
        final Optional<AnnotatedLipidSpectrum<SimpleSpectrum>> annotated = lipidCandidates.stream().map(x -> m2l.annotateSpectrum(x, ms2)).filter(Objects::nonNull).max(Comparator.naturalOrder());
        if (annotated.isPresent() && annotated.get().predictIsALipid()) {
            // specify molecular formula

            final AnnotatedLipidSpectrum<SimpleSpectrum> ano = annotated.get();
            final LipidSpecies species = ano.getAnnotatedSpecies();
            Whiteset whiteset = input.getAnnotation(Whiteset.class, Whiteset::empty);
            if (!whiteset.isEmpty()) {
                if (whiteset.getNeutralFormulas().contains(ano.getFormula()) || whiteset.getMeasuredFormulas().contains(ano.getIonType().neutralMoleculeToMeasuredNeutralMolecule(ano.getFormula()))) {
                 whiteset = Whiteset.ofNeutralizedFormulas(Arrays.asList(ano.getFormula()));
                } else {
                    LoggerFactory.getLogger(ElGordoPlugin.class).warn(input.getExperimentInformation().getSourceString() + " is estimated to be " + species.toString() + " but the corresponding molecular formula " + ano.getFormula() + " is not part of the candidate set.");
                    return;
                }
            } else {
                whiteset = Whiteset.ofNeutralizedFormulas(Arrays.asList(ano.getFormula()));
            }
            input.setAnnotation(Whiteset.class, whiteset);
            input.getOrCreatePeakAnnotation(DecompositionList.class).set(input.getParentPeak(), DecompositionList.fromFormulas(Arrays.asList(ano.getIonType().neutralMoleculeToMeasuredNeutralMolecule(ano.getFormula())), ano.getIonType().getIonization()));
            PeakAnnotation<PredefinedPeak> pa = input.getOrCreatePeakAnnotation(PredefinedPeak.class);
            input.setAnnotation(LipidSpecies.class, species);
            final TIntObjectHashMap<PredefinedPeak> elgordoScoreMap = new TIntObjectHashMap<>();
            for (int k=0, n=ano.numberOfAnnotatedPeaks(); k < n; ++k) {
                final LipidAnnotation annotation = ano.getAnnotationAt(k);
                final int i = Spectrums.mostIntensivePeakWithin(peaklist, ms2.getMzAt(ano.getPeakIndexAt(k)), ms2dev);
                if (i>=0) {
                    final MolecularFormula formula = annotation.getTarget()== LipidAnnotation.Target.LOSS ? ano.getFormula().subtract(annotation.getFormula()) : annotation.getFormula();
                    ProcessedPeak pk = input.getMergedPeaks().get(i);
                    final PredefinedPeak obj = new PredefinedPeak(formula, annotation.getIonType(), "el Gordo: " + annotation.toString());
                    pa.set(pk, obj);
                    elgordoScoreMap.put(pk.getIndex(), obj);
                }
            }
        }

    }

}
