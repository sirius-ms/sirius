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

import javax.swing.JLabel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the read-only {@link QueryNodeRenderer}: one chip per node, human-readable label, full
 * compiled query as tooltip, groups collapsed to their compiled form. (Constructing Swing components
 * needs no display, so these run headless.)
 */
public class QueryNodeRendererTest {

    private static String labelText(ChipComponent chip) {
        return ((JLabel) chip.getComponent(0)).getText();
    }

    @Test
    public void testNumericClauseRendersFieldOpValueWithCompiledTooltip() {
        QueryClause mz = QueryClause.numeric("ionMass", NumberOp.RANGE_INCLUSIVE, "300", "400", false);
        ChipComponent chip = QueryNodeRenderer.chip(mz, ChipComponent.Style.USER, null);

        assertEquals("ionMass [300 TO 400]", labelText(chip));
        assertEquals("ionMass:[300 TO 400]", chip.getToolTipText());
    }

    @Test
    public void testNegatedClauseGetsNotPrefixInLabelAndTooltip() {
        QueryClause msms = QueryClause.text("hasMsMs", "true", true);
        ChipComponent chip = QueryNodeRenderer.chip(msms, ChipComponent.Style.MODEL, null);

        assertTrue(labelText(chip).startsWith("NOT hasMsMs"), labelText(chip));
        assertEquals("NOT hasMsMs:true", chip.getToolTipText());
    }

    @Test
    public void testFreeTextClauseRendersQuoted() {
        ChipComponent chip = QueryNodeRenderer.chip(QueryClause.freeText("caffeine", false), ChipComponent.Style.USER, null);

        assertEquals("“caffeine”", labelText(chip));
        assertEquals("Full-text search in the default fields", chip.getToolTipText());
    }

    @Test
    public void testGroupRendersAsParenthesizedReadableFormWithCompiledTooltip() {
        QueryClause a = QueryClause.text("detectedAdducts", "[M+H]+", false);
        QueryClause b = QueryClause.text("detectedAdducts", "[M+Na]+", false);
        QueryGroup group = new QueryGroup(QueryNode.nextId("group"), false, List.of(a, b), List.of(LogicOp.OR));

        ChipComponent chip = QueryNodeRenderer.chip(group, ChipComponent.Style.MODEL, null);
        assertTrue(labelText(chip).startsWith("(") && labelText(chip).contains(" OR "), labelText(chip));
        assertEquals(LuceneQueryCompiler.render(group), chip.getToolTipText(), "tooltip stays fully-qualified lucene");
    }

    @Test
    public void testCompactModeShortensFieldNamesInTheLabel() {
        QueryClause db = QueryClause.numeric("topAnnotations.matchedDatabases.GNPS", NumberOp.RANGE_INCLUSIVE, "1", "5", false);
        // suffix length 2 -> keep "matchedDatabases.GNPS"
        ChipComponent compact = QueryNodeRenderer.chip(db, ChipComponent.Style.MODEL, null,
                FieldDisplay.Mode.COMPACT, field -> 2);
        assertTrue(labelText(compact).startsWith("matchedDatabases.GNPS "), labelText(compact));
        // tooltip keeps the fully-qualified field
        assertTrue(compact.getToolTipText().startsWith("topAnnotations.matchedDatabases.GNPS"), compact.getToolTipText());

        ChipComponent extensive = QueryNodeRenderer.chip(db, ChipComponent.Style.MODEL, null,
                FieldDisplay.Mode.EXTENSIVE, field -> 2);
        assertTrue(labelText(extensive).startsWith("topAnnotations.matchedDatabases.GNPS "), labelText(extensive));
    }
}
