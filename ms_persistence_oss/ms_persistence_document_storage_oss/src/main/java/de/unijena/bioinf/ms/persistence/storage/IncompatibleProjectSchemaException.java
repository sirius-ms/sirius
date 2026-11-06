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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A project was written by a SIRIUS whose project-data schema this version cannot read - see
 * {@link ProjectSchemaVersion#canRead}. Thrown before anything is written to the project, so the project is left
 * exactly as the newer version wrote it.
 */
@Getter
public class IncompatibleProjectSchemaException extends IOException {

    private final transient ProjectSchemaVersion recorded;
    private final transient ProjectSchemaVersion readable;

    public IncompatibleProjectSchemaException(@NotNull String location, @NotNull ProjectSchemaVersion recorded,
                                              @NotNull ProjectSchemaVersion readable) {
        super("Project '" + location + "' was written by a newer SIRIUS (project schema " + recorded
                + ") and cannot be opened by this version, which reads project schemas up to "
                + readable.major() + ".x. Please open it with a newer SIRIUS.");
        this.recorded = recorded;
        this.readable = readable;
    }
}
