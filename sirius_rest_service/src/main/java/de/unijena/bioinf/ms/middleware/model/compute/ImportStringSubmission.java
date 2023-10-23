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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Parameter Object to submit a job that imports ms/ms data from the given format into the specified project
 * Supported formats (ms, mgf, cef, msp, mzML, mzXML)
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ImportStringSubmission extends ImportSubmission {
    public enum Format {
        MS("ms"),
        MGF("mgf"),
        MZML("mzml"),
        MZXML("mzxml"),
        CEF("cef"),
        MSP("msp"),
        MAT("mat"),
        MASSBANK("txt");

        private final String ext;

        Format(String ext) {
            this.ext = ext;
        }

        public String getExtension() {
            return ext;
        }
    }

    /**
     * Name that specifies the data source. Can e.g. be a file path  or just a name.
     */
    @Nullable
    protected String sourceName;
    /**
     * Data format used in the data field.
     */
    @NotNull
    protected Format format;
    /**
     * Data content in specified format
     */
    @NotNull
    protected String data;
}
