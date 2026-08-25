/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the compact/extensive field-name display shared by chips and the autocomplete list.
 */
public class FieldDisplayTest {

    @Test
    public void testExtensiveIsTheFullyQualifiedName() {
        String full = "topAnnotations.formulaAnnotation.lipidAnnotation.lipid";
        assertEquals(full, FieldDisplay.of(full, FieldDisplay.Mode.EXTENSIVE));
    }

    @Test
    public void testCompactKeepsOnlyTheTerminalStructuralSegment() {
        assertEquals("lipid", FieldDisplay.compact("topAnnotations.formulaAnnotation.lipidAnnotation.lipid"));
        assertEquals("structureName", FieldDisplay.compact("topAnnotations.structureName"));
        assertEquals("confidenceExactMatch", FieldDisplay.compact("topAnnotations.confidenceExactMatch"));
        assertEquals("ionMass", FieldDisplay.compact("ionMass"));
    }

    @Test
    public void testCompactKeepsFieldPlusKeyForDynamicMapFields() {
        assertEquals("matchedDatabases.GNPS", FieldDisplay.compact("topAnnotations.matchedDatabases.GNPS"));
        assertEquals("qualities.PEAK_QUALITY", FieldDisplay.compact("qualities.PEAK_QUALITY"));
        assertEquals("tags.pfas", FieldDisplay.compact("tags.pfas"));
        assertEquals("molecularFormula.C", FieldDisplay.compact("topAnnotations.formulaAnnotation.molecularFormula.C"));
    }

    @Test
    public void testBlankNameIsReturnedAsIs() {
        assertEquals("", FieldDisplay.compact(""));
    }

    @Test
    public void testCompactBySignificantSuffixLengthKeepsLastNSegments() {
        assertEquals("matchedDatabases.GNPS", FieldDisplay.compact("topAnnotations.matchedDatabases.GNPS", 2));
        assertEquals("confidenceExactMatch", FieldDisplay.compact("topAnnotations.confidenceExactMatch", 1));
        assertEquals("foldChange.SAMPLE.BLANK.APEX_INTENSITY.AVG",
                FieldDisplay.compact("stats.foldChange.SAMPLE.BLANK.APEX_INTENSITY.AVG", 5));
    }

    @Test
    public void testSuffixLengthIsClampedToAvailableSegments() {
        assertEquals("ionMass", FieldDisplay.compact("ionMass", 5));      // only one segment available
        assertEquals("lipid", FieldDisplay.compact("a.b.lipid", 0));       // never fewer than one segment
    }

    @Test
    public void testOfWithSuffixLengthHonorsMode() {
        String full = "topAnnotations.matchedDatabases.GNPS";
        assertEquals(full, FieldDisplay.of(full, FieldDisplay.Mode.EXTENSIVE, 2));
        assertEquals("matchedDatabases.GNPS", FieldDisplay.of(full, FieldDisplay.Mode.COMPACT, 2));
    }
}
