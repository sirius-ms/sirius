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

    private static Set<String> model2ids(FeatureFilterModel model) {
        return terms(model).stream().map(FilterTerm::id).collect(java.util.stream.Collectors.toSet());
    }
}
