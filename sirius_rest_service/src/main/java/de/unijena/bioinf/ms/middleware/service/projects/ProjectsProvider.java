/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.projects.ProjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ProjectsProvider<P extends Project> extends DisposableBean {

    List<ProjectId> listAllProjectSpaces();

    default P getProjectOrThrow(String projectId) throws ResponseStatusException {
        return getProject(projectId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project with name '" + projectId + "'"));
    }

    Optional<P> getProject(String projectId);
    Optional<ProjectId> getProjectId(String projectId);

    default ProjectId getProjectIdOrThrow(String projectId) throws ResponseStatusException{
        return getProjectId(projectId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + projectId + "'"));
    }

    ProjectId openProjectSpace(@NotNull ProjectId id) throws IOException;

    ProjectId createProjectSpace(Path location) throws IOException;

    ProjectId createProjectSpace(@NotNull String nameSuggestion, @NotNull Path location) throws IOException;

    ProjectId createTemporaryProjectSpace() throws IOException;

    boolean containsProject(@NotNull String name);

    void closeProjectSpace(String name) throws IOException;

    void closeAll();

    default String ensureUniqueProjectName(String nameSuggestion) {
        if (!containsProject(nameSuggestion))
            return nameSuggestion;
        int app = 2;
        while (true) {
            final String n = nameSuggestion + "_" + app++;
            if (!containsProject(n))
                return n;
        }
    }
}