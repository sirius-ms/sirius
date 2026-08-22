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

import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFormat;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A persisted index is only loaded when it was built by the code that is about to read it.
 * <p>
 * The check the project could already make was about its DATA - has the project moved on without the index. This
 * one is about the CODE: a field renamed or added, or values written by rules that have since changed, leave an
 * index whose documents are shaped for questions nobody asks any more. Nothing in the project says so, so the
 * index has to say it itself.
 */
public class IndexRebuildOnCodeChangeTest {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;
        @IndexField(name = "name", fullTextSearch = true, defaultSearchField = true)
        public String name;
    }

    private static NoSQLProjectImpl mockProject(String id) {
        NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
        Mockito.when(project.getProjectId()).thenReturn(id);
        return project;
    }

    /** Writes an index for one document and returns the file the project lives in. */
    private static Path projectWithIndexedDocument(String name) throws Exception {
        Path dbFile = Files.createTempFile("index-signature", ".db");
        Metadata md = MsProjectDocumentDatabase.buildMetadata();
        try (NitriteDatabase warm = new NitriteDatabase(dbFile, md)) {
            warm.flush();
        }
        try (NitriteDatabase db = new NitriteDatabase(dbFile, md)) {
            SearchService service = new SearchServiceImpl(p -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap()));
            service.openOrCreateProjectIndex(mockProject(name));
            service.addDocument(name, new TestPojo("1", "alpha"));
            service.close();
        }
        return dbFile;
    }

    /** Opens the project again and reports whether the stored index survived the open. */
    private static boolean indexSurvivesOpen(Path dbFile, String name) throws Exception {
        Metadata md = MsProjectDocumentDatabase.buildMetadata();
        try (NitriteDatabase db = new NitriteDatabase(dbFile, md)) {
            SearchService service = new SearchServiceImpl(p -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap()));
            service.openOrCreateProjectIndex(mockProject(name));
            // touching the index is what triggers the restore
            boolean empty = service.isEmpty(name, TestPojo.class);
            service.close();
            return !empty;
        }
    }

    @Test
    public void testAnIndexIsRecordedWithWhatBuiltIt() throws Exception {
        Path dbFile = projectWithIndexedDocument("recorded");
        try (NitriteDatabase db = new NitriteDatabase(dbFile, MsProjectDocumentDatabase.buildMetadata())) {
            PersistentSearchIndex stored = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).orElseThrow();
            assertEquals(IndexFormat.VERSION, stored.getFormatVersion());
            assertNotNull(stored.getSchemaFingerprint(), "without it the next open cannot tell whether it fits");
            assertTrue(stored.matches(IndexFormat.VERSION, stored.getSchemaFingerprint()));
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testAnIndexBuiltByTheSameCodeIsLoaded() throws Exception {
        Path dbFile = projectWithIndexedDocument("same");
        try {
            assertTrue(indexSurvivesOpen(dbFile, "same"),
                    "nothing changed, so rebuilding would be waste");
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testAnIndexBuiltForAnotherSchemaIsDiscarded() throws Exception {
        Path dbFile = projectWithIndexedDocument("other-schema");
        try {
            // what a field rename leaves behind: the same documents, a shape that no longer matches
            try (NitriteDatabase db = new NitriteDatabase(dbFile, MsProjectDocumentDatabase.buildMetadata())) {
                PersistentSearchIndex stored = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).orElseThrow();
                stored.setSchemaFingerprint("0".repeat(64));
                db.upsert(stored);
            }
            assertFalse(indexSurvivesOpen(dbFile, "other-schema"),
                    "an index for another schema answers the wrong questions - it has to be rebuilt");
            try (NitriteDatabase db = new NitriteDatabase(dbFile, MsProjectDocumentDatabase.buildMetadata())) {
                assertTrue(db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).isEmpty(),
                        "and the stale record must go with it, or every open would rebuild again");
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testAnIndexBuiltByAnotherFormatIsDiscarded() throws Exception {
        Path dbFile = projectWithIndexedDocument("other-format");
        try {
            // the shape is unchanged, but the values were written by rules that have since changed
            try (NitriteDatabase db = new NitriteDatabase(dbFile, MsProjectDocumentDatabase.buildMetadata())) {
                PersistentSearchIndex stored = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).orElseThrow();
                stored.setFormatVersion(IndexFormat.VERSION - 1);
                db.upsert(stored);
            }
            assertFalse(indexSurvivesOpen(dbFile, "other-format"));
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testAnIndexFromBeforeTheCheckIsDiscarded() throws Exception {
        Path dbFile = projectWithIndexedDocument("legacy");
        try {
            // what every existing project looks like on the first open after this shipped: no version, no
            // fingerprint. Rebuilt once, which is the only safe answer for an index of unknown provenance.
            try (NitriteDatabase db = new NitriteDatabase(dbFile, MsProjectDocumentDatabase.buildMetadata())) {
                PersistentSearchIndex stored = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class).orElseThrow();
                stored.setFormatVersion(0);
                stored.setSchemaFingerprint(null);
                db.upsert(stored);
            }
            assertFalse(indexSurvivesOpen(dbFile, "legacy"));
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }
}
