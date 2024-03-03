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

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.frontend.subtools.InputResource;
import de.unijena.bioinf.ms.frontend.subtools.StringInputResource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * Parameter Object to submit a job that imports ms/ms data from the given format into the specified project
 * Supported formats (ms, mgf, cef, msp, mzML, mzXML)
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportStringSubmission extends AbstractImportSubmission {

    /**
     * Name that specifies the data source. Can e.g. be a file path  or just a name.
     */
    @Nullable
    @Schema(nullable = true)
    protected String sourceName;
    /**
     * Data format used in the data field.
     */
    @NotNull
    protected ImportFormat format;
    /**
     * Data content in specified format
     */
    @NotNull
    protected String data;

    @JsonIgnore
    public List<InputResource<?>> asInputResource(){
        return List.of(new StringInputResource(data, sourceName, format.getExtension()));
    }
}
