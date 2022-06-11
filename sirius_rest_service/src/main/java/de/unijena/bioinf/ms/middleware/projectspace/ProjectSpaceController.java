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
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "SIRIUS Project-Spaces", description = "Manage project-spaces.")
public class ProjectSpaceController extends BaseApiController {

    @Autowired
    public ProjectSpaceController(SiriusContext context) {
        super(context);
    }

    @Operation(summary = "List all opened project spaces.")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectSpaceId> getProjectSpaces() {
        return context.listAllProjectSpaces();
    }


    @Operation(summary = "Get project space info by its name.")
    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId getProjectSpace(@PathVariable String name) {
        //todo add infos like size and number of compounds?
        return context.getProjectSpace(name).map(x -> new ProjectSpaceId(name, x.projectSpace().getLocation())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + name + "'"));
    }

    @Operation(summary = "Open open existing project-space and make it accessible via the given name.")
    @PutMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId openProjectSpace(@PathVariable String name, @RequestParam String pathToProject) throws IOException {
        return context.openProjectSpace(new ProjectSpaceId(name, Path.of(pathToProject)));
    }

    @Operation(summary = "Create and open new project-space at given location and make it accessible via the given name.")
    @PostMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectSpaceId createProjectSpace(@PathVariable String name, @RequestParam String pathToProject) throws IOException {
        return context.createProjectSpace(name, Path.of(pathToProject));
    }

    @Operation(summary = "Close project-space and remove it from application. Project-space will NOT be deleted from disk.")
    @DeleteMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String name) throws IOException {
        context.closeProjectSpace(name);

    }


}
