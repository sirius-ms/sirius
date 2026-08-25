package de.unijena.bioinf.ms.middleware.service.projects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;

/**
 * Until compounds are indexed, compound queries are answered from the document store. They must select exactly the
 * compounds the search index would select, so that the restriction is only about which queries are supported and
 * never about what a supported query means.
 */
class CompoundIdQueryTest {

    private static CompoundIdQuery parse(String query) {
        return CompoundIdQuery.parse(query);
    }

    private static ResponseStatusException rejected(String query) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> parse(query),
                () -> "query should have been rejected: " + query);
        assertEquals(METHOD_NOT_ALLOWED, e.getStatusCode());
        return e;
    }

    @Test
    @DisplayName("a single id selects that compound")
    void singleId() {
        assertEquals(Set.of(1L), parse("compoundId:1").ids());
        assertFalse(parse("compoundId:1").negated());
    }

    @Test
    @DisplayName("the compound id is the default field")
    void bareTermIsAnId() {
        assertEquals(Set.of(7L), parse("7").ids());
    }

    @Test
    @DisplayName("OR unites the ids")
    void orUnitesIds() {
        assertEquals(Set.of(1L, 2L, 3L), parse("compoundId:1 OR compoundId:2 OR compoundId:3").ids());
    }

    @Test
    @DisplayName("AND of two ids selects nothing, since a compound has exactly one id")
    void andOfTwoIdsSelectsNothing() {
        assertTrue(parse("compoundId:1 AND compoundId:2").selectsNothing());
    }

    @Test
    @DisplayName("AND of an id with itself selects that compound")
    void andOfTheSameIdSelectsIt() {
        assertEquals(Set.of(1L), parse("compoundId:1 AND compoundId:1").ids());
    }

    @Test
    @DisplayName("NOT selects everything but the given ids")
    void notExcludesIds() {
        CompoundIdQuery selection = parse("NOT compoundId:3");

        assertTrue(selection.negated());
        assertEquals(Set.of(3L), selection.ids());
        assertFalse(selection.selectsNothing());
    }

    @Test
    @DisplayName("prohibited clauses are subtracted from the selected ones")
    void prohibitedClausesAreSubtracted() {
        CompoundIdQuery selection = parse("compoundId:1 OR compoundId:2 NOT compoundId:2");

        assertFalse(selection.negated());
        assertEquals(Set.of(1L), selection.ids());
    }

    @Test
    @DisplayName("groups are combined like in the index")
    void nestedGroupsAreSupported() {
        CompoundIdQuery selection = parse("(compoundId:1 OR compoundId:2) AND NOT compoundId:1");

        assertFalse(selection.negated());
        assertEquals(Set.of(2L), selection.ids());
    }

    @Test
    @DisplayName("a match all query does not restrict anything")
    void matchAllSelectsEverything() {
        assertTrue(parse("*:*").selectsEverything());
    }

    @Test
    @DisplayName("queries on other fields are rejected")
    void otherFieldsAreRejected() {
        assertTrue(rejected("tags.MyTag:sample").getReason().contains("tags.MyTag"));
    }

    @Test
    @DisplayName("queries that are not plain ids are rejected")
    void nonIdQueriesAreRejected() {
        rejected("compoundId:[1 TO 5]");
        rejected("compoundId:1*");
        rejected("compoundId:notAnId");
    }
}
