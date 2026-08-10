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
            field("tags.city", SearchableFieldType.TEXT));

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

    // --- IDLE suggestions ---

    @Test
    public void testIdleListsAllFieldsAndApplicableTokens() {
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertTrue(displays.containsAll(List.of("ionMass", "name", "quality", "hasMsMs", "tags.city")));
        assertTrue(displays.contains("NOT"));
        assertTrue(displays.contains("("));
        // no sibling, no open group -> no connectors, no closing paren
        assertFalse(displays.contains("AND"));
        assertFalse(displays.contains(")"));
    }

    @Test
    public void testConnectorsOfferedWithSiblingAndCloseParenWithOpenGroup() {
        model.updateContext(FIELDS, true, true);
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertTrue(displays.containsAll(List.of("AND", "OR", ")")));
    }

    @Test
    public void testTypingNarrowsSuggestions() {
        List<String> displays = model.suggestions("ion").stream().map(TokenInputModel.Suggestion::display).toList();
        assertEquals(List.of("ionMass"), displays);

        // segment matching finds nested fields
        assertTrue(model.suggestions("city").stream().anyMatch(s -> s.display().equals("tags.city")));
    }

    // --- staged clause building ---

    @Test
    public void testTextFieldGoesStraightToValueStage() {
        model.choose(suggestion("name", ""));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());
        assertTrue(model.suggestions("").isEmpty(), "free text values have no suggestions");

        Optional<TokenInputModel.Event> event = model.submitTyped("caffeine");
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted) event.orElseThrow();
        assertEquals("name", completed.clause().field());
        assertEquals("caffeine", completed.clause().value1());
        assertNull(completed.clause().op());
        assertEquals(LogicOp.AND, completed.logic());
        assertEquals(TokenInputModel.Stage.IDLE, model.stage());
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
        assertEquals(List.of("OR", "NOT"), model.pendingFragments());

        model.choose(suggestion("quality", ""));
        TokenInputModel.Event.ClauseCompleted completed = (TokenInputModel.Event.ClauseCompleted)
                model.choose(suggestion("BAD", "")).orElseThrow();
        assertTrue(completed.clause().negated());
        assertEquals(LogicOp.OR, completed.logic());

        // pending state must not leak into the next clause
        model.choose(suggestion("name", ""));
        TokenInputModel.Event.ClauseCompleted second = (TokenInputModel.Event.ClauseCompleted)
                model.submitTyped("x").orElseThrow();
        assertFalse(second.clause().negated());
        assertEquals(LogicOp.AND, second.logic());
    }

    @Test
    public void testChosenTokensDisappearFromTheSuggestions() {
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("NOT", ""));
        List<String> displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertFalse(displays.contains("NOT"), "NOT is already pending");

        model.choose(suggestion("AND", ""));
        displays = model.suggestions("").stream().map(TokenInputModel.Suggestion::display).toList();
        assertFalse(displays.contains("AND"), "connector is already pending");
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

    // --- typed-through multi-token input (grammar shortcut) ---

    @Test
    public void testTypedMultiTokenTextAppliesInOneSubmit() {
        model.updateContext(FIELDS, true, false);
        assertTrue(model.submitTyped("or not ion").isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());
        assertEquals(List.of("OR", "NOT", "ionMass"), model.pendingFragments());
    }

    @Test
    public void testUnmatchedTypedTextIsFreeText() {
        assertTrue(model.submitTyped("caffeine metabolite").isEmpty());
        assertEquals(TokenInputModel.Stage.IDLE, model.stage());
        assertTrue(model.pendingFragments().isEmpty(), "free text must not consume a stage");
    }

    // --- backspace pops stages ---

    @Test
    public void testBackspacePopsStages() {
        model.updateContext(FIELDS, true, false);
        model.choose(suggestion("NOT", ""));
        model.choose(suggestion("ionMass", ""));
        model.choose(model.suggestions(">=").get(0));
        assertEquals(TokenInputModel.Stage.VALUE, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertEquals(TokenInputModel.Stage.OPERATOR, model.stage());

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertEquals(TokenInputModel.Stage.IDLE, model.stage());
        assertEquals(List.of("NOT"), model.pendingFragments(), "field popped, NOT still pending");

        assertTrue(model.backspaceOnEmpty().isEmpty());
        assertTrue(model.pendingFragments().isEmpty(), "NOT popped");

        // nothing pending anymore -> ask the owner to remove the last committed chip
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
}
