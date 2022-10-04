/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.ms.middleware.gui;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.gui.model.GuiParameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/gui")
@Tag(name = "Graphical user Interface", description = "Open, control and close SIRIUS GUI on the specified project-space.")
public class GuiController extends BaseApiController {

    public GuiController(SiriusContext context) {
        super(context);
    }

    /**
     * Open GUI instance on specified project-space and bring the GUI window to foreground.
     *
     * @param readOnly  open in read-only mode.
     * @param projectId of project-space the GUI instance will connect to.
     */
    @Deprecated
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public void openGui(@PathVariable String projectId, @RequestParam(required = false, defaultValue = "true") boolean readOnly) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "NOT YET IMPLEMENTED");
    }

    /**
     * Apply given changes to the running GUI instance.
     *
     * @param projectId     of project-space the GUI instance is connected to.
     * @param guiParameters parameters that should be applied.
     */
    @Deprecated
    @PatchMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public void applyToGui(@PathVariable String projectId, @RequestBody GuiParameters guiParameters) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "NOT YET IMPLEMENTED");
    }

    /**
     * Close GUI instance of given project-space if available.
     *
     * @param projectId if project-space the GUI instance is connected to.
     */
    @Deprecated
    @DeleteMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeGui(@PathVariable String projectId) {
        boolean isOpen = false;
        if (isOpen) {
            //todo close
            throw new ResponseStatusException(HttpStatus.OK, "Gui instance on '" + projectId + "' successfully closed.");
        } else {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No running Gui instance on '" + projectId + "'. Nothing to do.");
        }
    }
}
