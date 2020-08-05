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

//todo do we also adjust this for M+K ?
public class AdductSwitchLossScorer implements LossScorer<Object> {

    private static final double DEFAULT_NA_H_SWITCH_SCORE = -3.6109179126442243;
    private double naHSwitchScore;
    private LossSizeScorer lossSizeScorer;

    public AdductSwitchLossScorer(LossSizeScorer lossSizeScorer) {
        this(DEFAULT_NA_H_SWITCH_SCORE, lossSizeScorer);
    }

    public AdductSwitchLossScorer(double naHSwitchScore, LossSizeScorer lossSizeScorer) {
        this.naHSwitchScore = naHSwitchScore;
        this.lossSizeScorer = lossSizeScorer;
        PeriodicTable T = PeriodicTable.getInstance();
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;
    }

//    private HashSet<MolecularFormula> allowedLosses = new HashSet<>(Arrays.asList(MolecularFormula.parse("C2H2O"), MolecularFormula.parse("CO"), MolecularFormula.parse("C2H4O2"), MolecularFormula.parse("CO2")));

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final Ionization sourceIon = loss.getSource().getIonization();
        final Ionization targetIon = loss.getTarget().getIonization();

        if (sourceIon.equals(targetIon)) return 0;
        {
            MolecularFormula F = loss.getFormula();
            if (F.isEmpty()) return Double.NEGATIVE_INFINITY;

            // first: correct loss size error
            final double wrongLossSize = lossSizeScorer.scoring(input.getMergedPeaks().get(loss.getSource().getPeakId()).getMass() - input.getMergedPeaks().get(loss.getTarget().getPeakId()).getMass());

            final double correctLossSize = lossSizeScorer.score(F);

            final double lossScore = DEFAULT_NA_H_SWITCH_SCORE;

            return lossScore - wrongLossSize + correctLossSize;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.naHSwitchScore = document.getDoubleFromDictionary(dictionary, "na-h-switch-score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "na-h-switch-score", naHSwitchScore);
    }
}
