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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.IOException;

@Deprecated
public class BayesianNetworkFromDatabaseProvider implements BayesianNetworkScoringProvider {

    @Override
    public BayesnetScoring getScoringOrNull(MolecularFormula formula) throws IOException {
        return null;
    }

    @Override
    public BayesnetScoring getScoringOrDefault(MolecularFormula formula) throws IOException {
        return null;
    }

    @Override
    public void storeScoring(MolecularFormula formula, BayesnetScoring scoring, boolean override) {

    }

    @Override
    public BayesnetScoring getDefaultScoring() throws IOException {
        return null;
    }

    @Override
    public boolean isDefaultScoring(BayesnetScoring scoring) {
        return false;
    }

    @Override
    public void storeDefaultScoring(BayesnetScoring scoring, boolean override) throws IOException {

    }
}
