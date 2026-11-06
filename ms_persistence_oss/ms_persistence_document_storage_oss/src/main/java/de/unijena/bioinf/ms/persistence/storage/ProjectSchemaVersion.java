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

import org.jetbrains.annotations.NotNull;

/**
 * The version of the project-data schema a project conforms to, as {@code major.minor}.
 * <p>
 * The two numbers answer different questions. The major version says which SIRIUS versions can read a project:
 * a project is readable by every version that knows its major version, and a version must refuse a project of a
 * newer major version rather than show it half understood. The minor version counts the conversions within one
 * major version: a minor step fills in or corrects data that older versions of the same major version read just
 * as well, they merely do not produce it themselves.
 * <p>
 * Both are stored as one integer, {@code major * 1000 + minor} (see {@link #encoded()}), under the key every
 * released SIRIUS reads as the version. So a version is written in one step, and the plain numbers projects
 * recorded before there were two parts read as what they were: {@code 0.N}, conversions without any promise of
 * compatibility, which no version refused.
 */
public record ProjectSchemaVersion(int major, int minor) implements Comparable<ProjectSchemaVersion> {

    /** How many minor versions one major version holds in the stored integer. */
    private static final int MINOR_VERSIONS = 1000;

    public ProjectSchemaVersion {
        if (major < 0 || minor < 0 || minor >= MINOR_VERSIONS)
            throw new IllegalArgumentException("Not a schema version: " + major + "." + minor);
    }

    /** The version a project records as the given integer - see {@link #encoded()}. */
    public static ProjectSchemaVersion decode(int encoded) {
        if (encoded < 0)
            throw new IllegalArgumentException("Not a schema version: " + encoded);
        return new ProjectSchemaVersion(encoded / MINOR_VERSIONS, encoded % MINOR_VERSIONS);
    }

    /** The integer a project records for this version, ordered as the versions are. */
    public int encoded() {
        return major * MINOR_VERSIONS + minor;
    }

    /**
     * Whether a SIRIUS that writes this version can read a project recorded at the given one. It can for every
     * major version up to its own, newer minor versions of its own major version included.
     */
    public boolean canRead(@NotNull ProjectSchemaVersion recorded) {
        return recorded.major <= major;
    }

    public boolean isOlderThan(@NotNull ProjectSchemaVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(@NotNull ProjectSchemaVersion other) {
        return major != other.major ? Integer.compare(major, other.major) : Integer.compare(minor, other.minor);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
