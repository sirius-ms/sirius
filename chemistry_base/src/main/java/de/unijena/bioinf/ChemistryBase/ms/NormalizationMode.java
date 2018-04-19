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
package de.unijena.bioinf.ChemistryBase.ms;


import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public enum NormalizationMode implements ImmutableParameterized {
    SUM, MAX,FIRST;

    @Override
    public Object readFromParameters(ParameterHelper helper, DataDocument document, Object dictionary) {
        String mode = document.getStringFromDictionary(dictionary, "mode");
        return NormalizationMode.valueOf(mode);
    }

    @Override
    public void exportParameters(ParameterHelper helper, DataDocument document, Object dictionary) {
        document.addToDictionary(dictionary, "mode", toString());
    }
}
