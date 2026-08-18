/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import de.unijena.bioinf.ms.middleware.service.search.description.DetectedAdductPossibleValues;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PrecursorIonTypeQueryRewriter;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adducts are indexed in the canonical notation of {@link PrecursorIonType#toString()}, which spaces its
 * operators ({@code [M + H]+}). Users write them the way they are used to - {@code [M+H]+}, {@code M+H} - and
 * would find nothing, since the field is keyword indexed and matches only the exact term. Both halves of the
 * fix are tested here: queries are normalized by parsing them, and the common adducts are offered as values.
 */
public class AdductSearchTest {

    private static final String FIELD = "detectedAdducts";

    private final PrecursorIonTypeQueryRewriter rewriter = new PrecursorIonTypeQueryRewriter();

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestFeature {
        @IndexField(documentId = true)
        public String id;

        @IndexField(queryRewriter = PrecursorIonTypeQueryRewriter.class)
        public Set<String> detectedAdducts;
    }

    private SearchService searchService;
    private Project<?> mockProject;
    private final String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");
        searchService = new SearchServiceImpl(project -> new PerPojoSearchContext(null, new HashMap<>()));
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    // ---- normalizing what the user typed ------------------------------------------------------------------

    private Query rewrite(String typed) {
        return rewriter.rewrite(FIELD, typed, false);
    }

    private static TermQuery termQuery(String text) {
        return new TermQuery(new Term(FIELD, text));
    }

    @Test
    public void testCommonlyTypedNotationsAreNormalized() {
        assertEquals(termQuery("[M + H]+"), rewrite("[M+H]+"), "omitted whitespace");
        assertEquals(termQuery("[M + H]+"), rewrite("M+H"), "omitted brackets and charge");
        assertEquals(termQuery("[M + H]+"), rewrite("[M+H]"), "omitted charge");
        assertEquals(termQuery("[M + Na]+"), rewrite("[M+Na]+"));
        assertEquals(termQuery("[M - H]-"), rewrite("[M-H]-"));
        assertEquals(termQuery("[M + H]+"), rewrite("  [M + H]+  "), "surrounding whitespace");
    }

    /**
     * Every known adduct must be findable when written without whitespace, which is how they are usually typed.
     * A property over the whole table rather than a handful of examples, so adducts added later are covered.
     */
    @Test
    public void testEveryKnownAdductIsFoundWithoutWhitespace() {
        for (PrecursorIonType ionType : PeriodicTable.getInstance().getAdductsAndUnKnowns()) {
            String canonical = ionType.toString();
            String asTyped = canonical.replaceAll("\\s+", "");
            if (asTyped.equals(canonical))
                continue; // nothing to normalize
            assertEquals(termQuery(canonical), rewrite(asTyped),
                    "adduct '" + asTyped + "' must be normalized to '" + canonical + "'");
        }
    }

    /**
     * A query that is already canonical, or that is no adduct at all, is left exactly as it was: rewriting is an
     * improvement of the query, never a restriction of what can be searched.
     */
    @Test
    public void testNothingIsRewrittenWhenThereIsNothingToNormalize() {
        assertNull(rewrite("[M + H]+"), "already canonical");
        assertNull(rewrite("not an adduct"));
        assertNull(rewrite(""));
        assertNull(rewrite("   "));
    }

    /**
     * The adduct parser is lenient: text it recognizes nothing in comes back as the intrinsically charged
     * {@code [M]+} rather than as an error. Rewriting to that would turn a search for something else into a
     * search for all intrinsically charged features - a wrong answer is worse than no answer.
     */
    @Test
    public void testUnrecognizableTextIsNotTurnedIntoTheIntrinsicallyChargedAdduct() {
        assertNull(rewrite("Xxx"));
        assertNull(rewrite("caffeine"));

        // ... while the intrinsically charged adduct itself stays searchable in the notations users write
        assertEquals(termQuery("[M]+"), rewrite("M+"));
        assertEquals(termQuery("[M]-"), rewrite("M-"));
    }

    /**
     * A search box must not be able to change what SIRIUS considers a common ion mode. Parsing an adduct whose
     * ion mode is unknown normally registers it, which is right for data being imported and wrong here: one
     * typo would change how every adduct parsed afterwards is read.
     */
    @Test
    public void testAQueryDoesNotWidenTheKnownIonModes() {
        List<String> before = knownPositiveIonModes();

        rewrite("[M+Xe]+"); // adduct-shaped, and no Xe ion mode is known

        assertEquals(before, knownPositiveIonModes());
    }

    private static List<String> knownPositiveIonModes() {
        List<String> names = new ArrayList<>();
        PeriodicTable.getInstance().getKnownIonModes(1).forEach(ion -> names.add(ion.toString()));
        return names;
    }

    // ---- searching with a sloppily written adduct ---------------------------------------------------------

    @Test
    public void testFeatureIsFoundByASloppilyWrittenAdduct() {
        searchService.addDocument(projectId, new TestFeature("1", Set.of("[M + H]+")));
        searchService.addDocument(projectId, new TestFeature("2", Set.of("[M + Na]+")));

        assertEquals(1, searchService.search(projectId, "detectedAdducts:\"[M+H]+\"",
                PageRequest.of(0, 10), TestFeature.class).getTotalElements());
        assertEquals(1, searchService.search(projectId, "detectedAdducts:\"M+H\"",
                PageRequest.of(0, 10), TestFeature.class).getTotalElements());
        // the canonical notation keeps working, and still selects only its own feature
        assertEquals(1, searchService.search(projectId, "detectedAdducts:\"[M + Na]+\"",
                PageRequest.of(0, 10), TestFeature.class).getTotalElements());
        assertEquals(0, searchService.search(projectId, "detectedAdducts:\"[M+K]+\"",
                PageRequest.of(0, 10), TestFeature.class).getTotalElements());
    }

    // ---- the values offered for completion ----------------------------------------------------------------

    private static List<String> offeredAdducts(String... detectedInProject) {
        return new DetectedAdductPossibleValues(() -> Set.of(detectedInProject)).getPossibleValues(FIELD);
    }

    /**
     * Searchable fields are described per project, so the vocabulary is the adducts this project actually
     * detected - not the open domain of everything that could be an adduct.
     */
    @Test
    public void testOnlyTheAdductsOfTheProjectAreOffered() {
        List<String> offered = offeredAdducts("[M + H]+", "[M + Na]+");

        assertTrue(offered.contains("[M + H]+"));
        assertTrue(offered.contains("[M + Na]+"));
        assertFalse(offered.contains("[M + K]+"), "not detected in this project: " + offered);
    }

    /**
     * A feature whose adducts could not be detected is indexed under the unknown ion type of its charge, and
     * the project records only real detections - so the value most features carry in a fresh project would be
     * missing from its own vocabulary if it were not added.
     */
    @Test
    public void testUnknownAdductsAreOfferedAlthoughTheProjectDoesNotRecordThem() {
        List<String> offered = offeredAdducts("[M + H]+");

        assertTrue(offered.contains(PrecursorIonType.unknownPositive().toString()), "offered: " + offered);
        assertTrue(offered.contains(PrecursorIonType.unknownNegative().toString()));
    }

    @Test
    public void testProjectWithoutRecordedAdductsStillOffersTheUnknownOnes() {
        assertEquals(List.of(PrecursorIonType.unknownPositive().toString(),
                PrecursorIonType.unknownNegative().toString()), offeredAdducts());
        assertNull(new DetectedAdductPossibleValues(() -> null).getPossibleValues(FIELD),
                "a project that cannot report its adducts offers none");
    }

    @Test
    public void testOtherFieldsAreNotAnswered() {
        assertNull(new DetectedAdductPossibleValues(() -> Set.of("[M + H]+")).getPossibleValues("name"));
    }

    /**
     * The offered values must be the indexed notation: an offer the query then has to normalize would mean the
     * completion hands out something the index does not contain.
     */
    @Test
    public void testOfferedValuesAreTheIndexedNotation() {
        for (String offered : offeredAdducts("[M + H]+", "[M + Na]+", "[M - H]-"))
            assertNull(rewrite(offered), "offered value '" + offered + "' is not the canonical notation");
    }

    @Test
    public void testOfferedValuesAreOrderedAndFreeOfDuplicates() {
        List<String> offered = offeredAdducts("[M + Na]+", "[M + H]+", PrecursorIonType.unknownPositive().toString());

        assertEquals(offered.stream().distinct().toList(), offered, "no duplicates, also across the unknowns");
        // the most common ion types first - that is what a completion should show at the top
        assertTrue(offered.indexOf("[M + H]+") < offered.indexOf("[M + Na]+"), "offered: " + offered);
    }

    /**
     * Guards the wiring: the rewriter has to be declared on the real model field, not only on a test pojo. The
     * values are not declared there - they are project state and come from the search context.
     */
    @Test
    public void testTheRealModelFieldDeclaresTheRewriter() throws NoSuchFieldException {
        IndexField annotation = AlignedFeature.class.getDeclaredField(DetectedAdductPossibleValues.DETECTED_ADDUCTS_FIELD)
                .getAnnotation(IndexField.class);

        assertEquals(PrecursorIonTypeQueryRewriter.class, annotation.queryRewriter());
    }

    /**
     * End to end: the field the provider answers for has to be the one the model is indexed under, and the
     * search context has to apply the provider to it.
     */
    @Test
    public void testTheSearchContextReportsTheProjectsAdducts() throws IOException {
        Set<String> detected = new HashSet<>(Set.of("[M + H]+"));
        try (PerPojoSearchContext context = new PerPojoSearchContext(null, new HashMap<>())) {
            SearchableFieldService fields = DescribedFields.serviceFor(context,
                    new DetectedAdductPossibleValues(() -> detected));

            // the unknown ion types come first, as SIRIUS ranks them: "any adduct" before a specific one
            assertEquals(List.of(PrecursorIonType.unknownPositive().toString(),
                            PrecursorIonType.unknownNegative().toString(), "[M + H]+"),
                    adductField(fields).getPossibleValues());

            // an import detecting another adduct is reported without describing the index again
            detected.add("[M + Na]+");
            assertTrue(adductField(fields).getPossibleValues().contains("[M + Na]+"));
        }
    }

    private static SearchableField adductField(SearchableFieldService fields) {
        return fields.describe(AlignedFeature.class).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()))
                .get(FIELD);
    }
}
