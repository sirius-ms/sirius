

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

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

public class Smiles implements Ms2ExperimentAnnotation {

    public final String smiles;

    public Smiles(String smiles) {
        this.smiles = smiles;
    }

    @Override
    public String toString() {
        return smiles;
    }

    public boolean isConnected() {
        return SmilesU.isConnected(smiles);
    }

    public int getFormalCharge() {
        return SmilesU.getFormalChargeFromSmiles(smiles);
    }

    public boolean isMultipleCharged() {
        return SmilesU.isMultipleCharged(smiles);
    }

    public int getNumberOfPartialCharges() {
        return SmilesU.getNumberOfPartialChargesFromSmiles(smiles);
    }

    /**
     * @return 2d smiles created by regex Text replace.
     */
    public Smiles get2DSmiles() {
        return new Smiles(SmilesU.get2DSmilesByTextReplace(smiles));
    }

    public Smiles stripStereoCentres() {
        return new Smiles(SmilesU.stripStereoCentres(smiles));
    }

    public Smiles stripDoubleBondGeometry() {
        return new Smiles(SmilesU.stripDoubleBondGeometry(smiles));
    }
}
