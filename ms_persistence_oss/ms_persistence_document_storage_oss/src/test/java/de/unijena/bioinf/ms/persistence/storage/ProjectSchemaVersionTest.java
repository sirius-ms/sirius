/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What a project-data schema version says about which SIRIUS can read a project: every version of the same or an
 * older major version can, whatever the minor version, and none of a newer one.
 */
class ProjectSchemaVersionTest {

    @Test
    void aVersionReadsEveryMinorVersionOfItsOwnMajorVersion() {
        ProjectSchemaVersion reader = new ProjectSchemaVersion(0, 4);
        assertTrue(reader.canRead(new ProjectSchemaVersion(0, 3)));
        assertTrue(reader.canRead(new ProjectSchemaVersion(0, 7)), "a newer minor version is still readable");
    }

    @Test
    void aVersionReadsOlderMajorVersionsAndRefusesNewerOnes() {
        ProjectSchemaVersion reader = new ProjectSchemaVersion(1, 0);
        assertTrue(reader.canRead(new ProjectSchemaVersion(0, 4)));
        assertFalse(reader.canRead(new ProjectSchemaVersion(2, 0)));
        assertFalse(new ProjectSchemaVersion(0, 4).canRead(new ProjectSchemaVersion(1, 0)));
    }

    @Test
    void versionsAreOrderedByMajorVersionFirst() {
        assertTrue(new ProjectSchemaVersion(0, 999).isOlderThan(new ProjectSchemaVersion(1, 0)));
        assertTrue(new ProjectSchemaVersion(1, 0).isOlderThan(new ProjectSchemaVersion(1, 1)));
        assertFalse(new ProjectSchemaVersion(1, 0).isOlderThan(new ProjectSchemaVersion(1, 0)));
        assertEquals("1.1", new ProjectSchemaVersion(1, 1).toString());
    }

    @Test
    void theNumbersRecordedBeforeThereWereTwoPartsAreMinorVersionsOfZero() {
        assertEquals(new ProjectSchemaVersion(0, 3), ProjectSchemaVersion.decode(3), "what SIRIUS 6.5.4 records");
        assertEquals(new ProjectSchemaVersion(0, 0), ProjectSchemaVersion.decode(0));
    }

    @Test
    void theStoredIntegerHoldsBothPartsInTheirOrder() {
        assertEquals(1000, new ProjectSchemaVersion(1, 0).encoded());
        assertEquals(1002, new ProjectSchemaVersion(1, 2).encoded());
        assertEquals(new ProjectSchemaVersion(2, 17), ProjectSchemaVersion.decode(new ProjectSchemaVersion(2, 17).encoded()));
        assertTrue(new ProjectSchemaVersion(0, 999).encoded() < new ProjectSchemaVersion(1, 0).encoded());
    }

    @Test
    void aVersionIsNotNegativeAndItsMinorVersionFitsTheStoredInteger() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectSchemaVersion(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProjectSchemaVersion(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new ProjectSchemaVersion(1, 1000));
        assertThrows(IllegalArgumentException.class, () -> ProjectSchemaVersion.decode(-1));
    }
}
