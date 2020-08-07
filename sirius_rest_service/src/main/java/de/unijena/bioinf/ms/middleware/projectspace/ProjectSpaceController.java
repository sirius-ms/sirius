/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.projectspace;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping(value = "/api/projects")
public class ProjectSpaceController extends BaseApiController {

    @Autowired
    public ProjectSpaceController(SiriusContext context) {
        super(context);
    }

    @GetMapping(value = "")
    public List<ProjectSpaceId> getProjectSpaces() {
        return context.listAllProjectSpaces();
    }

    @GetMapping(value = "/{name}")
    public ProjectSpaceId getProjectSpace(@PathVariable String name) {
        return context.getProjectSpace(name).map(x -> new ProjectSpaceId(name, x.getRootPath())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no project space with name '" + name + "'"));
    }

    @PutMapping(value = "/{name}")
    public ProjectSpaceId openProjectSpace(@PathVariable String name, @RequestParam(required = true) Path path) throws IOException {
        return context.openProjectSpace(new ProjectSpaceId(name, path));
    }

    @PostMapping(value = "/new")
    public ProjectSpaceId openProjectSpace(@RequestParam(required = true) Path path) throws IOException {
        final String name = path.getFileName().toString();
        return context.ensureUniqueName(name, (newName)-> {
            try {
                return context.openProjectSpace(new ProjectSpaceId(newName,path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
