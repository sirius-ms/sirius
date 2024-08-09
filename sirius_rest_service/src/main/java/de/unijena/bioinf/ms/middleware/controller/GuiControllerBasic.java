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

import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "Gui", description = "Basic GUI Control: Open and close SIRIUS Graphical User Interface (GUI) on specified projects.")
@ConditionalOnExpression("!${sirius.middleware.controller.gui.advanced:false} && !${de.unijena.bioinf.sirius.headless:false}")
public class GuiControllerBasic extends GuiController {

    public GuiControllerBasic(ProjectsProvider<?> projectsProvider, GuiService guiService) {
        super(projectsProvider, guiService);
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     * @param projectId of project-space the GUI instance will connect to.
     */
    @PostMapping(value = "/api/projects/{projectId}/gui", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void openGui(@PathVariable String projectId) {
        guiService.createGuiInstance(projectId, null);
    }
}
