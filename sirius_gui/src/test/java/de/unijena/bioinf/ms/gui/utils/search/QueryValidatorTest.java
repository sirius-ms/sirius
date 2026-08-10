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

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the live validation of the free-text query: syntax problems and unknown field names are
 * reported as human-readable warnings (the server would silently match nothing for unknown fields).
 */
public class QueryValidatorTest {

    private static final List<SearchableField> FIELDS = List.of(
            new SearchableField().name("ionMass").fieldType(SearchableFieldType.DOUBLE),
            new SearchableField().name("quality").fieldType(SearchableFieldType.ENUM),
            new SearchableField().name("tags.city").fieldType(SearchableFieldType.TEXT),
            new SearchableField().name("topAnnotations.structureAnnotation.inchiKey").fieldType(SearchableFieldType.TEXT));

    private static Optional<String> validate(String query) {
        return QueryValidator.validate(query, FIELDS);
    }

    @Test
    public void testValidQueriesPass() {
        assertTrue(validate("").isEmpty());
        assertTrue(validate("caffeine").isEmpty());
        assertTrue(validate("ionMass:[300 TO 400]").isEmpty());
        assertTrue(validate("quality:GOOD AND tags.city:\"new york\"").isEmpty());
        assertTrue(validate("pyro*").isEmpty());
    }

    @Test
    public void testSyntaxErrorIsReported() {
        Optional<String> problem = validate("name:\"unbalanced");
        assertTrue(problem.isPresent());
        assertTrue(problem.get().toLowerCase().contains("syntax"), problem.get());
    }

    @Test
    public void testUnknownFieldIsReportedWithSuggestion() {
        Optional<String> problem = validate("ionmasses:300");
        assertTrue(problem.isPresent());
        assertTrue(problem.get().contains("ionmasses"), problem.get());
        assertTrue(problem.get().contains("ionMass"), "should suggest the known field: " + problem.get());
    }

    @Test
    public void testKnownFieldsAreCaseInsensitiveAndNestedFieldsWork() {
        assertTrue(validate("IONMASS:300").isEmpty());
        assertTrue(validate("topAnnotations.structureAnnotation.inchiKey:XYZ").isEmpty());
    }

    @Test
    public void testUnknownTagFieldsAreNotFlagged() {
        // tag definitions can lag behind the cached field list - do not flag tags.* as unknown
        assertTrue(validate("tags.freshlyCreated:foo").isEmpty());
    }

    @Test
    public void testEscapedColonsAreNotFieldSeparators() {
        // a time value with escaped colons must not make "12" an unknown field
        assertTrue(validate("tags.city:foo AND ionMass:12\\:00").isEmpty());
    }
}
