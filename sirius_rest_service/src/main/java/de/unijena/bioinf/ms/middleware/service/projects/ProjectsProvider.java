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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public interface ProjectsProvider<P extends Project> extends DisposableBean {

    List<ProjectInfo> listAllProjectSpaces();

    default P getProjectOrThrow(String projectId) throws ResponseStatusException {
        return getProject(projectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project with name '" + projectId + "'"));
    }

    Optional<P> getProject(String projectId);

    Optional<ProjectInfo> getProjectInfo(@NotNull String projectId, @NotNull EnumSet<ProjectInfo.OptField> optFields);

    default ProjectInfo getProjectInfoOrThrow(String projectId, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws ResponseStatusException {
        return getProjectInfo(projectId, optFields).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + projectId + "'"));
    }

    ProjectInfo openProject(@NotNull String projectId, @Nullable String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException;

    ProjectInfo createTempProject(@NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException;

    default ProjectInfo createProject(String projectIdSuggestion, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        return createProject(projectIdSuggestion, null, optFields);
    }

    default ProjectInfo createProject(@NotNull String projectIdSuggestion, @Nullable String location, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException{
        return createProject(projectIdSuggestion, location, optFields, true);
    }

    ProjectInfo createProject(@NotNull String projectIdSuggestion, @Nullable String location, @NotNull EnumSet<ProjectInfo.OptField> optFields, boolean failIfExists) throws IOException;

    boolean containsProject(@NotNull String projectId);

    void closeProjectSpace(String projectId) throws IOException;

    @Deprecated
    default ProjectInfo copyProject(@NotNull String projectId, @NotNull String pathToProject, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        return copyProject(projectId, pathToProject, null, optFields);
    }

    @Deprecated
    ProjectInfo copyProject(@NotNull String projectId, @NotNull String pathToProject, @Nullable String copyId, @NotNull EnumSet<ProjectInfo.OptField> optFields) throws IOException;

    void closeAll();

    default String ensureUniqueProjectId(String nameSuggestion) {
        if (!containsProject(nameSuggestion))
            return nameSuggestion;
        int app = 2;
        nameSuggestion = nameSuggestion.replaceAll("_[0-9]+$", "");
        while (true) {
            final String n = nameSuggestion + "_" + app++;
            if (!containsProject(n))
                return n;
        }
    }

    Pattern projectIdValidator = Pattern.compile("[a-zA-Z0-9_-]+", Pattern.CASE_INSENSITIVE);
    default String validateId(String projectId){
        if (!projectIdValidator.matcher(projectId).matches())
            throw new IllegalArgumentException("Illegal ProjectId. ProjectId must only contain [a-zA-Z0-9_-]+!");
        return projectId;
    }
}