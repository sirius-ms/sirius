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
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.FilterSession;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter;
import de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory;
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the AlignedFeature {@link FilterTerm} provider: active facets become PANEL terms carrying the
 * facet's query node, and {@link FilterTerm#remove()} resets exactly that facet in the backing model.
 */
public class PanelFilterTermsTest {

    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset();
        return model;
    }

    private static List<FilterTerm> terms(FeatureFilterModel model) {
        return PanelFilterTerms.of(model, ConfidenceDisplayMode.EXACT);
    }

    private static FilterTerm byId(List<FilterTerm> terms, String id) {
        return terms.stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("no term with id " + id + " in " + terms.stream().map(FilterTerm::id).toList()));
    }

    @Test
    public void testActiveFacetsBecomePanelTermsWithNodes() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+")));

        List<FilterTerm> terms = terms(model);
        assertEquals(Set.of("mz", "adducts"), terms.stream().map(FilterTerm::id).collect(java.util.stream.Collectors.toSet()));
        terms.forEach(t -> assertEquals(Provenance.PANEL, t.provenance()));

        // the mz term carries the real-field node
        assertEquals("ionMass", ((QueryClause) byId(terms, "mz").toQueryNode()).field());
    }

    @Test
    public void testRemoveResetsExactlyThatFacet() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        model.setHasMs1(true);
        assertEquals(Set.of("mz", "hasMs1"), model2ids(model));

        byId(terms(model), "mz").remove();

        assertFalse(model.isMzFilterActive(), "removing the mz term must reset the mz filter");
        assertTrue(model.isHasMs1(), "and must leave the other facet untouched");
        assertEquals(Set.of("hasMs1"), model2ids(model));
    }

    @Test
    public void testInactiveModelHasNoTerms() {
        assertTrue(terms(cleanSlate()).isEmpty());
    }

    @Test
    public void testNumericFacetsExposeAnInlineRangeEditThatAppliesToTheModel() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        model.setCurrentMaxMz(400);

        de.unijena.bioinf.ms.gui.utils.query.RangeEdit range = byId(terms(model), "mz").rangeEdit();
        assertNotNull(range, "m/z is inline-editable");
        assertEquals(300.0, range.currentMin());
        assertEquals(400.0, range.currentMax());
        assertEquals(model.getMinMz(), range.lowerBound());
        assertEquals(model.getMaxMz(), range.upperBound());

        // applying the setter mutates exactly this facet's bounds on the backing model
        range.setter().accept(325.0, 375.0);
        assertEquals(325.0, model.getCurrentMinMz());
        assertEquals(375.0, model.getCurrentMaxMz());
    }

    @Test
    public void testSetAndBooleanFacetsHaveNoInlineRangeEdit() {
        FeatureFilterModel model = cleanSlate();
        model.setHasMs1(true);
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+")));
        assertNull(byId(terms(model), "hasMs1").rangeEdit(), "a boolean facet is not range-editable");
        assertNull(byId(terms(model), "adducts").rangeEdit(), "a set facet is edited via the dialog, not inline");
    }

    /**
     * A facet's reset is an operation on the VALUE, so it resets the category on whatever filter it is applied
     * to - not on the model the facet was read from (the old closure-over-the-filter bug).
     */
    @Test
    public void testCategorizedQualityResetAppliesToTheGivenFilter() {
        FeatureFilterModel source = cleanSlate();
        source.getPeakShapeQualityFilter().removeQuality(DataQuality.LOWEST);
        FilterTerm term = byId(terms(source), "quality." + DefaultQualityCategory.PEAK_QUALITY.name());

        FeatureFilterModel other = cleanSlate();
        other.getPeakShapeQualityFilter().removeQuality(DataQuality.BAD);
        FeatureFilter reset = PanelQueryNodeFactory.facets(other, ConfidenceDisplayMode.EXACT).stream()
                .filter(f -> f.id().equals(term.id())).findFirst().orElseThrow()
                .reset().apply(other.get());

        assertEquals(FeatureFilter.ALL_QUALITIES, reset.categorizedQuality().get(DefaultQualityCategory.PEAK_QUALITY),
                "the reset opens the category on the filter it is applied to");
        assertTrue(source.getPeakShapeQualityFilter().isEnabled(), "and leaves the source model alone");
        term.remove();
        assertFalse(source.getPeakShapeQualityFilter().isEnabled(), "remove() applies it to the term's own model");
    }

    /** A session-bound term stages its removal on the working value; the applied filter waits for commit. */
    @Test
    public void testSessionBoundTermsStageTheirRemoval() {
        FeatureFilterModel model = cleanSlate();
        model.setCurrentMinMz(300);
        FilterSession session = new FilterSession(model);

        byId(PanelFilterTerms.of(session, ConfidenceDisplayMode.EXACT), "mz").remove();

        assertFalse(session.working().isMzFilterActive(session.context()), "removed from the working value");
        assertTrue(model.isMzFilterActive(), "the applied filter is untouched until commit");
        assertTrue(PanelFilterTerms.of(session, ConfidenceDisplayMode.EXACT).isEmpty());
        session.commit();
        assertFalse(model.isMzFilterActive());
    }

    private static Set<String> model2ids(FeatureFilterModel model) {
        return terms(model).stream().map(FilterTerm::id).collect(java.util.stream.Collectors.toSet());
    }
}
