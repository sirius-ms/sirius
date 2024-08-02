/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataSource {

    public enum Format {
        UNKNOWN, JENA_MS, MASCOT_MGF, MZXML, MZML, AGILENT_CEF, NIST_MSP, MS_FINDER_MAT, MASSBANK, JSON
    }

    private String source;

    private Format format;


    public static DataSource fromPath(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();

        Format format = switch (ext) {
            case "mzml", "mzxml" -> Format.MZML;
            case "ms" -> Format.JENA_MS;
            case "mfg" -> Format.MASCOT_MGF;
            case "cef" -> Format.AGILENT_CEF;
            case "msp" -> Format.NIST_MSP;
            case "mat" -> Format.MS_FINDER_MAT;
            case "txt", "mblib", "mb" -> Format.MASSBANK;
            case "json" -> Format.JSON;
            default -> Format.UNKNOWN;
        };

        return DataSource.builder().source(path).format(format).build();

    }

}
