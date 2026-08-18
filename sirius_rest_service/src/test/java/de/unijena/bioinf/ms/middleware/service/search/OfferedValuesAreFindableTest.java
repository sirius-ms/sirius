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

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClass;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.description.DetectedAdductPossibleValues;
import de.unijena.bioinf.ms.middleware.service.search.description.IndexFacts;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import de.unijena.bioinf.ms.middleware.service.search.description.TagDefinitionDocs;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.model.sirius.ComputedSubtools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A value that is offered has to be findable. Offering one that a query for it does not match is worse than
 * offering none: the completion hands the user a value, the user picks it, and the filter comes back empty.
 * <p>
 * This is the general form of the check the adduct, compound class and lipid tests each do for themselves: it
 * walks every field the description reports a vocabulary for, indexes an object carrying one of the offered
 * values, and searches for it. A field that gains a vocabulary and is not covered here fails the test rather
 * than slipping through - which is the part that keeps this honest as fields are added.
 */
public class OfferedValuesAreFindableTest {

    /** A tag of every value type, so the tag fields are covered too. */
    private static final Map<String, ValueType> TAG_TYPES = new TreeMap<>(Map.of(
            "sampleType", ValueType.TEXT,
            "replicate", ValueType.INTEGER,
            "measured", ValueType.DATE,
            "isBlank", ValueType.BOOLEAN,
            "pfas", ValueType.NONE));

    private static final Map<String, TagDefinition> TAG_DEFINITIONS = Map.of(
            "sampleType", tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank")),
            "replicate", tagDefinition("replicate", ValueType.INTEGER, List.of(1, 2, 3)),
            "measured", tagDefinition("measured", ValueType.DATE, List.of(0L)),
            "isBlank", tagDefinition("isBlank", ValueType.BOOLEAN, List.of()),
            "pfas", tagDefinition("pfas", ValueType.NONE, List.of()));

    private static final Set<String> DETECTED_ADDUCTS = Set.of("[M + H]+", "[M + Na]+");

    /**
     * The value each field is given in the indexed objects below, and therefore the value searched for. Every
     * one of them must be a value the field offers - which is asserted, so this map cannot drift into testing
     * something the client would never be handed.
     */
    private static final Map<String, String> INDEXED_VALUE = new LinkedHashMap<>() {{
        put("quality", "GOOD");
        put("hasMs1", "true");
        put("hasMsMs", "true");
        put("detectedAdducts", "[M + H]+");
        put("qualities.peakShape", "DECENT");
        put("qualities.isotopePattern", "BAD");
        put("computedTools.librarySearch", "true");
        put("computedTools.formulaSearch", "true");
        put("computedTools.zodiac", "true");
        put("computedTools.fingerprint", "true");
        put("computedTools.canopus", "true");
        put("computedTools.structureSearch", "true");
        put("computedTools.deNovoSearch", "true");
        put("topAnnotations.compoundClassAnnotation.cfClass", "Carboxylic acids and derivatives");
        put("topAnnotations.compoundClassAnnotation.npcPathway", "Alkaloids");
        put("topAnnotations.compoundClassAnnotation.npcSuperclass", "Alkylresorsinols");
        put("topAnnotations.compoundClassAnnotation.npcClass", "2-arylbenzofurans");
        put("topAnnotations.formulaAnnotation.lipidAnnotation.lipid", "true");
        put("topAnnotations.formulaAnnotation.lipidAnnotation.lipidClassName", "Hexose Ceramide");
        put("topAnnotations.formulaAnnotation.lipidAnnotation.lipidMapsId", "LMGL01010000");
        put("tags.sampleType", "Sample");
        put("tags.replicate", "2");
        put("tags.measured", "1970-01-01");
        put("tags.isBlank", "true");
        put("tags.pfas", "true");
    }};

    private PerPojoSearchContext context;
    private SearchableFieldService searchableFields;

    @BeforeEach
    public void setup() {
        context = new PerPojoSearchContext(null, TAG_TYPES);
        searchableFields = new SearchableFieldService(IndexFacts.of(context),
                new DetectedAdductPossibleValues(() -> DETECTED_ADDUCTS),
                new TagDefinitionDocs(name -> Optional.ofNullable(TAG_DEFINITIONS.get(name))));

        context.addDocument(feature());
        context.addDocument(run());
    }

    @AfterEach
    public void cleanup() throws IOException {
        context.close(true);
    }

    // ---- the objects that carry every offered value ---------------------------------------------------------

    private static AlignedFeature feature() {
        return AlignedFeature.builder()
                .alignedFeatureId("1")
                .quality(DataQuality.GOOD)
                .hasMs1(true)
                .hasMsMs(true)
                .detectedAdducts(Set.of("[M + H]+"))
                .qualities(Map.of("peakShape", DataQuality.DECENT, "isotopePattern", DataQuality.BAD))
                .computedTools(ComputedSubtools.builder()
                        .librarySearch(true).formulaSearch(true).zodiac(true).fingerprint(true)
                        .canopus(true).structureSearch(true).deNovoSearch(true)
                        .build())
                .topAnnotations(FeatureAnnotations.builder()
                        .compoundClassAnnotation(compoundClasses())
                        .formulaAnnotation(FormulaCandidate.builder()
                                .lipidAnnotation(LipidAnnotation.builder()
                                        .lipidSpecies("HexCer 34:1")
                                        .lipidClassName("Hexose Ceramide")
                                        .lipidMapsId("LMGL01010000")
                                        .build())
                                .build())
                        .build())
                .tags(tags())
                .build();
    }

    private static CompoundClasses compoundClasses() {
        CompoundClasses classes = new CompoundClasses();
        classes.setClassyFireLineage(List.of(named("Carboxylic acids and derivatives")));
        classes.setNpcPathway(named("Alkaloids"));
        classes.setNpcSuperclass(named("Alkylresorsinols"));
        classes.setNpcClass(named("2-arylbenzofurans"));
        return classes;
    }

    private static CompoundClass named(String name) {
        return CompoundClass.builder().name(name).build();
    }

    private static Map<String, Tag> tags() {
        Map<String, Tag> tags = new LinkedHashMap<>();
        tags.put("sampleType", Tag.builder().tagName("sampleType").value("Sample").build());
        tags.put("replicate", Tag.builder().tagName("replicate").value(2).build());
        // a tag carries its value in the form the API uses, which for a date is the formatted one
        tags.put("measured", Tag.builder().tagName("measured").value("1970-01-01").build());
        tags.put("isBlank", Tag.builder().tagName("isBlank").value(true).build());
        tags.put("pfas", Tag.builder().tagName("pfas").value(null).build());
        return tags;
    }

    private static Run run() {
        return Run.builder().runId("1").name("blank-01").tags(tags()).build();
    }

    // ---- the property ---------------------------------------------------------------------------------------

    @Test
    public void testEveryOfferedValueFindsTheObjectCarryingIt() {
        assertOfferedValuesAreFindable(AlignedFeature.class, "1");
        assertOfferedValuesAreFindable(Run.class, "1");
    }

    private <T> void assertOfferedValuesAreFindable(Class<T> modelClass, String expectedId) {
        List<String> uncovered = new ArrayList<>();
        int checked = 0;

        for (SearchableField field : searchableFields.describe(modelClass)) {
            if (field.getPossibleValues() == null)
                continue;

            String value = INDEXED_VALUE.get(field.getName());
            if (value == null) {
                uncovered.add(field.getName());
                continue;
            }
            assertTrue(field.getPossibleValues().contains(value),
                    "the value this test indexes for " + field.getName() + " is not one the field offers: " + value);

            assertEquals(List.of(expectedId),
                    context.searchIds(query(field, value), PageRequest.of(0, 10), modelClass).getContent(),
                    () -> "searching " + query(field, value) + " must find the "
                            + modelClass.getSimpleName() + " that carries it");
            checked++;
        }

        assertTrue(uncovered.isEmpty(), modelClass.getSimpleName() + " offers values for fields this test does "
                + "not cover - index one of their values and add them to INDEXED_VALUE: " + uncovered);
        assertFalse(checked == 0, "no field of " + modelClass.getSimpleName() + " was checked");
    }

    /**
     * The query a client would send for a value it was offered: quoted when it contains whitespace, which is
     * what the GUI's query compiler does with it too.
     */
    private static String query(SearchableField field, String value) {
        return value.contains(" ")
                ? field.getName() + ":\"" + value + "\""
                : field.getName() + ":" + value;
    }

    private static TagDefinition tagDefinition(String tagName, ValueType valueType, List<?> possibleValues) {
        return TagDefinition.builder()
                .tagName(tagName)
                .tagType("TEST")
                .valueDefinition(new ValueDefinition<>(valueType, possibleValues, null, null))
                .build();
    }
}
