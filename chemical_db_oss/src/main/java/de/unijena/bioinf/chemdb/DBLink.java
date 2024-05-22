

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/*
    A single link to a compound database with a database name
    and a database id
 */
@Getter
public final class DBLink {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Setter
    private @Nullable String name;
    private final @Nullable String id;

    public DBLink(@Nullable String name, @Nullable String id) {
        this.name = name;
        this.id = id;
        if (name == null && id == null)
            throw new IllegalArgumentException("Either id or name must be set.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DBLink dbLink = (DBLink) o;

        if (!Objects.equals(name, dbLink.name)) return false;
        return Objects.equals(id, dbLink.id);
    }

    @Override
    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if ((id == null || id.isBlank()) && (name == null || name.isBlank()))
            return "EMPTY";

        if (id != null && !id.isBlank() && name != null && !name.isBlank())
            return name + ":" + id;

        if (id != null && !id.isBlank())
            return id;

        return name;
    }
}