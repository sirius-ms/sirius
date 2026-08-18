/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Parsing an ion type whose ion mode the table does not know yet registers that ion mode, so every parse can
 * widen what SIRIUS considers a common ion mode. That is wanted when the input is data being imported; it is
 * not wanted when the input is something a user typed into a search box, where a typo would change how every
 * later adduct is read.
 * <p>
 * Parsing without storing gives the same answer without that effect.
 */
public class ParseOnlyIonTypeTest {

    private static List<String> knownIonModes(int charge) {
        List<String> names = new ArrayList<>();
        PeriodicTable.getInstance().getKnownIonModes(charge).forEach(ion -> names.add(ion.toString()));
        return names;
    }

    /**
     * Xe is a real element and no ion mode of it is known, so parsing it is the case that would register one.
     */
    @Test
    public void testParsingWithoutStoringLeavesTheKnownIonModesAlone() throws UnknownElementException {
        List<String> before = knownIonModes(1);

        PrecursorIonType parsed = PeriodicTable.getInstance().ionByName("[M + Xe]+", true);

        assertEquals("[M + Xe]+", parsed.toString());
        assertEquals("Xe", parsed.getIonization().getAtoms().toString());
        assertEquals("parsing must not widen the known ion modes", before, knownIonModes(1));
    }

    /**
     * The same parse, without asking for it to be left alone, still registers - the behaviour everything that
     * imports data relies on.
     */
    @Test
    public void testParsingStoresANewIonModeByDefault() throws UnknownElementException {
        List<String> before = knownIonModes(-1);

        PrecursorIonType parsed = PeriodicTable.getInstance().ionByName("[M + Rn]-");

        assertEquals("[M + Rn]-", parsed.toString());
        assertTrue("a new ion mode must be registered by default",
                knownIonModes(-1).size() > before.size());
        assertTrue(knownIonModes(-1).contains(parsed.getIonization().toString()));
    }

    /**
     * Everything else about the parse is unchanged: the table is still consulted first, the known spellings
     * still resolve, and an adduct whose ion mode is already known parses the same way either way.
     */
    @Test
    public void testParsingWithoutStoringAnswersLikeTheNormalParse() throws UnknownElementException {
        for (String name : List.of("[M+H]+", "M+H", "[M + Na]+", "[M-H]-", "[M+NH4]+", "[M + FA - H]-", "2M+H")) {
            assertEquals("parse-only must answer like the normal parse for " + name,
                    PeriodicTable.getInstance().ionByName(name).toString(),
                    PeriodicTable.getInstance().ionByName(name, true).toString());
        }
    }
}
