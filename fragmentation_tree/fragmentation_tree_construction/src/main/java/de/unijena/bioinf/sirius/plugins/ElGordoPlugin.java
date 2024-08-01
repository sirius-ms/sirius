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

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.ForbidRecalibration;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonFragmentsScore;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.PredefinedPeak;
import de.unijena.bioinf.elgordo.*;
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
        final HashSet<MolecularFormula> commonFragments = new HashSet<>();
        final HashSet<MolecularFormula> commonLosses = new HashSet<>();
        for (LipidClass c : LipidClass.values()) {
            if (c.fragmentLib!=null) {
                for (PrecursorIonType ionType : c.fragmentLib.getDetectableModes()) {
                    final FragmentLib.FragmentSet fragmentSet = c.fragmentLib.getFor(ionType).get();
                    commonFragments.addAll(Arrays.asList(fragmentSet.fragments));
                    commonLosses.addAll(Arrays.asList(fragmentSet.losses));
                }
            }
        }
        final LossSizeScorer lossSizeScorer = FragmentationPatternAnalysis.getByClassName(LossSizeScorer.class, initializer.getAnalysis().getPeakPairScorers());
        final CommonLossEdgeScorer commonLossEdgeScorer = FragmentationPatternAnalysis.getByClassName(CommonLossEdgeScorer.class, initializer.getAnalysis().getLossScorers());
        if (lossSizeScorer!=null && commonLossEdgeScorer!=null) {
            initializer.addLossScorer(new FattyAcidChainScorer(lossSizeScorer));
            for (MolecularFormula f : commonLosses) {
                if (!commonLossEdgeScorer.isCommonLoss(f)) {
                    final double penalty = lossSizeScorer.score(f);
                    if (penalty < 0) {
                        commonLossEdgeScorer.addCommonLoss(f, -penalty * 0.66);
                    }
                }
            }
        }

        final CommonFragmentsScore commonFragmentsScore = FragmentationPatternAnalysis.getByClassName(CommonFragmentsScore.class, initializer.getAnalysis().getDecompositionScorers());
        if (commonFragmentsScore!=null) {
            for (MolecularFormula f : commonFragments) {
                if (commonFragmentsScore.score(f) < 1) {
                    commonFragmentsScore.addCommonFragment(f, 1);
                }
            }
        }
    }
    @Called("FattyAcidChainScore")
    public class FattyAcidChainScorer implements LossScorer<Object> {
        LossSizeScorer lossSizeScorer;

        public FattyAcidChainScorer(LossSizeScorer lossSizeScorer) {
            this.lossSizeScorer = lossSizeScorer;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
            return null;
        }

        @Override
        public double score(Loss loss, ProcessedInput input, Object precomputed) {
            final Optional<LipidChain> c = LipidChain.fromFormula(loss.getFormula());
            if (c.isPresent()) {
                final int chainLength = c.get().getChainLength();
                if (chainLength < 6 || chainLength > 36) return 0d;
                final int doubleBonds = c.get().getNumberOfDoubleBonds();
                if (doubleBonds > 6) return 0d;
                final double penalty = lossSizeScorer.score(loss.getFormula());
                if (penalty < 0) {
                    double score = -penalty * Math.pow(0.95, doubleBonds*doubleBonds)*0.5;
                    return score;
                }
            }
            return 0d;
        }
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
        if (input.getAnnotationOrThrow(Whiteset.class).isFinalized()) return;
        final List<MassToLipid.LipidCandidate> lipidCandidates = new MassToLipid(input.getAnnotation(MS1MassDeviation.class).map(x->x.allowedMassDeviation).orElseGet(()->new Deviation(20)), input.getExperimentInformation().getPrecursorIonType().getCharge()).analyzePrecursor(input.getExperimentInformation().getIonMass()); //todo ElementFiter: this use of "or Else use 20 pp" clashes with whiteset.resolve. Should we expect given Dev values everywhere at this stage or set some default earlier?
        final Deviation ms2dev = input.getAnnotation(MS2MassDeviation.class).map(x -> x.allowedMassDeviation).orElseGet(() -> new Deviation(20));
        final MassToLipid m2l = new MassToLipid(ms2dev, input.getExperimentInformation().getPrecursorIonType().getCharge());
        final Spectrum<ProcessedPeak> peaklist = Spectrums.wrap(input.getMergedPeaks());
        final SimpleSpectrum ms2 = new SimpleSpectrum(peaklist);
        final Optional<AnnotatedLipidSpectrum<SimpleSpectrum>> annotated = lipidCandidates.stream().map(x -> m2l.annotateSpectrum(x, ms2)).filter(Objects::nonNull).max(Comparator.naturalOrder());
        if (annotated.isPresent() && annotated.get().getDetectionLevel().isFormulaSpecified()) {
            // specify molecular formula
            final AnnotatedLipidSpectrum<SimpleSpectrum> ano = annotated.get();
            final LipidSpecies species = ano.getAnnotatedSpecies();
            Whiteset whiteset = input.getAnnotation(Whiteset.class, Whiteset::empty);
            boolean enforceElGordo = input.getAnnotation(EnforceElGordoFormula.class, () -> EnforceElGordoFormula.newInstance(true)).value;
            if (!whiteset.isEmpty()) {
                if (whiteset.getNeutralFormulas().contains(ano.getFormula()) || whiteset.getMeasuredFormulas().contains(ano.getIonType().neutralMoleculeToMeasuredNeutralMolecule(ano.getFormula()))) {
                    if (enforceElGordo) {
                        //enforce using only the ElGordo formula
                        whiteset = whiteset.filterByNeutralFormulas(Collections.singleton(ano.getFormula()), input.getAnnotationOrThrow(PossibleAdducts.class).getAdducts(), ElGordoPlugin.class).setFinalized(true); //todo ElementFilter: test
                        //alternatively a similar behaviour could be established just generating a new whiteset, but this "forgets" previous provider classes
//                        whiteset = Whiteset.ofNeutralizedFormulas(Arrays.asList(ano.getFormula()), ElGordoPlugin.class);
                    } else {
                        whiteset = whiteset.addNeutral(Collections.singleton(ano.getFormula()), ElGordoPlugin.class);
                    }
                } else {
                    //todo ElementFilter: is this a warning? can happen for certain parameter combinations, e.g. for searching small database with formulas
                    LoggerFactory.getLogger(ElGordoPlugin.class).warn(input.getExperimentInformation().getName() + " is estimated to be " + species.toString() + " but the corresponding molecular formula " + ano.getFormula() + " is not part of the candidate set. Compound name = " + input.getExperimentInformation().getName());
                    whiteset = whiteset.addNeutral(Collections.singleton(ano.getFormula()), ElGordoPlugin.class);
                }
            } else {
                whiteset = Whiteset.ofNeutralizedFormulas(Arrays.asList(ano.getFormula()), ElGordoPlugin.class).setFinalized(enforceElGordo);
            }
            input.setAnnotation(Whiteset.class, whiteset);
            if (input.getAnnotation(EnforceElGordoFormula.class, () -> EnforceElGordoFormula.newInstance(true)).value) {
                input.setAnnotation(PossibleAdducts.class, new PossibleAdducts(ano.getIonType())); //should have no real influence on current pipeline, since the enfored formula also implicitely specifies the adduct
//                input.getOrCreatePeakAnnotation(DecompositionList.class).set(input.getParentPeak(), DecompositionList.fromFormulas(Arrays.asList(ano.getIonType().neutralMoleculeToMeasuredNeutralMolecule(ano.getFormula())), ano.getIonType().getIonization())); //todo ElementFiler: not used in FPA. Info overriden in FPA.performDecomposition()
            }

            // pre-annotate spectrum
            if (annotated.get().getDetectionLevel().arePeaksCorrectlyAnnotated()){
                input.setAnnotation(LipidSpecies.class, species);
                PeakAnnotation<PredefinedPeak> pa = input.getOrCreatePeakAnnotation(PredefinedPeak.class);
                input.setAnnotation(LipidSpecies.class, species);
                input.setAnnotation(ForbidRecalibration.class, ForbidRecalibration.FORBIDDEN);
                final TIntObjectHashMap<PredefinedPeak> elgordoScoreMap = new TIntObjectHashMap<>();
                for (int k = 0, n = ano.numberOfAnnotatedPeaks(); k < n; ++k) {
                    final LipidAnnotation annotation = ano.getAnnotationAt(k);
                    final int j = ano.getPeakIndexAt(k);
                    if (j < 0) continue;
                    final int i = Spectrums.mostIntensivePeakWithin(peaklist, ms2.getMzAt(j), ms2dev);
                    if (i >= 0) {
                        final MolecularFormula formula = annotation.getMeasuredPeakFormula();
                        ProcessedPeak pk = input.getMergedPeaks().get(i);
                        final PredefinedPeak obj = new PredefinedPeak(formula, annotation.getIonType(), "el Gordo: " + annotation.toString());
                        pa.set(pk, obj);
                        elgordoScoreMap.put(pk.getIndex(), obj);
                    }
                }
            } else {
                input.setAnnotation(LipidSpecies.class, species.makeGeneric());
            }
        }

    }

}
