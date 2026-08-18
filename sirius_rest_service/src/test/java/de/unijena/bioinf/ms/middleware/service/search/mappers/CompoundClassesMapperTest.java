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

package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClass;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The predicted compound classes come from two fixed ontologies (ClassyFire/ChemOnt and NPC). Their names are
 * searchable but not guessable - nobody types "Carboxylic acids and derivatives" from memory - so each level
 * offers its ontology as possible values.
 */
public class CompoundClassesMapperTest {

    private static final String ROOT = "topAnnotations.compoundClassAnnotation";

    private static final CompoundClassesMapper MAPPER = new CompoundClassesMapper();

    private static List<String> valuesOf(String field) {
        return MAPPER.getPossibleValues(ROOT + "." + field);
    }

    @Test
    public void testNpcPathwaysAreOffered() {
        assertEquals(List.of("Alkaloids", "Amino acids and Peptides", "Carbohydrates", "Fatty acids",
                        "Polyketides", "Shikimates and Phenylpropanoids", "Terpenoids"),
                valuesOf("npcPathway"));
    }

    /**
     * Each NPC level offers exactly its own level - together they account for the whole ontology, so no level is
     * mixed up with another and none is missing.
     */
    @Test
    public void testEveryNpcPropertyIsOfferedOnItsLevel() {
        List<String> pathways = valuesOf("npcPathway");
        List<String> superclasses = valuesOf("npcSuperclass");
        List<String> classes = valuesOf("npcClass");

        assertTrue(superclasses.contains("Alkylresorsinols"), "superclasses: " + superclasses.size());
        assertTrue(classes.contains("2-arylbenzofurans"), "classes: " + classes.size());

        assertEquals(NPCFingerprintVersion.get().size(),
                pathways.size() + superclasses.size() + classes.size(),
                "every NPC property must be offered on exactly one level");
    }

    @Test
    public void testClassyFireOntologyIsOffered() {
        List<String> cfClasses = valuesOf("cfClass");

        assertEquals(ClassyFireFingerprintVersion.getDefault().size(), cfClasses.size());
        assertTrue(cfClasses.contains("Organic compounds"), "must contain the ChemOnt root");
        assertTrue(cfClasses.contains("Carboxylic acids and derivatives"));
    }

    /**
     * The values are only useful if they are exactly the terms that end up in the index - otherwise a client
     * offers a value that then matches nothing. Index a feature classified on every level and check that each
     * indexed term is offered for its field.
     */
    @Test
    public void testOfferedValuesAreTheIndexedTerms() {
        NPCFingerprintVersion npc = NPCFingerprintVersion.get();
        ClassyFireFingerprintVersion classyFire = ClassyFireFingerprintVersion.getDefault();

        CompoundClasses classes = new CompoundClasses();
        // one property per NPC level, found by level rather than by index so the test does not encode the layout
        classes.setNpcPathway(CompoundClass.of(npcPropertyOf(npc, NPCFingerprintVersion.NPCLevel.PATHWAY), 0.9, 0));
        classes.setNpcSuperclass(CompoundClass.of(npcPropertyOf(npc, NPCFingerprintVersion.NPCLevel.SUPERCLASS), 0.8, 1));
        classes.setNpcClass(CompoundClass.of(npcPropertyOf(npc, NPCFingerprintVersion.NPCLevel.CLASS), 0.7, 2));

        ClassyfireProperty cfLeaf = classyFire.getMolecularProperty(classyFire.size() - 1);
        classes.setClassyFireLineage(List.of(CompoundClass.of(cfLeaf, 0.9, 0)));
        classes.setClassyFireAlternatives(List.of(CompoundClass.of(classyFire.getMolecularProperty(0), 0.5, 1)));

        List<IndexableField> indexed = new ArrayList<>();
        MAPPER.toIndexableFields(ROOT, classes).forEach(indexed::add);

        assertFalse(indexed.isEmpty(), "test feature must produce indexed terms");
        indexed.forEach(field -> {
            List<String> offered = MAPPER.getPossibleValues(field.name());
            assertNotNull(offered, field.name() + " must offer possible values");
            assertTrue(offered.contains(field.stringValue()),
                    "indexed term '" + field.stringValue() + "' is not offered for " + field.name());
        });
    }

    private static NPCFingerprintVersion.NPCProperty npcPropertyOf(NPCFingerprintVersion npc,
                                                                   NPCFingerprintVersion.NPCLevel level) {
        for (int i = 0; i < npc.size(); i++)
            if (npc.getMolecularProperty(i).getLevel() == level)
                return npc.getMolecularProperty(i);
        throw new AssertionError("no NPC property on level " + level);
    }
}
