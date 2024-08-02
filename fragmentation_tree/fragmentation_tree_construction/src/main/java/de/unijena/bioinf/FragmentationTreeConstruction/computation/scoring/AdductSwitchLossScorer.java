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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import org.jetbrains.annotations.NotNull;

//todo do we also adjust this for M+K ?

/**
 * This LossScorer scores adduct switches, that is parent and child fragment have a different ionization mode.
 * It is strongly related to the {@link de.unijena.bioinf.sirius.plugins.AdductSwitchPlugin} which defines which losses
 * or fragment pairs are forbidden.
 * Furthermore, the {@link AdductSwitchLossScorer} implicitly assumes the also the {@link LossSizeScorer} is used,
 * because it corrects its scores based on the different ionization modes of fragments.
 */
public class AdductSwitchLossScorer implements LossScorer<Object> {

    /**
     * default penalty for adduct switch between fragments
     */
    private static final double DEFAULT_NA_H_SWITCH_SCORE = -3.6109179126442243;

    /**
     * default penalty for child fragments of a fragment produced by adduct switch
     */
    private static final double DEFAULT_NA_H_SWITCH_CHILD_FRAG_SCORE = 0.0;


    private double naHSwitchScore;
    private double naHSwitchChildrenScore;
    private LossSizeScorer lossSizeScorer;

    public AdductSwitchLossScorer(@NotNull LossSizeScorer lossSizeScorer) {
        this(DEFAULT_NA_H_SWITCH_SCORE, DEFAULT_NA_H_SWITCH_CHILD_FRAG_SCORE, lossSizeScorer);
    }

    public AdductSwitchLossScorer(double naHSwitchScore, double naHSwitchChildrenScore, @NotNull LossSizeScorer lossSizeScorer) {
        this.naHSwitchScore = naHSwitchScore;
        this.naHSwitchChildrenScore = naHSwitchChildrenScore;
        this.lossSizeScorer = lossSizeScorer;
        PeriodicTable T = PeriodicTable.getInstance();
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        //extract precursor ionization
        return graph.getRoot().getIonization();
    }

//    private HashSet<MolecularFormula> allowedLosses = new HashSet<>(Arrays.asList(MolecularFormula.parse("C2H2O"), MolecularFormula.parse("CO"), MolecularFormula.parse("C2H4O2"), MolecularFormula.parse("CO2")));

    @Override
    public double score(Loss loss, ProcessedInput input, Object rootIonizationObj) {
        final Ionization rootIon = (Ionization)rootIonizationObj;
        final Ionization sourceIon = loss.getSource().getIonization();
        final Ionization targetIon = loss.getTarget().getIonization();

        if (rootIon.equals(targetIon)) return 0; //no adduct switch has happened.
        {
            //here, we have some kind of adduct switch or a child fragment of an adduct switch fragment
            MolecularFormula F = loss.getFormula();
            if (!loss.isArtificial() && F.isEmpty()) return Double.NEGATIVE_INFINITY;

            if (sourceIon.equals(targetIon)){
                //this is a child fragment of a adduct switch fragment
                return naHSwitchChildrenScore;
            } else {
                //adduct switch is occurring for this specific targetIon

                // first: correct loss size error, because we have different ionizations and this score is based on peak m/z and not formula m/z
                final double wrongLossSize = lossSizeScorer.scoring(input.getMergedPeaks().get(loss.getSource().getPeakId()).getMass() - input.getMergedPeaks().get(loss.getTarget().getPeakId()).getMass());
                final double correctLossSize = lossSizeScorer.score(F);

                final double lossScore = naHSwitchScore;

                return lossScore - wrongLossSize + correctLossSize;
            }
        }
    }

    public AdductSwitchLossScorer replaceScores(double naHSwitchScore, double naHSwitchChildrenScore){
        return new AdductSwitchLossScorer(naHSwitchScore, naHSwitchChildrenScore, lossSizeScorer);
    }

    @Override
    public boolean processArtificialEdges() {
        return true;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.naHSwitchScore = document.getDoubleFromDictionary(dictionary, "na-h-switch-score");
        this.naHSwitchChildrenScore = document.getDoubleFromDictionary(dictionary, "na-h-switch-child-score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "na-h-switch-score", naHSwitchScore);
        document.addToDictionary(dictionary, "na-h-switch-child-score", naHSwitchChildrenScore);
    }
}
