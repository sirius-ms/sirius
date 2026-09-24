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

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A bare number in a query finds the feature whose m/z it means (GitHub issue sirius-ms/sirius#325): the term
 * is searched as a window on the NUMERIC default search fields, on top of the text ones. The window is half a
 * unit of the typed precision and at least 10 ppm wide; fielded terms, quoted terms and text are untouched, and a
 * text term does not reach the numeric field at all (it used to fail the whole query).
 */
class NumericFreeTextSearchTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feature {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
        @IndexField(name = "ionMass", sortable = true, defaultSearchField = true)
        public Double ionMass;
        @IndexField(name = "rt")
        public Double rt; // numeric, but not a default search field
    }

    private static SinglePojoLuceneIndexManager<Feature> index() {
        SinglePojoLuceneIndexManager<Feature> index = new SinglePojoLuceneIndexManager<>(
                new ByteBuffersDirectory(), Feature.class, null, name -> ValueType.TEXT);
        index.addDocument(new Feature("exact", "caffeine", 301.1421, 12.0));
        index.addDocument(new Feature("close", "theophylline", 301.1489, 13.0));
        index.addDocument(new Feature("half", "half up", 301.5, 14.0));
        index.addDocument(new Feature("far", "something else", 405.2, 15.0));
        index.addDocument(new Feature("named", "sample 512", 500.0, 301.14)); // a number in the NAME, 301.14 as RT
        return index;
    }

    private static Set<String> ids(SinglePojoLuceneIndexManager<Feature> index, String query) {
        return index.search(query, Pageable.unpaged()).getContent().stream().map(f -> f.id).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("two decimals: half a unit of the last decimal, so 301.14 finds 301.1421 and not 301.1489")
    void twoDecimalsMeanPlusMinusFiveThousandths() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of("exact"), ids(index, "301.14"));
        }
    }

    @Test
    @DisplayName("an integer means the whole unit: 301 finds every m/z in [300.5, 301.5]")
    void anIntegerMeansTheWholeUnit() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of("exact", "close", "half"), ids(index, "301"));
        }
    }

    @Test
    @DisplayName("the default-field text search of a bare number stays: a number in a name is still found")
    void theTextSearchOfTheNumberStays() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of("named"), ids(index, "512"), "no m/z near 512; the name has it");
        }
    }

    @Test
    @DisplayName("many decimals: the ppm floor keeps the window as wide as an instrument is precise")
    void manyDecimalsFallBackToThePpmFloor() throws Exception {
        try (var index = index()) {
            // half a unit of the 4th decimal would be 0.00005 (0.2 ppm); the 10 ppm floor is 0.003 at m/z 301
            assertEquals(Set.of("exact"), ids(index, "301.1421"));
            assertEquals(Set.of("exact"), ids(index, "301.1440"), "0.0019 off, inside 10 ppm");
            assertEquals(Set.of(), ids(index, "301.1530"), "0.0041 off the closest m/z, outside 10 ppm");
        }
    }

    @Test
    @DisplayName("the window is on the numeric default search fields, not on every numeric field")
    void onlyDefaultSearchFieldsTakePart() throws Exception {
        try (var index = index()) {
            assertFalse(ids(index, "13").contains("close"), "rt 13.0 is numeric but not a default search field");
            assertEquals(Set.of(), ids(index, "13"));
        }
    }

    @Test
    @DisplayName("a text term does not reach the numeric default field, so text search keeps working")
    void textTermsDoNotBreakOnTheNumericDefaultField() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of("exact"), ids(index, "caffeine"), "a term a point field cannot parse must not fail the query");
            assertEquals(Set.of("exact"), ids(index, "caff*"));
        }
    }

    @Test
    @DisplayName("fielded, quoted and textual terms are untouched")
    void onlyBareNumbersAreWindowed() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of(), ids(index, "name:301"), "a fielded term searches that field only - no m/z window");
            assertEquals(Set.of("named"), ids(index, "\"512\""), "a quoted term is text");
            assertEquals(Set.of("exact"), ids(index, "caffeine"));
            assertEquals(Set.of("exact"), ids(index, "ionMass:[301.1 TO 301.145]"), "an explicit range is the precise tool and stays exact");
        }
    }

    @Test
    @DisplayName("bare numbers combine with the rest of the query like any other term")
    void combinesWithOtherTerms() throws Exception {
        try (var index = index()) {
            assertEquals(Set.of("exact"), ids(index, "301.14 AND caffeine"));
            assertEquals(Set.of("exact", "far"), ids(index, "301.14 OR 405.2"));
            assertEquals(Set.of("close", "half"), ids(index, "301 AND NOT caffeine"));
        }
    }

    @Test
    @DisplayName("a query that is not lucene is still rejected as the client's error")
    void malformedQueriesStillFail() throws Exception {
        try (var index = index()) {
            assertThrows(ResponseStatusException.class, () -> ids(index, "301.14 AND ("));
        }
    }

    @Test
    void theHalfWidthFollowsThePrecisionWithAPpmFloor() {
        assertEquals(0.5, NumericFreeTextQueryNodeProcessor.halfWidth("301", 301, 10), 1e-12);
        assertEquals(0.05, NumericFreeTextQueryNodeProcessor.halfWidth("301.1", 301.1, 10), 1e-12);
        assertEquals(0.005, NumericFreeTextQueryNodeProcessor.halfWidth("301.14", 301.14, 10), 1e-12);
        assertEquals(10 * 301.1421 / 1e6, NumericFreeTextQueryNodeProcessor.halfWidth("301.1421", 301.1421, 10), 1e-12,
                "four decimals: the ppm floor wins");
        assertEquals(0.005, NumericFreeTextQueryNodeProcessor.halfWidth("12.00", 12, 10), 1e-12, "trailing zeros state precision too");
    }

}
