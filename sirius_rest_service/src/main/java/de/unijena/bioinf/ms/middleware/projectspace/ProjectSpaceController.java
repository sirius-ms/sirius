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

package de.unijena.bioinf.ms.middleware.projectspace;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.projectspace.model.ProjectSpaceId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping(value = "/api/projects")
@Tag(name = "Project-Spaces", description = "Manage SIRIUS project-spaces.")
public class ProjectSpaceController extends BaseApiController {
//todo add access to fingerprint definitions aka molecular property names

    @Autowired
    public ProjectSpaceController(SiriusContext context) {
        super(context);
    }

    /**
     * List all opened project spaces.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectSpaceId> getProjectSpaces() {
        return context.listAllProjectSpaces();
    }

    /**
     * Get project space info by its projectId.
     * @param projectId unique name/identifier tof the project-space to be accessed.
     */
    @GetMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId getProjectSpace(@PathVariable String projectId) {
        //todo add infos like size and number of compounds?
        return context.getProjectSpace(projectId).map(x -> new ProjectSpaceId(projectId, x.projectSpace().getLocation())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + projectId + "'"));
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     */
    @PutMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId openProjectSpace(@PathVariable String projectId, @RequestParam String pathToProject) throws IOException {
        return context.openProjectSpace(new ProjectSpaceId(projectId, Path.of(pathToProject)));
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     */
    @PostMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId createProjectSpace(@PathVariable String projectId, @RequestParam String pathToProject) throws IOException {
        return context.createProjectSpace(projectId, Path.of(pathToProject));
    }

    /**
     * Close project-space and remove it from application. Project-space will NOT be deleted from disk.
     * @param projectId unique name/identifier of the  project-space to be closed.
     */
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String projectId) throws IOException {
        context.closeProjectSpace(projectId);
    }
}
