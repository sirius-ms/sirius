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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.gui.GuiParameters;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/gui")
@Tag(name = "[Experimental] GUI",
        description = "Open, control and close SIRIUS Graphical User Interface (GUI) on the specified project-space.")
public class GuiController {

    private final ProjectsProvider projectsProvider;

    private final GuiService guiService;

    @Autowired
    public GuiController(ProjectsProvider<?> projectsProvider, GuiService guiService) {
        this.projectsProvider = projectsProvider;
        this.guiService = guiService;
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     *
     * @param readOnly  open in read-only mode.
     * @param projectId of project-space the GUI instance will connect to.
     */
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void openGui(@PathVariable String projectId, @RequestBody(required = false) GuiParameters guiParameters, @RequestParam(required = false, defaultValue = "true") boolean readOnly) {
        guiService.createGuiInstance(projectId, guiParameters);
    }

    /**
     * Apply given changes to the running GUI instance.
     *
     * @param projectId     of project-space the GUI instance is connected to.
     * @param guiParameters parameters that should be applied.
     */
    @PatchMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public void applyToGui(@PathVariable String projectId, @RequestBody GuiParameters guiParameters) {
        guiService.applyToGuiInstance(projectId, guiParameters);
    }

    /**
     * Close GUI instance of given project-space if available.
     *
     * @param projectId if project-space the GUI instance is connected to.
     */
    @DeleteMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeGui(@PathVariable String projectId) {
        guiService.closeGuiInstance(projectId);
    }
}
