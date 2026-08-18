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

import de.unijena.bioinf.elgordo.LipidClass;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.description.LipidClassVocabulary;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A lipid class is indexed under its long name - "Diacylglycerophosphocholine" - which is what a client is
 * offered, because that is what the index holds. Nobody types it: they type PC. The abbreviations are informal
 * shorthand rather than an official vocabulary, so they are made to match without being offered.
 */
public class LipidClassSearchTest {

    private static final String CLASS_NAME_FIELD =
            "topAnnotations.formulaAnnotation.lipidAnnotation.lipidClassName";
    private static final String LIPID_MAPS_FIELD =
            "topAnnotations.formulaAnnotation.lipidAnnotation.lipidMapsId";

    private final LipidClassVocabulary vocabulary = new LipidClassVocabulary();

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

        // one feature per lipid class, identified by the class it was annotated with
        for (LipidClass lipidClass : LipidClass.values())
            searchService.addDocument(projectId, featureOf(lipidClass));
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    private static AlignedFeature featureOf(LipidClass lipidClass) {
        return AlignedFeature.builder()
                .alignedFeatureId(lipidClass.name())
                .topAnnotations(FeatureAnnotations.builder()
                        .formulaAnnotation(FormulaCandidate.builder()
                                .lipidAnnotation(LipidAnnotation.builder()
                                        .lipidSpecies(lipidClass.abbr() + " 34:1")
                                        .lipidClassName(lipidClass.longName())
                                        .lipidMapsId(lipidClass.getLipidMapsId())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private List<String> search(String query) {
        return searchService.search(projectId, query, PageRequest.of(0, 100), AlignedFeature.class)
                .getContent().stream().map(AlignedFeature::getAlignedFeatureId).toList();
    }

    // ---- matching by abbreviation ---------------------------------------------------------------------------

    /**
     * Every abbreviation must find the class it stands for. A property over the whole enum rather than a few
     * examples, because the analyzer treats them differently - "HexCer" is split on its case change while "PC"
     * is not - and that is exactly the kind of difference a handful of examples misses.
     */
    @Test
    public void testEveryAbbreviationFindsItsClass() {
        for (LipidClass lipidClass : LipidClass.values()) {
            List<String> found = search(CLASS_NAME_FIELD + ":" + lipidClass.abbr());
            assertTrue(found.contains(lipidClass.name()),
                    "searching '" + lipidClass.abbr() + "' must find " + lipidClass.name() + ", found " + found);
        }
    }

    @Test
    public void testTheLongNameKeepsWorking() {
        assertTrue(search(CLASS_NAME_FIELD + ":\"Diacylglycerophosphocholine\"").contains("PC"));
        assertTrue(search(CLASS_NAME_FIELD + ":\"Hexose Ceramide\"").contains("HexCer"));
    }

    /**
     * The rewrite is an offer, not a restriction: a word of a long name still matches word by word, since the
     * field is analyzed and is one of the fields a query without a field name searches.
     */
    @Test
    public void testWordSearchIsUnaffected() {
        assertTrue(search(CLASS_NAME_FIELD + ":betaine").contains("DGTS"));
    }

    /**
     * What the rewrite cannot fix, stated so nobody expects otherwise: the index holds the words of a name, so
     * an abbreviation whose long name is a word of another long name matches both. MGDG and DGDG go further and
     * share one long name, which no query can tell apart.
     */
    @Test
    public void testKnownImprecisionOfWordBasedMatching() {
        List<String> ceramides = search(CLASS_NAME_FIELD + ":Cer");
        assertTrue(ceramides.contains("Cer"));
        assertTrue(ceramides.contains("HexCer"), "'Ceramide' is a word of 'Hexose Ceramide': " + ceramides);

        assertEquals(LipidClass.MGDG.longName(), LipidClass.DGDG.longName());
        List<String> glycosyl = search(CLASS_NAME_FIELD + ":MGDG");
        assertTrue(glycosyl.containsAll(List.of("MGDG", "DGDG")), "indistinguishable by name: " + glycosyl);
    }

    @Test
    public void testTextThatIsNoAbbreviationIsLeftAlone() {
        assertTrue(search(CLASS_NAME_FIELD + ":caffeine").isEmpty());
    }

    // ---- what is offered ------------------------------------------------------------------------------------

    /**
     * The long names, and only those: an abbreviation is not in the index, so offering it would hand a client a
     * value that its own query has to be rewritten to work.
     */
    @Test
    public void testOnlyTheIndexedLongNamesAreOffered() {
        List<String> offered = vocabulary.getPossibleValues(CLASS_NAME_FIELD);

        assertNotNull(offered);
        assertTrue(offered.contains("Diacylglycerophosphocholine"));
        assertTrue(offered.contains("Hexose Ceramide"));
        for (LipidClass lipidClass : LipidClass.values())
            assertFalse(offered.contains(lipidClass.abbr()),
                    "abbreviations match but are not offered: " + lipidClass.abbr());
    }

    /**
     * MGDG and DGDG share a long name, so the list must not offer it twice.
     */
    @Test
    public void testOfferedNamesAreFreeOfDuplicates() {
        List<String> offered = vocabulary.getPossibleValues(CLASS_NAME_FIELD);

        assertEquals(offered.stream().distinct().toList(), offered);
        assertEquals(LipidClass.values().length - 1, offered.size(),
                "one long name is shared by two classes");
    }

    @Test
    public void testLipidMapsIdsAreOffered() {
        List<String> offered = vocabulary.getPossibleValues(LIPID_MAPS_FIELD);

        List<String> expected = Arrays.stream(LipidClass.values())
                .map(LipidClass::getLipidMapsId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        assertEquals(expected, offered, "the ids of the classes that have one, and nothing else");
    }

    @Test
    public void testOtherFieldsAreNotAnswered() {
        assertNull(vocabulary.getPossibleValues("topAnnotations.formulaAnnotation.lipidAnnotation.lipidSpecies"));
        assertNull(vocabulary.getPossibleValues("name"));
    }

    /**
     * The values offered must be the ones indexed - the test that would have caught a vocabulary drifting from
     * the mapper that writes it.
     */
    @Test
    public void testOfferedValuesAreTheIndexedOnes() {
        List<String> offeredNames = vocabulary.getPossibleValues(CLASS_NAME_FIELD);
        List<String> offeredIds = vocabulary.getPossibleValues(LIPID_MAPS_FIELD);

        for (LipidClass lipidClass : LipidClass.values()) {
            assertTrue(offeredNames.contains(lipidClass.longName()),
                    lipidClass + " is indexed as '" + lipidClass.longName() + "' but not offered");
            if (lipidClass.getLipidMapsId() != null)
                assertTrue(offeredIds.contains(lipidClass.getLipidMapsId()),
                        lipidClass + " is indexed with id " + lipidClass.getLipidMapsId() + " but not offered");
        }
    }

    // ---- what the lipid marker says it is ------------------------------------------------------------------

    /**
     * {@code lipid} is written by the mapper as true and only for features that have a lipid annotation, so it
     * is a flag rather than text: the value carries nothing, only its presence does. The index cannot say that
     * - a mapper-contributed field is a keyword as far as lucene is concerned - so the field says it, and a
     * client that is told BOOLEAN with one possible value knows it is looking at a flag.
     */
    @Test
    public void testTheLipidMarkerIsDescribedAsTheFlagItIs() {
        Map<String, SearchableField> fields = DescribedFields.asMap(AlignedFeature.class);

        SearchableField lipid = fields.get("topAnnotations.formulaAnnotation.lipidAnnotation.lipid");
        assertEquals(SearchableField.FieldType.BOOLEAN, lipid.getFieldType());
        assertEquals(List.of("true"), lipid.getPossibleValues());
        assertFalse(lipid.isFullTextSearch());
    }

    /**
     * The type is stated per field, so the other fields the same mapper writes keep the type they have.
     */
    @Test
    public void testTheOtherLipidFieldsStayText() {
        Map<String, SearchableField> fields = DescribedFields.asMap(AlignedFeature.class);

        for (String field : List.of("lipidClassName", "lipidMapsId", "lipidSpecies"))
            assertEquals(SearchableField.FieldType.TEXT,
                    fields.get("topAnnotations.formulaAnnotation.lipidAnnotation." + field).getFieldType(), field);
    }
}
