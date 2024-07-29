/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import java.nio.file.Path;
import java.util.function.Function;

public class Location<ID extends ProjectSpaceContainerId> {
    final String relativePath;
    final String fileExt;
    final Function<ID, String> filename;


    public Location(String relativePath, String fileExt, Function<ID, String> filenameFunction) {
        this.relativePath = relativePath;
        this.filename = filenameFunction;
        this.fileExt = fileExt;
    }

    public String fileExt() {
        return fileExt;
    }

    public String fileExtDot() {
        return "." + fileExt();
    }

    public String fileName(ID id) {
        return filename.apply(id) + fileExtDot();
    }

    public String relFilePath(ID id) {
        return Path.of(relativePath).resolve(fileName(id)).toString();
    }

    public String relDir() {
        return relativePath;
    }
}
