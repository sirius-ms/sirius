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

package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the schema fingerprint has to notice, and what it must not.
 * <p>
 * It decides whether a persisted index is discarded on open, so both directions cost something: missing a change
 * leaves stale documents answering queries that no longer match them, while reacting to a change that does not
 * affect the index rebuilds every project for nothing.
 */
public class IndexSchemaFingerprintTest {

    private static String fingerprintOf(Class<?> pojoClass) {
        return GenericPojoMapper.schemaOf(pojoClass, new TagMapper(tagName -> null)).fingerprint();
    }

    public static class Base {
        @IndexField(documentId = true)
        String id;
        @IndexField(fullTextSearch = true, defaultSearchField = true)
        String name;
        @IndexField(sortable = true)
        double mass;
    }

    /** The same fields, declared in a different order. */
    public static class Reordered {
        @IndexField(sortable = true)
        double mass;
        @IndexField(fullTextSearch = true, defaultSearchField = true)
        String name;
        @IndexField(documentId = true)
        String id;
    }

    /** One field renamed - the case a persisted index cannot survive. */
    public static class Renamed {
        @IndexField(documentId = true)
        String id;
        @IndexField(name = "title", fullTextSearch = true, defaultSearchField = true)
        String name;
        @IndexField(sortable = true)
        double mass;
    }

    public static class WithExtraField extends Base {
        @IndexField
        boolean hasMsMs;
    }

    /** Same names, but the text field is no longer analyzed or searched without being named. */
    public static class Rekinded {
        @IndexField(documentId = true)
        String id;
        @IndexField
        String name;
        @IndexField(sortable = true)
        double mass;
    }

    @Test
    public void testTheSameSchemaAlwaysFingerprintsTheSame() {
        assertEquals(fingerprintOf(Base.class), fingerprintOf(Base.class));
        assertEquals(64, fingerprintOf(Base.class).length(), "a sha-256 digest in hex");
    }

    @Test
    public void testDeclarationOrderIsNotPartOfTheShape() {
        assertEquals(fingerprintOf(Base.class), fingerprintOf(Reordered.class),
                "the same fields in another order index the same documents - rebuilding for that is waste");
    }

    @Test
    public void testARenamedFieldChangesIt() {
        assertNotEquals(fingerprintOf(Base.class), fingerprintOf(Renamed.class),
                "documents written under the old name cannot answer a query naming the new one");
    }

    @Test
    public void testAnAddedFieldChangesIt() {
        assertNotEquals(fingerprintOf(Base.class), fingerprintOf(WithExtraField.class),
                "existing documents have nothing under the new field, so it would match nothing");
    }

    @Test
    public void testHowAFieldIsHeldChangesIt() {
        assertNotEquals(fingerprintOf(Base.class), fingerprintOf(Rekinded.class),
                "a keyword field does not answer the queries a text field answered");
    }

    @Test
    public void testTheProjectsTagsAreNotPartOfIt() {
        // a tag adds its own field as it is defined; if that changed the fingerprint, defining a tag would
        // look like a code change and rebuild the whole index
        String withoutTags = GenericPojoMapper.schemaOf(AlignedFeature.class, new TagMapper(tagName -> null))
                .fingerprint();
        String withTags = GenericPojoMapper.schemaOf(AlignedFeature.class,
                new TagMapper(tagName -> "sampleType".equals(tagName) ? ValueType.TEXT : null)).fingerprint();
        assertEquals(withoutTags, withTags);
    }

    @Test
    public void testTheRealModelHasAStableFingerprint() {
        // guards the plumbing on the model that is actually indexed: no exception, and repeatable
        assertEquals(fingerprintOf(AlignedFeature.class), fingerprintOf(AlignedFeature.class));
        assertNotEquals(fingerprintOf(AlignedFeature.class), fingerprintOf(Base.class));
    }
}
