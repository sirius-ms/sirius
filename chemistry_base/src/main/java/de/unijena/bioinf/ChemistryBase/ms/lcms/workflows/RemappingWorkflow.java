/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.lcms.workflows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * map an existing project space to LC/MS files
 */
@JsonTypeName("map")
public class RemappingWorkflow extends LCMSWorkflow{

    private final String[] files;
    private final boolean dropExistingAssignments;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RemappingWorkflow(
            @JsonProperty("files") String[] files,
            @JsonProperty("drop-existing") boolean dropExistingAssignments) {
        this.files = files;
        this.dropExistingAssignments = dropExistingAssignments;
    }

    public String[] getFiles() {
        return files;
    }

    public boolean isDropExistingAssignments() {
        return dropExistingAssignments;
    }
}
