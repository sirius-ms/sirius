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

package de.unijena.bioinf.ms.persistence.model.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import one.microstream.reference.Lazy;

import java.nio.file.Path;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class SourceFile {
    enum Type{
        //todo do we need more info here?
        MZML("Open MzML format"), MZXML("Open MzXML format (Deprecated)");

        public final String fullName;

        Type(String fullName) {
            this.fullName = fullName;
        }
    }


    /**
     * path of this source file
     */
    private Path filePath;

    public String getFileName(){
        return Optional.ofNullable(filePath).map(f -> f.getFileName().toString()).orElse(null);
    }

    /**
     * File Type, needed for parsing.
     */
    private Type type;
    /**
     * File contents stored as gzipped byte stream
     */
    private Lazy<byte[]> data;
}
