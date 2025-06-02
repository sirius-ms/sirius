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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.gui.GuiInfo;
import de.unijena.bioinf.ms.middleware.model.gui.GuiParameters;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "Gui", description = "GUI Control: Open, control and close SIRIUS Graphical User Interface (GUI) on specified projects.")
@ConditionalOnExpression("!${de.unijena.bioinf.sirius.headless:false}")
public class GuiController {

    protected final ProjectsProvider projectsProvider;

    protected final GuiService guiService;

    @Autowired
    protected GuiController(ProjectsProvider<?> projectsProvider, GuiService guiService) {
        this.projectsProvider = projectsProvider;
        this.guiService = guiService;
    }

    /**
     * Get list of currently running gui windows, managed by this SIRIUS instance.
     * Note this will not show any Clients that are connected from a separate process!
     * @return List of GUI windows that are currently managed by this SIRIUS instance.
     */
    @GetMapping(value = "/api/guis", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<GuiInfo> getGuis() {
        return guiService.findGui();
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * @param projectId of project-space the GUI instance will connect to.
     */
    //todo enabled advanced setting here due to compatibility with explorer.
    // remove after sdk update
    @PostMapping(value = "/api/projects/{projectId}/gui", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void openGui(@PathVariable String projectId,
                                @RequestBody(required = false) GuiParameters guiParameters,
                                @RequestParam(required = false, defaultValue = "false") boolean readOnly
    ) {
        guiService.createGuiInstance(projectId, guiParameters);
    }

    /**
     * Close GUI instance of given project-space if available.
     *
     * @param projectId if project-space the GUI instance is connected to.
     */
    @DeleteMapping(value = "/api/projects/{projectId}/gui", produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean closeGui(@PathVariable String projectId, @RequestParam(required = false) boolean closeProject) throws IOException {
        boolean closed = guiService.closeGuiInstance(projectId);
        if (closeProject)
            projectsProvider.closeProjectSpace(projectId);
        return closed;
    }
}
