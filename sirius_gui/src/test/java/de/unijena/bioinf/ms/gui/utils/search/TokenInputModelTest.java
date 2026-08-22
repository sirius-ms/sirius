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

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the staged token input of the search bar (GitLab-filtered-search style): at IDLE all
 * fields and applicable special tokens are suggested and narrow while typing; choosing a field
 * advances to the operator stage (numeric fields only) and then the value stage(s); ranges take two
 * staged values; backspace on empty input pops one stage.
 */
public class TokenInputModelTest {

    private static SearchableField field(String name, SearchableFieldType type) {
        return new SearchableField().name(name).fieldType(type);
    }

    private static final List<SearchableField> FIELDS = List.of(
            field("ionMass", SearchableFieldType.DOUBLE),
            field("name", SearchableFieldType.TEXT),
            field("quality", SearchableFieldType.ENUM).possibleValues(List.of("GOOD", "DECENT", "BAD")),
            field("hasMsMs", SearchableFieldType.BOOLEAN),
            field("tags.city", SearchableFieldType.TEXT),
            field("npcPathway", SearchableFieldType.TEXT)
                    .possibleValues(List.of("Alkaloids", "Amino acids and Peptides", "Terpenoids")),
            // a presence flag: written only when the property holds, so true is the only value it can take
            field("lipid", SearchableFieldType.BOOLEAN).possibleValues(List.of("true")),
            // a tag its definition currently restricts to one value is NOT a flag - it holds a value, and
            // the definition can be extended while the project is open
            field("tags.batch", SearchableFieldType.TEXT).possibleValues(List.of("B1")),
            field("tags.measured", SearchableFieldType.DATE).possibleValues(List.of("1970-01-01")));

    private TokenInputModel model;

    @BeforeEach
    public void setup() {
        model = new TokenInputModel();
        model.updateContext(FIELDS, TokenInputModel.Join.NOTHING, false);
    }

    private TokenInputModel.Suggestion suggestion(String display, String typed) {
        return model.suggestions(typed).stream()
                .filter(s -> s.display().equals(display))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no suggestion '" + display + "' in " + model.suggestions(typed).stream()
                                .map(TokenInputModel.Suggestion::display).toList()));
    }

    private static List<String> displays(List<TokenInputModel.Suggestion> suggestions) {
        return suggestions.stream().map(TokenInputModel.Suggestion::display).toList();
    }

    private List<String> fragmentTexts() {
        return model.pendingFragments().stream().map(TokenInputModel.Fragment::text).toList();
    }

    // --- entry-stage suggestions ---

    @Test
    public void testFieldStageListsAllFieldsAndApplicableTokens() {
        // nothing to join -> the rows carry no connector
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertTrue(displays.containsAll(List.of("ionMass", "name", "quality", "hasMsMs", "tags.city")));
        assertTrue(displays.contains("NOT"));
        assertTrue(displays.contains("("));
        // connectors live in their own stage, never in the field list; no group open -> no ')'
        assertFalse(displays.contains("AND"));
        assertFalse(displays.contains("OR"));
        assertFalse(displays.contains(")"));
    }

    @Test
    public void testWithASiblingEveryRowCarriesItsConnector() {
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertEquals(TokenInputModel.Stage.FIELD, model.stage(), "there is no connector stage");
        // simple mode: the top-level terms are ANDed, so that is the only connector offered
        List<String> displays = displays(model.suggestions(""));
        assertTrue(displays.containsAll(List.of("AND ionMass", "AND NOT", "AND (")));
        assertTrue(displays.stream().noneMatch(d -> d.startsWith("OR ")));
        assertTrue(displays.stream().noneMatch(d -> d.equals("ionMass")),
                "an unjoined row would hide which connector it is added with");

        // advanced mode offers both, the variants of a candidate adjacent and AND first
        model.setMode(QueryMode.ADVANCED);
        List<String> advanced = displays(model.suggestions("ionmass"));
        assertEquals(List.of("AND ionMass", "OR ionMass"), advanced.subList(0, 2));
    }

    @Test
    public void testTheFirstTermOfAContainerCarriesNoConnector() {
        // nothing to join yet -> the rows stay bare (the empty top level, or a freshly opened group)
        assertTrue(displays(model.suggestions("")).contains("ionMass"));
        model.updateContext(FIELDS, TokenInputModel.Join.NOTHING, true);
        assertTrue(displays(model.suggestions("")).contains("ionMass"));
    }

    @Test
    public void testATypedConnectorNarrowsTheRowsToIt() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        // the whole input so far is the connector -> everything, joined by it
        List<String> or = displays(model.suggestions("or"));
        assertTrue(or.stream().allMatch(d -> d.startsWith("OR ")), or.toString());
        assertTrue(or.contains("OR ionMass"));
        // ... and with a prefix after it, only what that prefix matches
        assertEquals(List.of("OR ionMass", "OR \"ion\""), displays(model.suggestions("or ion")));
        // whitespace is required, so a field prefix that merely starts like a connector still matches
        // fields (there is none here, so only the free-text row survives)
        assertEquals(List.of("AND \"orma\"", "OR \"orma\""), displays(model.suggestions("orma")));
    }

    @Test
    public void testTypingNarrowsSuggestions() {
        // the free-text row is appended at every entry stage; assert on the field suggestions
        List<String> fields = model.suggestions("ion").stream()
                .filter(s -> s instanceof TokenInputModel.Suggestion.FieldSuggestion)
                .map(TokenInputModel.Suggestion::display).toList();
        assertEquals(List.of("ionMass"), fields);

        // segment matching finds nested fields
        assertTrue(model.suggestions("city").stream().anyMatch(s -> s.display().equals("tags.city")));
    }

    // --- staged clause building ---

    @Test
    public void testTextFieldGoesStraightToValueStage() {
        // no sibling -> starts at FIELD, choose a field directly
        model.choose(suggestion("name", ""));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
        assertTrue(model.suggestions("").isEmpty(), "free text values have no suggestions");

        Optional<TokenInputModel.Event> event = model.submitTyped("caffeine");
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted) event.orElseThrow();
        assertEquals("name", completed.clause().field());
        assertEquals("caffeine", completed.clause().value1());
        assertNull(completed.clause().op());
        assertEquals(LogicOp.AND, completed.logic());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
    }

    @Test
    public void testEnumFieldSuggestsItsValues() {
        model.choose(suggestion("quality", ""));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
        assertEquals(List.of("GOOD", "DECENT", "BAD"),
                model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList());
        // value narrowing is case-insensitive
        assertEquals(List.of("GOOD"),
                model.suggestions("go").stream().map(TokenInputModel.Suggestion::display).toList());

        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("GOOD", "")).orElseThrow();
        assertEquals("quality", completed.clause().field());
        assertEquals("GOOD", completed.clause().value1());
    }

    /**
     * A text field with a closed vocabulary (a compound class ontology, a restricted tag) behaves like
     * an enum at the value stage: its values are offered, and picking one completes the clause.
     */
    @Test
    public void testTextFieldWithVocabularySuggestsItsValues() {
        model.choose(suggestion("npcPathway", ""));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
        assertEquals(List.of("Alkaloids", "Amino acids and Peptides", "Terpenoids"),
                model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList());

        // a word inside the value narrows too - nobody recalls where "acids" sits in the name
        assertEquals(List.of("Amino acids and Peptides"),
                model.suggestions("acids").stream().map(TokenInputModel.Suggestion::display).toList());

        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("Amino acids and Peptides", "")).orElseThrow();
        assertEquals("npcPathway", completed.clause().field());
        assertEquals("Amino acids and Peptides", completed.clause().value1());
    }

    /**
     * The vocabulary is an offer, not a restriction: the field stays queryable with anything, e.g. a
     * wildcard over the ontology.
     */
    @Test
    public void testTypedValueIsAcceptedBesideTheVocabulary() {
        model.choose(suggestion("npcPathway", ""));

        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("Alka*").orElseThrow();
        assertEquals("Alka*", completed.clause().value1());
    }

    @Test
    public void testBooleanFieldSuggestsTrueFalse() {
        model.choose(suggestion("hasMsMs", ""));
        assertEquals(List.of("true", "false"),
                model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList());
    }

    @Test
    public void testNumericFieldHasOperatorStage() {
        model.choose(suggestion("ionMass", ""));
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());
        assertEquals(NumberOp.values().length, model.suggestions("").size());

        // ">" narrows to > and >=
        assertEquals(2, model.suggestions(">").size());

        model.choose(model.suggestions(">=").get(0));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());

        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("300").orElseThrow();
        assertEquals(NumberOp.GTE, completed.clause().op());
        assertEquals("300", completed.clause().value1());
    }

    /**
     * The operator rows are offered in {@link NumberOp} declaration order, and that order is a UX
     * decision: equality first because it is the most common numeric filter, then the comparisons
     * with the inclusive bound ahead of the strict one, and the two-valued ranges last because they
     * are the most laborious to fill in. The first row is also what Enter picks (the suggestion list
     * pre-selects index 0), so this pins the default operator too.
     */
    @Test
    public void testOperatorSuggestionsAreOrderedEqualsFirstRangesLast() {
        model.choose(suggestion("ionMass", ""));
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());

        assertEquals(List.of("=", ">=", "<=", ">", "<", "[ TO ]", "{ TO }"),
                model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList());
    }

    @Test
    public void testRangeTakesTwoStagedValuesWithOpenBounds() {
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions("[").get(0)); // inclusive range
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());

        assertTrue(model.submitTyped("300").isEmpty(), "lower bound must advance, not complete");
        assertEquals(TokenInputModel.Stage.VALUE2, model.stage());

        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("").orElseThrow(); // empty upper bound = open ended
        assertEquals(NumberOp.RANGE_INCLUSIVE, completed.clause().op());
        assertEquals("300", completed.clause().value1());
        assertEquals("", completed.clause().value2());
    }

    // --- special tokens ---

    @Test
    public void testNotAndConnectorFlowIntoTheCompletedClause() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR NOT", "")); // the connector and the negation in one pick
        assertEquals(List.of("OR", "NOT"), fragmentTexts());

        // the connector is staged, so what follows is offered unjoined
        model.choose(suggestion("quality", ""));
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("BAD", "")).orElseThrow();
        assertTrue(completed.clause().negated());
        assertEquals(LogicOp.OR, completed.logic());

        // pending state must not leak into the next clause: the next token starts fresh, with the
        // connector to be chosen again and no negation carried over
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        model.choose(suggestion("AND name", ""));
        TokenInputModel.Event.ClauseCompleted second = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("x").orElseThrow();
        assertFalse(second.clause().negated());
        assertEquals(LogicOp.AND, second.logic());
    }

    @Test
    public void testFieldStageHidesAlreadyChosenNot() {
        // nothing to join -> NOT selectable directly, then gone once pending
        model.choose(suggestion("NOT", ""));
        assertFalse(model.suggestions("").stream().anyMatch(s -> s.display().equals("NOT")),
                "NOT is already pending");
    }

    @Test
    public void testAStagedConnectorLeavesTheRowsBare() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR NOT", "")); // stages the connector without completing the token
        List<String> displays = displays(model.suggestions(""));
        assertTrue(displays.contains("ionMass"), "the connector is decided - do not ask again");
        assertTrue(displays.stream().noneMatch(d -> d.startsWith("AND ") || d.startsWith("OR ")));
    }

    @Test
    public void testOpenGroupConsumesPendingNegationAndLogic() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR NOT", ""));

        TokenInputModel.Event.OpenGroup event = (TokenInputModel.Event.OpenGroup)
                model.choose(suggestion("(", "")).orElseThrow();
        assertTrue(event.negated());
        assertEquals(LogicOp.OR, event.logic());
        assertTrue(model.pendingFragments().isEmpty(), "pending tokens are consumed by the group");
    }

    @Test
    public void testCloseGroupEvent() {
        model.updateContext(FIELDS, TokenInputModel.Join.NOTHING, true);
        assertInstanceOf(TokenInputModel.Event.CloseGroup.class,
                model.choose(suggestion(")", "")).orElseThrow());
    }

    @Test
    public void testCloseGroupIsOfferedWithoutAConnector() {
        // inside a group with a sibling: every row is joined by OR (simple mode), except ) - closing
        // the group joins nothing, it ends the term before it
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, true);
        List<String> displays = displays(model.suggestions(""));
        assertTrue(displays.contains("OR ionMass"));
        assertTrue(displays.contains(")"));
        assertFalse(displays.contains("OR )"));
        assertInstanceOf(TokenInputModel.Event.CloseGroup.class,
                model.choose(suggestion(")", "")).orElseThrow());
    }

    // --- the negation sits where it can be found ---

    @Test
    public void testTheNegationIsTheSecondRowOfAnUntouchedList() {
        model.setRunAction(RUN_SEARCH);
        List<String> displays = displays(model.suggestions(""));
        assertEquals("Search", displays.get(0), "the run action stays the default pick");
        assertEquals("NOT", displays.get(1), "half the fields are asked about in the negative - do not bury it");

        // with something to join it carries the connector like every other row, and still sits there
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertEquals("AND NOT", displays(model.suggestions("")).get(1));
    }

    @Test
    public void testWithoutARunActionTheNegationIsFirst() {
        assertEquals("NOT", displays(model.suggestions("")).get(0));
    }

    @Test
    public void testTypingKeepsTheBestMatchFirst() {
        model.setRunAction(RUN_SEARCH);
        // the hoist is for the untouched list only: while typing, the default pick has to be what was typed
        assertEquals("ionMass", displays(model.suggestions("ion")).get(0));
        assertEquals("NOT", displays(model.suggestions("no")).get(0), "and NOT is a match like any other");
    }

    @Test
    public void testAStagedNegationLeavesTheList() {
        model.setRunAction(RUN_SEARCH);
        model.choose(suggestion("NOT", ""));
        assertTrue(displays(model.suggestions("")).stream().noneMatch(d -> d.endsWith("NOT")),
                "it is already staged - offering it again would say it twice");
    }

    // --- query modes: which connectors and nestings the builder offers ---

    @Test
    public void testSimpleModeImpliesAndAtTheTopLevelAndOrInsideAGroup() {
        // top level: AND, and it is what a clause typed without a connector is joined by
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertEquals(List.of(LogicOp.AND), model.allowedConnectors());
        assertTrue(model.submitTyped("name").isEmpty(), "the field is staged, the value is still to come");
        assertEquals(LogicOp.AND, ((TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("caffeine").orElseThrow()).logic());

        // inside a group the alternatives are ORed - including the implied connector of a clause
        // typed without one, which must not fall back to AND
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, true);
        assertEquals(List.of(LogicOp.OR), model.allowedConnectors());
        assertEquals(List.of("OR name"), displays(model.suggestions("name")).subList(0, 1));
        assertTrue(model.submitTyped("name").isEmpty());
        assertEquals(LogicOp.OR, ((TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("caffeine").orElseThrow()).logic());
    }

    @Test
    public void testTheFirstUserTermPicksTheJointToTheFilterPanelBlock() {
        // an empty user query with panel filters showing: the first term is joined to those, and that
        // connector is the joint between the two blocks - which the row shows by opening the brace
        model.updateContext(FIELDS, TokenInputModel.Join.PANEL_TERMS, false);
        assertEquals(List.of("AND ( ionMass"), displays(model.suggestions("ionmass")).subList(0, 1));
        assertTrue(model.suggestions("ionmass").get(0) instanceof TokenInputModel.Suggestion.Joined joined
                && joined.blockJoin(), "the row has to say it is the big joint, not one inside the query");

        // simple mode ANDs the blocks, as it ANDs everything at the top level ...
        assertEquals(List.of(LogicOp.AND), model.allowedConnectors());
        assertTrue(model.inputProblem("or ionmass").isPresent());

        // ... advanced mode offers the OR that makes the query an alternative to the panel filters
        model.setMode(QueryMode.ADVANCED);
        assertEquals(List.of("AND ( ionMass", "OR ( ionMass"), displays(model.suggestions("ionmass")).subList(0, 2));
        assertTrue(model.inputProblem("or ionmass").isEmpty());
        assertTrue(model.canAccept("or ionmass"));
    }

    @Test
    public void testTheBlockJointRidesOnTheEventTheOwnerApplies() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.PANEL_TERMS, false);
        // the owner reads the connector off the event and applies it to the joint (a container drops the
        // logic of its first item, so this is the only place it can be read from)
        model.choose(suggestion("OR ( name", ""));
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("caffeine").orElseThrow();
        assertEquals(LogicOp.OR, completed.logic());

        // an opened group carries it the same way
        model.updateContext(FIELDS, TokenInputModel.Join.PANEL_TERMS, false);
        TokenInputModel.Event.OpenGroup group = (TokenInputModel.Event.OpenGroup)
                model.choose(suggestion("OR ( (", "")).orElseThrow();
        assertEquals(LogicOp.OR, group.logic());
    }

    @Test
    public void testNothingToJoinMeansNoConnectorRows() {
        // no user terms and no panel filters: the term is the whole query, there is nothing to join it to
        model.updateContext(FIELDS, TokenInputModel.Join.NOTHING, false);
        assertTrue(displays(model.suggestions("")).contains("ionMass"));
        assertTrue(model.inputProblem("or ionmass").isEmpty(), "a leading connector is merely meaningless here");
    }

    @Test
    public void testAdvancedModeOffersBothConnectorsEverywhere() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertEquals(List.of(LogicOp.AND, LogicOp.OR), model.allowedConnectors());
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, true);
        assertEquals(List.of(LogicOp.AND, LogicOp.OR), model.allowedConnectors());
        assertTrue(displays(model.suggestions("")).containsAll(List.of("AND ionMass", "OR ionMass")));
    }

    @Test
    public void testSimpleModeRefusesAConnectorItDoesNotOffer() {
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        // a typed OR must not be quietly applied as the AND the mode allows - that would change the
        // meaning of the query, so it is refused and named
        assertTrue(model.inputProblem("or ionmass").isPresent());
        assertTrue(model.inputProblem("or").isPresent());
        assertFalse(model.canAccept("or ionmass"));
        assertTrue(model.submitTyped("or ionmass").isEmpty());
        assertTrue(model.pendingFragments().isEmpty(), "a refused input must not consume a stage");
        assertFalse(model.isTerminal("or ionmass"), "and running the query must not bake it in either");
        // the allowed rows stay on offer, so the way forward is visible
        assertTrue(displays(model.suggestions("or ionmass")).contains("AND ionMass"));

        // inside a group it is the other way round: AND is the one that does not fit
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, true);
        assertTrue(model.inputProblem("and ionmass").isPresent());
        assertTrue(model.inputProblem("or ionmass").isEmpty());

        // and advanced mode allows both
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertTrue(model.inputProblem("or ionmass").isEmpty());
        assertTrue(model.canAccept("or ionmass"));
    }

    @Test
    public void testAConnectorIsOnlyRefusedWhereOneIsAskedFor() {
        // nothing to join: a leading connector is meaningless rather than wrong (CompletionParser
        // consumes it and builds the clause without one)
        assertTrue(model.inputProblem("or ionmass").isEmpty());
        assertTrue(model.inputProblem("").isEmpty());
        assertTrue(model.inputProblem("caffeine").isEmpty(), "a word is a full-text term, not a problem");

        // already staged: the connector is settled, so text is not read as one any more
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("AND NOT", ""));
        assertTrue(model.inputProblem("or ionmass").isEmpty());
    }

    @Test
    public void testSimpleModeDoesNotNestGroups() {
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertTrue(model.nestingAllowed(), "a group at the top level is what simple mode is built on");
        assertTrue(displays(model.suggestions("")).contains("AND ("));

        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, true); // inside a group
        assertFalse(model.nestingAllowed());
        assertFalse(displays(model.suggestions("")).contains("OR ("));
        assertTrue(model.inputProblem("(").isPresent(), "nor typed");
        assertTrue(model.suggestions("(").isEmpty(),
                "and not as a full-text term either - nobody searches for a literal paren");
        assertTrue(model.inputProblem("(ionmass").isPresent());
        assertFalse(model.canAccept("("));
        assertTrue(model.submitTyped("(").isEmpty());

        model.setMode(QueryMode.ADVANCED);
        assertTrue(model.nestingAllowed());
        assertTrue(displays(model.suggestions("")).contains("AND ("));
        assertTrue(model.inputProblem("(").isEmpty());
    }

    @Test
    public void testFreeTextAndParseRowsCarryTheConnectorToo() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        List<String> displays = displays(model.suggestions("caffeine"));
        assertEquals(List.of("AND \"caffeine\"", "OR \"caffeine\""), displays,
                "a word after a term is a full-text search - joined by a connector the user picks");

        // and choosing one builds exactly that clause
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("OR \"caffeine\"", "caffeine")).orElseThrow();
        assertTrue(completed.clause().isFreeText());
        assertEquals("caffeine", completed.clause().value1());
        assertEquals(LogicOp.OR, completed.logic());
    }

    @Test
    public void testJoinedRowsBuildTheSameClauseAsTheTwoStepPickDid() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR quality", ""));
        assertEquals(List.of("OR", "quality"), fragmentTexts());
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("GOOD", "")).orElseThrow();
        assertEquals("quality", completed.clause().field());
        assertEquals("GOOD", completed.clause().value1());
        assertEquals(LogicOp.OR, completed.logic());
    }

    // --- keyless full-text clause ---

    @Test
    public void testEntryStageOffersFreeTextSuggestionLast() {
        // no match -> the only suggestion is the free-text row
        List<TokenInputModel.Suggestion> forUnmatched = model.suggestions("caffeine");
        assertEquals(1, forUnmatched.size());
        assertInstanceOf(TokenInputModel.Suggestion.FreeTextSuggestion.class, forUnmatched.get(0));

        // with a field match, the free-text row is offered last (so the field stays the default)
        List<TokenInputModel.Suggestion> forPrefix = model.suggestions("ion");
        assertInstanceOf(TokenInputModel.Suggestion.FieldSuggestion.class, forPrefix.get(0));
        assertInstanceOf(TokenInputModel.Suggestion.FreeTextSuggestion.class, forPrefix.get(forPrefix.size() - 1));

        // empty input offers no free-text row
        assertTrue(model.suggestions("").stream()
                .noneMatch(s -> s instanceof TokenInputModel.Suggestion.FreeTextSuggestion));
    }

    @Test
    public void testChoosingFreeTextCompletesAKeylessClause() {
        TokenInputModel.Suggestion freeText = model.suggestions("caffeine metabolite").get(0);
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(freeText).orElseThrow();
        assertTrue(completed.clause().isFreeText());
        assertEquals("caffeine metabolite", completed.clause().value1());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
    }

    // --- terminal-token detection (Enter accepts a complete clause before searching) ---

    @Test
    public void testTerminalOnlyForCompleteClauses() {
        // entry stage: a typed default-field term is terminal, empty is not
        assertTrue(model.isTerminal("caffeine"));
        assertFalse(model.isTerminal("  "));

        // field chosen, operator stage: never terminal (must still pick a value)
        model.choose(suggestion("ionMass", ""));
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());
        assertFalse(model.isTerminal(">="));

        // single-valued operator: terminal once a value is typed
        model.choose(model.suggestions(">=").get(0));
        assertFalse(model.isTerminal(""));
        assertTrue(model.isTerminal("300"));
    }

    @Test
    public void testRangeIsTerminalOnlyAtTheUpperBound() {
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions("[").get(0)); // inclusive range
        assertFalse(model.isTerminal("300"), "lower bound alone is not terminal");

        model.submitTyped("300"); // advance to the upper bound
        assertTrue(model.isTerminal(""), "at the upper bound the range completes (open end)");
        assertTrue(model.isTerminal("400"));
    }

    @Test
    public void testCompleteFreeTextMakesAKeylessClause() {
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false); // sibling -> connector defaults to AND
        TokenInputModel.Event.ClauseCompleted completed =
                (TokenInputModel.Event.ClauseCompleted) model.completeFreeText("caffeine metabolite");
        assertTrue(completed.clause().isFreeText());
        assertEquals("caffeine metabolite", completed.clause().value1());
        assertEquals(LogicOp.AND, completed.logic());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage(), "token is reset after completion");
    }

    // --- typed-through multi-token input (grammar shortcut) ---

    @Test
    public void testTypedMultiTokenTextAppliesInOneSubmit() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertTrue(model.submitTyped("or not ion").isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());
        assertEquals(List.of("OR", "NOT", "ionMass"), fragmentTexts());
    }

    @Test
    public void testUnmatchedTypedTextIsFreeText() {
        assertTrue(model.submitTyped("caffeine metabolite").isEmpty());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        assertTrue(model.pendingFragments().isEmpty(), "free text must not consume a stage");
    }

    // --- backspace pops stages ---

    @Test
    public void testBackspacePopsStages() {
        // nothing to join -> NOT is choosable directly
        model.choose(suggestion("NOT", ""));
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions(">=").get(0));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        assertEquals(List.of("NOT"), fragmentTexts(), "field popped, NOT still pending");

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertTrue(model.pendingFragments().isEmpty(), "NOT popped");

        // nothing pending anymore -> ask the owner to remove the last committed chip
        assertInstanceOf(TokenInputModel.Event.RemoveLastNode.class, model.backspaceOnEmpty().orElseThrow());
    }

    @Test
    public void testBackspaceClearsAStagedConnectorBeforeTheLastChip() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR NOT", "")); // stages OR + NOT
        assertEquals(List.of("OR", "NOT"), fragmentTexts());

        assertTrue(model.backspaceOnEmpty().isEmpty(), "pops the negation");
        assertTrue(model.backspaceOnEmpty().isEmpty(), "pops the connector");
        assertTrue(model.pendingFragments().isEmpty());
        assertTrue(displays(model.suggestions("")).contains("OR ionMass"), "the connector is asked again");
        // and only with nothing staged does Backspace reach the committed chips
        assertInstanceOf(TokenInputModel.Event.RemoveLastNode.class, model.backspaceOnEmpty().orElseThrow());
    }

    @Test
    public void testBackspaceFromSecondRangeValueReturnsToFirst() {
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions("[").get(0));
        model.submitTyped("300");
        assertEquals(TokenInputModel.Stage.VALUE2, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
    }

    // --- stage prompts guide the user ---

    @Test
    public void testStagePrompts() {
        assertFalse(model.stagePrompt().isEmpty());
        model.choose(suggestion("ionMass", ""));
        String operatorPrompt = model.stagePrompt();
        assertFalse(operatorPrompt.isEmpty());
        model.choose(model.suggestions("[").get(0));
        assertNotEquals(operatorPrompt, model.stagePrompt());
    }

    // --- parse-as-query entry ---

    @Test
    public void testStructuredTextOffersParseAsQuery() {
        var suggestions = model.suggestions("ionMass:[100 TO 200]");
        assertTrue(suggestions.stream().anyMatch(s -> s instanceof TokenInputModel.Suggestion.FreeTextSuggestion),
                "free text is always offered");
        assertTrue(suggestions.stream().anyMatch(s -> s instanceof TokenInputModel.Suggestion.ParseQuerySuggestion),
                "a field:range query should also offer 'parse as query'");
    }

    @Test
    public void testPlainWordsDoNotOfferParseAsQuery() {
        var suggestions = model.suggestions("caffeine");
        assertTrue(suggestions.stream().anyMatch(s -> s instanceof TokenInputModel.Suggestion.FreeTextSuggestion));
        assertFalse(suggestions.stream().anyMatch(s -> s instanceof TokenInputModel.Suggestion.ParseQuerySuggestion),
                "plain words are just free text - parse-as-query would be redundant");
    }

    @Test
    public void testChooseParseAsQueryEmitsParsedQuery() {
        TokenInputModel.Suggestion parse = model.suggestions("quality:GOOD OR quality:DECENT").stream()
                .filter(s -> s instanceof TokenInputModel.Suggestion.ParseQuerySuggestion)
                .findFirst().orElseThrow();
        TokenInputModel.Event event = model.choose(parse).orElseThrow();
        assertInstanceOf(TokenInputModel.Event.QueryParsed.class, event);
        QueryContainer container = ((TokenInputModel.Event.QueryParsed) event).container();
        assertEquals(2, container.items().size());
        assertEquals(List.of(LogicOp.OR), container.logics());
    }

    // --- the run-search row: Enter on an empty input runs the search ---

    private static final TokenInputModel.RunAction RUN_SEARCH = new TokenInputModel.RunAction(
            "Search", "run the search (Enter)", "Enter to search, type to add query terms");

    @Test
    public void testRunActionRowIsOfferedFirstWhileNothingIsTyped() {
        model.setRunAction(RUN_SEARCH);
        List<TokenInputModel.Suggestion> suggestions = model.suggestions("");
        // on top so it is the default pick of an untouched input - Enter runs the search right away
        assertInstanceOf(TokenInputModel.Suggestion.RunActionSuggestion.class, suggestions.get(0));
        assertEquals("Search", suggestions.get(0).display());
        assertEquals("run the search (Enter)", suggestions.get(0).description());
        // the normal field/token rows still follow it
        assertTrue(suggestions.stream().skip(1)
                .anyMatch(s -> s instanceof TokenInputModel.Suggestion.FieldSuggestion));
    }

    @Test
    public void testRunActionRowDisappearsAsSoonAsSomethingIsTyped() {
        model.setRunAction(RUN_SEARCH);
        assertTrue(model.suggestions("ion").stream().noneMatch(TokenInputModelTest::isRunAction),
                "a typed field prefix must make the field the default pick again");
        assertTrue(model.suggestions("caffeine").stream().noneMatch(TokenInputModelTest::isRunAction),
                "typed free text must not keep the search row selected");
        // whitespace is not "typed something"
        assertInstanceOf(TokenInputModel.Suggestion.RunActionSuggestion.class, model.suggestions("  ").get(0));
    }

    @Test
    public void testRunActionRowIsSuppressedWhileATokenIsStaged() {
        model.setRunAction(RUN_SEARCH);
        model.choose(suggestion("NOT", "")); // still the field stage, but NOT is staged
        assertTrue(model.suggestions("").stream().noneMatch(TokenInputModelTest::isRunAction),
                "a staged NOT means the user is mid-token - do not offer to run the search");

        model.choose(suggestion("ionMass", "")); // operator stage
        assertTrue(model.suggestions("").stream().noneMatch(TokenInputModelTest::isRunAction));
    }

    @Test
    public void testRunActionRowIsOfferedWithASiblingToo() {
        model.setRunAction(RUN_SEARCH);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        assertInstanceOf(TokenInputModel.Suggestion.RunActionSuggestion.class, model.suggestions("").get(0));
    }

    @Test
    public void testRunActionRowRequiresAConfiguredAction() {
        // no run action configured (a host that has no such key) -> the row never shows
        assertTrue(model.suggestions("").stream().noneMatch(TokenInputModelTest::isRunAction));
    }

    @Test
    public void testChoosingTheRunActionChangesNothing() {
        // the owner intercepts the row and runs the search; the model must not touch its token state
        model.setRunAction(RUN_SEARCH);
        TokenInputModel.Suggestion runAction = model.suggestions("").get(0);
        assertTrue(model.choose(runAction).isEmpty());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        assertTrue(model.pendingFragments().isEmpty());
    }

    // --- canAccept: whether Enter/Tab has anything to add (else Enter falls through to the search) ---

    @Test
    public void testCanAcceptMirrorsWhatSubmitTypedConsumes() {
        // entry stage: grammar text is consumed; plain free text is not (the free-text ROW handles it)
        assertTrue(model.canAccept("not ion"));
        assertFalse(model.canAccept("caffeine metabolite"));
        assertFalse(model.canAccept(""));

        model.choose(suggestion("ionMass", "")); // operator stage
        assertTrue(model.canAccept(">="));
        assertFalse(model.canAccept(">"), "ambiguous between > and >= - submitTyped does not consume it");
        assertFalse(model.canAccept("nonsense"));
        assertFalse(model.canAccept(""));

        model.choose(model.suggestions(">=").get(0)); // single-valued value stage
        assertTrue(model.canAccept("300"));
        assertFalse(model.canAccept(""), "an empty single value cannot complete a clause");
    }

    @Test
    public void testCanAcceptOpenRangeBounds() {
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions("[").get(0)); // inclusive range
        assertTrue(model.canAccept(""), "an empty lower bound is an open range start");
        model.submitTyped("300");
        assertTrue(model.canAccept(""), "an empty upper bound is an open range end");
    }

    // --- prompts point at Enter (Tab keeps working as a synonym) ---

    @Test
    public void testStagePromptsPointAtEnterAsTheAcceptKey() {
        model.setRunAction(RUN_SEARCH);
        assertEquals(RUN_SEARCH.emptyPrompt(), model.stagePrompt(),
                "an untouched input advertises the search key and how to build a query");

        model.choose(suggestion("NOT", "")); // staged token -> back to the stage guidance
        assertNotEquals(RUN_SEARCH.emptyPrompt(), model.stagePrompt());
        assertTrue(model.stagePrompt().contains("Enter"));
        assertFalse(model.stagePrompt().contains("Tab"));

        model.choose(suggestion("ionMass", "")); // operator stage
        assertTrue(model.stagePrompt().contains("Enter"));
        assertFalse(model.stagePrompt().contains("Tab"));

        model.choose(model.suggestions("[").get(0)); // range lower bound
        assertTrue(model.stagePrompt().contains("Enter"));
        assertFalse(model.stagePrompt().contains("Tab"));
    }

    @Test
    public void testStagePromptWithoutARunActionKeepsTheStageGuidance() {
        // no run action (the row is off) -> the empty entry stage still guides, without a search key
        assertFalse(model.stagePrompt().isEmpty());
        assertFalse(model.stagePrompt().contains("Tab"));
    }

    // --- staged fragments name their field, so the chip can show it per display mode ---

    @Test
    public void testOnlyTheFieldFragmentCarriesAFieldName() {
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions(">=").get(0));

        List<TokenInputModel.Fragment> fragments = model.pendingFragments();
        assertEquals(List.of("ionMass", ">="), fragmentTexts());
        // the field name lets the chip render compact/fully-qualified and put the real name in its tooltip
        assertEquals("ionMass", fragments.get(0).fieldName());
        assertNull(fragments.get(1).fieldName(), "the operator is display text, not a field");
    }

    @Test
    public void testConnectorAndNegationFragmentsNameNoField() {
        model.setMode(QueryMode.ADVANCED);
        model.updateContext(FIELDS, TokenInputModel.Join.USER_TERMS, false);
        model.choose(suggestion("OR NOT", ""));
        model.choose(suggestion("tags.city", ""));

        List<TokenInputModel.Fragment> fragments = model.pendingFragments();
        assertEquals(List.of("OR", "NOT", "tags.city"), fragmentTexts());
        assertNull(fragments.get(0).fieldName());
        assertNull(fragments.get(1).fieldName());
        assertEquals("tags.city", fragments.get(2).fieldName());
    }

    private static boolean isRunAction(TokenInputModel.Suggestion suggestion) {
        return suggestion instanceof TokenInputModel.Suggestion.RunActionSuggestion;
    }

    // --- fields that can only hold one value ---

    /**
     * A field whose only possible value is true says everything by being mentioned. Picking its name finishes
     * the clause, so the user never has to learn that a presence flag is matched by searching for true.
     */
    @Test
    public void testPickingAFlagFieldCompletesTheClause() {
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("lipid", "")).orElseThrow();

        assertEquals("lipid", completed.clause().field());
        assertEquals("true", completed.clause().value1());
        assertFalse(completed.clause().negated());
        assertEquals(LogicOp.AND, completed.logic());
        assertEquals(TokenInputModel.Stage.FIELD, model.stage(), "nothing is left pending");
    }

    @Test
    public void testTypingAFlagFieldCompletesTheClause() {
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("lipid").orElseThrow();

        assertEquals("lipid", completed.clause().field());
        assertEquals("true", completed.clause().value1());
    }

    /**
     * "no lipid class" is the negation, and it has to compose the same way.
     */
    @Test
    public void testAFlagFieldKeepsAPendingNegation() {
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("not lipid").orElseThrow();

        assertEquals("lipid", completed.clause().field());
        assertEquals("true", completed.clause().value1());
        assertTrue(completed.clause().negated());
    }

    /**
     * Only a flag completes itself. A field that offers one value today but could hold others - a tag its
     * definition currently restricts to a single date - still asks which value is meant.
     */
    @Test
    public void testAFieldWithOneOfferedValueThatIsNoFlagStillAsksForIt() {
        assertTrue(model.choose(suggestion("tags.batch", "")).isEmpty(),
                "one offered value is not the same as one possible value");
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
        assertEquals(List.of("B1"), model.suggestions("").stream()
                .map(TokenInputModel.Suggestion::display).toList());

        model.reset();
        assertTrue(model.choose(suggestion("tags.measured", "")).isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage(), "a date is still compared");
    }

    @Test
    public void testFieldsWithSeveralValuesStillAskForOne() {
        assertTrue(model.choose(suggestion("quality", "")).isEmpty());
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());

        model.reset();
        assertTrue(model.choose(suggestion("hasMsMs", "")).isEmpty(), "a real boolean holds either value");
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
    }

    @Test
    public void testNumericFieldsStillGoToTheOperatorStage() {
        assertTrue(model.choose(suggestion("ionMass", "")).isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());
    }

    /**
     * Completing the field for the user takes nothing away: a query written out by hand still parses, which is
     * how false stays reachable for anyone who wants the flag the other way round.
     */
    @Test
    public void testWritingTheClauseOutStillWorks() {
        TokenInputModel.Event.QueryParsed parsed = (TokenInputModel.Event.QueryParsed)
                model.choose(suggestion("lipid:false", "lipid:false")).orElseThrow();

        QueryClause clause = (QueryClause) parsed.container().items().get(0);
        assertEquals("lipid", clause.field());
        assertEquals("false", clause.value1());
    }
}
