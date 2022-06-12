/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.projectspace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

@Getter
public final class ProjectSpaceId {

    public final @NotNull String name;
    public final @NotNull String path;

    /*public ProjectSpaceId(@NotNull  String name, @NotNull Path path) {
            this()
    }*/
    public ProjectSpaceId(@NotNull String name, @NotNull String path) {
        this.name = name;
        this.path = path;
    }


    @JsonIgnore
    public Path getAsPath() {
        return Path.of(getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectSpaceId that = (ProjectSpaceId) o;
        return name.equals(that.name) &&
                path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }

    public static ProjectSpaceId of(String projectId, Path location) {
        return new ProjectSpaceId(projectId, location.toAbsolutePath().toString());
    }
}
