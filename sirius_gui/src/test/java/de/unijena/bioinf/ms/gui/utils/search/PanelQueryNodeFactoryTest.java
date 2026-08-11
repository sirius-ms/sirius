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
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link PanelQueryNodeFactory}: active filter-panel facets render as query nodes with the real
 * lucene field names and the right structural shape (scalar range, boolean term, OR/AND groups,
 * negated clause). Also cross-checks that every field name used occurs in the model's compiled query,
 * so the hand-written mapping cannot silently drift from {@code FeatureFilterModel.toLuceneQueryBuilder}.
 */
public class PanelQueryNodeFactoryTest {

    /** Neutralizes the default-active filters (MS/MS present, feature quality) for a clean slate. */
    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset();
        return model;
    }

    private static List<QueryNode> nodes(FeatureFilterModel model) {
        return PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT);
    }

    private static QueryClause onlyClause(List<QueryNode> nodes) {
        assertEquals(1, nodes.size());
        assertInstanceOf(QueryClause.class, nodes.get(0));
        return (QueryClause) nodes.get(0);
    }

    private static QueryGroup onlyGroup(List<QueryNode> nodes) {
        assertEquals(1, nodes.size());
        assertInstanceOf(QueryGroup.class, nodes.get(0));
        return (QueryGroup) nodes.get(0);
    }

    @Test
    public void testMzRendersAsRealFieldRange() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300); // interior lower; upper stays at absolute max (5000) -> open

        QueryClause mz = onlyClause(nodes(model));
        assertEquals("ionMass", mz.field());
        assertEquals(NumberOp.RANGE_INCLUSIVE, mz.op());
        assertEquals("300", mz.value1());
        assertEquals("5000", mz.value2(), "faithful to the model: the concrete (absolute-max) upper bound");
        assertFalse(mz.negated());
    }

    @Test
    public void testRtRendersAsOrGroupOverTheThreeRtFields() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinRt(10);

        QueryGroup rt = onlyGroup(nodes(model));
        assertEquals(3, rt.items().size());
        assertEquals(List.of(LogicOp.OR, LogicOp.OR), rt.logics());
        List<String> fields = rt.items().stream().map(n -> ((QueryClause) n).field()).toList();
        assertEquals(List.of("rtStartSeconds", "rtApexSeconds", "rtEndSeconds"), fields);
    }

    @Test
    public void testConfidenceFieldFollowsTheDisplayMode() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinConfidence(0.5);

        assertEquals("topAnnotations.confidenceExactMatch",
                ((QueryClause) PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT).get(0)).field());
        assertEquals("topAnnotations.confidenceApproxMatch",
                ((QueryClause) PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.APPROXIMATE).get(0)).field());
    }

    @Test
    public void testHasMsMsRendersAsBooleanTerm() {
        FeatureFilterModel model = cleanSlate();
        model.setHasMsMs(true);

        QueryClause msms = onlyClause(nodes(model));
        assertEquals("hasMsMs", msms.field());
        assertNull(msms.op(), "a boolean term has no numeric operator");
        assertEquals("true", msms.value1());
    }

    @Test
    public void testAdductsRenderAsOrGroupOfRealFieldClauses() {
        FeatureFilterModel model = cleanSlate();
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+")));

        QueryGroup group = onlyGroup(nodes(model));
        assertEquals(2, group.items().size());
        assertEquals(List.of(LogicOp.OR), group.logics(), "the selected adducts are alternatives (OR)");
        group.items().forEach(item -> assertEquals("detectedAdducts", ((QueryClause) item).field()));
    }

    @Test
    public void testOverallQualityRendersAsQualityFieldGroup() {
        // fresh model WITHOUT resetting the feature quality filter -> it is enabled by default
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);

        List<QueryNode> nodes = nodes(model);
        assertEquals(1, nodes.size());
        List<String> fields = new ArrayList<>();
        collectFields(nodes.get(0), fields);
        assertFalse(fields.isEmpty());
        fields.forEach(f -> assertEquals("quality", f, "overall quality clauses use the 'quality' field"));
    }

    @Test
    public void testElementsRenderAsAndGroupOfPerElementRanges() {
        FeatureFilterModel model = cleanSlate();
        model.setElementFilter(new ElementFilter("CHNOPS"));

        QueryGroup group = onlyGroup(nodes(model));
        assertEquals(List.of(LogicOp.AND), group.logics().stream().distinct().toList(),
                "element constraints must all hold (AND)");
        group.items().forEach(item -> {
            QueryClause c = (QueryClause) item;
            assertTrue(c.field().startsWith("topAnnotations.formulaAnnotation.molecularFormula."), c.field());
            assertEquals(NumberOp.RANGE_INCLUSIVE, c.op());
        });
    }

    @Test
    public void testDbHitRendersWithCandidateCountRange() {
        FeatureFilterModel model = cleanSlate();
        model.setDbFilter(new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem")), 5));

        // a single DB collapses the OR group to a plain clause
        QueryClause db = onlyClause(nodes(model));
        assertEquals("topAnnotations.matchedDatabases.PubChem", db.field());
        assertEquals(NumberOp.RANGE_INCLUSIVE, db.op());
        assertEquals("1", db.value1());
        assertEquals("5", db.value2());
    }

    @Test
    public void testBlankFoldChangeRendersAsOpenEndedRange() {
        FeatureFilterModel model = cleanSlate();
        model.getSampleBlankFoldChange().setEnabled(true); // default currentMinFoldChange = 2.0

        QueryClause blank = onlyClause(nodes(model));
        assertEquals(FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME, blank.field());
        assertEquals("2", blank.value1());
        assertEquals("", blank.value2(), "fold change is a lower-bounded, open-ended range");
    }

    @Test
    public void testNoLipidIsNegatedClauseAndAnyLipidIsPlain() {
        FeatureFilterModel noLipid = cleanSlate();
        noLipid.setLipidFilter(FeatureFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED);
        assertTrue(onlyClause(nodes(noLipid)).negated(), "'no lipids' must be a NOT clause");

        FeatureFilterModel anyLipid = cleanSlate();
        anyLipid.setLipidFilter(FeatureFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED);
        assertFalse(onlyClause(nodes(anyLipid)).negated(), "'any lipid' must be a plain clause");
    }

    @Test
    public void testInactiveModelYieldsNoNodes() {
        assertTrue(nodes(cleanSlate()).isEmpty());
    }

    @Test
    public void testUsedFieldNamesOccurInTheModelsCompiledQuery() {
        // activate a broad set of facets together (feature quality stays enabled by default)
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.setCurrentMinMz(300);
        model.setCurrentMinRt(10);
        model.setCurrentMinConfidence(0.5);
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+")));
        model.setElementFilter(new ElementFilter("CHNOPS"));
        model.setDbFilter(new DbFilter(List.of(new SearchableDatabase().databaseId("PubChem")), 5));
        model.getSampleBlankFoldChange().setEnabled(true);
        model.setLipidFilter(FeatureFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED);

        String compiled = model.toLuceneQuery(ConfidenceDisplayMode.EXACT)
                .orElseThrow(() -> new AssertionError("model with active filters must compile to a query"));

        List<String> usedFields = new ArrayList<>();
        PanelQueryNodeFactory.nodesFor(model, ConfidenceDisplayMode.EXACT).forEach(node -> collectFields(node, usedFields));
        assertFalse(usedFields.isEmpty());
        usedFields.forEach(field -> assertTrue(compiled.contains(field),
                "field '" + field + "' used by a chip is not in the model's compiled query: " + compiled));
    }

    private static void collectFields(QueryNode node, List<String> out) {
        if (node instanceof QueryClause clause) {
            if (!clause.field().isEmpty())
                out.add(clause.field());
        } else if (node instanceof QueryGroup group) {
            group.items().forEach(item -> collectFields(item, out));
        }
    }
}
