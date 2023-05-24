/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.nitrite.wrappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.FormulaCandidate;

import java.io.IOException;

public class FormulaCandidateDeserializer extends JsonDeserializer<FormulaCandidate> {

    @Override
    public FormulaCandidate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        MolecularFormula formula = null;
        long bitset = 0L;

        JsonToken jsonToken = p.nextToken();
        while (!jsonToken.isStructEnd()) {

            // expect field name
            final String fieldName = p.currentName();
            switch (fieldName) {
                case "formula":
                    formula = MolecularFormula.parseOrThrow(p.nextTextValue());
                    break;
                case "bitset":
                    bitset = p.nextLongValue(0L);
                    break;
            }
            jsonToken = p.nextToken();
        }

        return new FormulaCandidate(formula, PrecursorIonType.unknownPositive(), bitset);
    }

}
