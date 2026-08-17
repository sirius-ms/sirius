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
                    .possibleValues(List.of("Alkaloids", "Amino acids and Peptides", "Terpenoids")));

    private TokenInputModel model;

    @BeforeEach
    public void setup() {
        model = new TokenInputModel();
        model.updateContext(FIELDS, false, false);
    }

    private TokenInputModel.Suggestion suggestion(String display, String typed) {
        return model.suggestions(typed).stream()
                .filter(s -> s.display().equals(display))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no suggestion '" + display + "' in " + model.suggestions(typed).stream()
                                .map(TokenInputModel.Suggestion::display).toList()));
    }

    private List<String> fragmentTexts() {
        return model.pendingFragments().stream().map(TokenInputModel.Fragment::text).toList();
    }

    // --- entry-stage suggestions ---

    @Test
    public void testFieldStageListsAllFieldsAndApplicableTokens() {
        // no sibling -> entry stage is FIELD
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
    public void testWithSiblingTheEntryStageIsConnector() {
        model.updateContext(FIELDS, true, false);
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage());
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertEquals(List.of("AND", "OR"), displays, "the connector stage offers only AND/OR");

        // choosing a connector advances to the field stage, where fields and ')' appear
        model.updateContext(FIELDS, true, true);
        model.choose(suggestion("OR", ""));
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());
        List<String> fieldStage = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertTrue(fieldStage.containsAll(List.of("ionMass", "NOT", "(", ")")));
        assertFalse(fieldStage.contains("AND"));
    }

    @Test
    public void testTypingNarrowsConnectors() {
        model.updateContext(FIELDS, true, false);
        List<String> connectors = model.suggestions("o").stream()
                .filter(s -> s instanceof TokenInputModel.Suggestion.TokenSuggestion)
                .map(TokenInputModel.Suggestion::display).toList();
        assertEquals(List.of("OR"), connectors);
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
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("OR", ""));
        model.choose(suggestion("NOT", ""));
        assertEquals(List.of("OR", "NOT"), fragmentTexts());

        model.choose(suggestion("quality", ""));
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("BAD", "")).orElseThrow();
        assertTrue(completed.clause().negated());
        assertEquals(LogicOp.OR, completed.logic());

        // pending state must not leak into the next clause: the next token starts at the connector
        // stage again with no negation carried over
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage());
        model.choose(suggestion("AND", ""));
        model.choose(suggestion("name", ""));
        TokenInputModel.Event.ClauseCompleted second = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("x").orElseThrow();
        assertFalse(second.clause().negated());
        assertEquals(LogicOp.AND, second.logic());
    }

    @Test
    public void testFieldStageHidesAlreadyChosenNot() {
        // no sibling -> FIELD stage, NOT selectable then gone once pending
        model.choose(suggestion("NOT", ""));
        assertFalse(model.suggestions("").stream().anyMatch(s -> s.display().equals("NOT")),
                "NOT is already pending");
    }

    @Test
    public void testConnectorsLeaveTheListOnceChosen() {
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("AND", ""));
        // choosing a connector advances to the field stage, which no longer offers connectors
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertFalse(displays.contains("AND"));
        assertFalse(displays.contains("OR"));
    }

    @Test
    public void testOpenGroupConsumesPendingNegationAndLogic() {
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("OR", ""));
        model.choose(suggestion("NOT", ""));

        TokenInputModel.Event.OpenGroup event = (TokenInputModel.Event.OpenGroup)
                model.choose(suggestion("(", "")).orElseThrow();
        assertTrue(event.negated());
        assertEquals(LogicOp.OR, event.logic());
        assertTrue(model.pendingFragments().isEmpty(), "pending tokens are consumed by the group");
    }

    @Test
    public void testCloseGroupEvent() {
        model.updateContext(FIELDS, false, true);
        assertInstanceOf(TokenInputModel.Event.CloseGroup.class,
                model.choose(suggestion(")", "")).orElseThrow());
    }

    @Test
    public void testConnectorStageInsideOpenGroupOffersCloseGroup() {
        // sibling + a group open -> connector stage; ) is offered here too so the group can be closed
        // from the keyboard (select + Enter) right after a committed clause, without adding another
        model.updateContext(FIELDS, true, true);
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage());
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertEquals(List.of("AND", "OR", ")"), displays);
        assertInstanceOf(TokenInputModel.Event.CloseGroup.class,
                model.choose(suggestion(")", "")).orElseThrow());
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
        model.updateContext(FIELDS, true, false); // sibling -> connector defaults to AND
        TokenInputModel.Event.ClauseCompleted completed =
                (TokenInputModel.Event.ClauseCompleted) model.completeFreeText("caffeine metabolite");
        assertTrue(completed.clause().isFreeText());
        assertEquals("caffeine metabolite", completed.clause().value1());
        assertEquals(LogicOp.AND, completed.logic());
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage(), "token is reset after completion");
    }

    // --- typed-through multi-token input (grammar shortcut) ---

    @Test
    public void testTypedMultiTokenTextAppliesInOneSubmit() {
        model.updateContext(FIELDS, true, false);
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
        // no sibling -> NOT is choosable directly at the field stage
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
    public void testBackspaceFromFieldStageClearsTheConnectorBackToConnectorStage() {
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("OR", "")); // CONNECTOR -> FIELD, pending OR
        assertEquals(TokenInputModel.Stage.FIELD, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty()); // clears the pending connector
        model.updateContext(FIELDS, true, false);       // owner re-syncs the context
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage());
        assertTrue(model.pendingFragments().isEmpty());
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
    public void testRunActionRowIsOfferedAtTheConnectorStage() {
        model.setRunAction(RUN_SEARCH);
        model.updateContext(FIELDS, true, false); // sibling -> connector stage
        assertEquals(TokenInputModel.Stage.CONNECTOR, model.stage());
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
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("OR", ""));
        model.choose(suggestion("NOT", ""));
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
}
