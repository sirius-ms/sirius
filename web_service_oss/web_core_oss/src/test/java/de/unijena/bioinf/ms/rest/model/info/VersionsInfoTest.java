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

package de.unijena.bioinf.ms.rest.model.info;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * maven-artifact is used for its version parsing only, and plexus-utils is excluded from it, so that it is not
 * shipped. This checks that the version parsing does not need it.
 */
class VersionsInfoTest {

    @Test
    void plexusUtilsIsNotOnTheClasspath() {
        // the precondition of the test below: without it, that test proves nothing
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.codehaus.plexus.util.StringUtils"));
    }

    @Test
    void versionsParseAndCompareWithoutPlexusUtils() {
        VersionsInfo info = new VersionsInfo("6.6.0-SNAPSHOT", "2026-10-01", false);
        assertEquals(6, info.siriusGuiVersion.getMajorVersion());
        assertEquals(6, info.siriusGuiVersion.getMinorVersion());
        assertEquals("SNAPSHOT", info.siriusGuiVersion.getQualifier());

        assertTrue(info.siriusGuiVersion.compareTo(new DefaultArtifactVersion("6.6.0")) < 0, "a snapshot precedes its release");
        assertTrue(new DefaultArtifactVersion("6.10.0").compareTo(new DefaultArtifactVersion("6.9.3")) > 0, "versions compare numerically");

        assertTrue(VersionsInfo.areMinorEqual(new DefaultArtifactVersion("6.6.1"), new DefaultArtifactVersion("6.6.0")));
        assertFalse(VersionsInfo.areIncrementalEqual(new DefaultArtifactVersion("6.6.1"), new DefaultArtifactVersion("6.6.0")));
        assertFalse(VersionsInfo.areMajorEqual(new DefaultArtifactVersion("5.8.6"), new DefaultArtifactVersion("6.6.0")));
    }
}
