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

package de.unijena.bioinf.ms.middleware.model.compute;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Selection of formats that are supported by the data importers.
 */
@Schema(enumAsRef = true, nullable = true)
public enum ImportFormat {
    MS("ms"),
    MGF("mgf"),
    MZML("mzml"),
    MZXML("mzxml"),
    CEF("cef"),
    MSP("msp"),
    MAT("mat"),
    MASSBANK("txt");

    private final String ext;

    ImportFormat(String ext) {
        this.ext = ext;
    }

    public String getExtension() {
        return ext;
    }
}
