/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.projects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

@Getter
@Builder
public final class ProjectInfo {
    @Schema(enumAsRef = true, name = "ProjectInfoOptField", nullable = true)
    public enum OptField {none, compatibilityInfo, sizeInformation}

    /**
     * a user selected unique name of the project for easy access.
     */
    public final @NotNull String projectId;
    /**
     * storage location of the project.
     */
    public final @NotNull String location;

    /**
     * Description of this project.
     */
    @Schema(nullable = true)
    public final @Nullable String description;


    //compatibilityCheck
    /**
     * Indicates whether computed results (e.g. fingerprints, compounds classes) are compatible with the backend.
     * If true project is up-to-date and there are no restrictions regarding usage.
     * If false project is incompatible and therefore "read only" until the incompatible results have been removed. See updateProject endpoint for further information
     * If NULL the information has not been requested.
     */
    @Schema(nullable = true)
    public final @Nullable Boolean compatible;

    //sizeInformation
    /**
     * Number of features (aligned over runs) in this project. If NULL, information has not been requested (See OptField 'sizeInformation').
     */
    @Schema(nullable = true)
    public final @Nullable Integer numOfFeatures;
    /**
     * Number of compounds (group of ion identities) in this project. If NULL, Information has not been requested (See OptField 'sizeInformation') or might be unavailable for this project type.
     */
    @Schema(nullable = true)
    public final @Nullable Integer numOfCompounds;
    /**
     * Size in Bytes this project consumes on disk If NULL, Information has not been requested (See OptField 'sizeInformation').
     */
    @Schema(nullable = true)
    public final @Nullable Long numOfBytes;



    @JsonIgnore
    public Path getAsPath() {
        return Path.of(getLocation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectInfo that = (ProjectInfo) o;
        return projectId.equals(that.projectId) &&
                location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, location);
    }

    public static ProjectInfo of(String projectId, String location) {
        return ProjectInfo.builder().projectId(projectId).location(location).build();
    }
}
