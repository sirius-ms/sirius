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

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;


import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
/**
 * NOT TREAD SAFE
 */
public interface ProjectIO {

    /**
     * Returns a list of all files in the current directory, filtered by the given globPattern
     *
     * @param globPattern a glob-like pattern for the file.
     * @return list of files in the current directory that match globPattern
     * @throws IOException if io error occurs
     */
    public List<String> list(String globPattern, final boolean recursive, final boolean includeFiles, final boolean includeDirs) throws IOException;

    public default List<String> list(String globPattern) throws IOException {
        return list(globPattern, false, true, true);
    }

    public default List<String> listDirs(String globPattern) throws IOException {
        return list(globPattern, false, false, true);
    }

    public default List<String> listFiles(String globPattern) throws IOException {
        return list(globPattern, false, true, false);
    }

    public default List<String> listRecursive(String globPattern) throws IOException {
        return list(globPattern, true, true, true);
    }

    public default List<String> listDirsRecursive(String globPattern) throws IOException {
        return list(globPattern, true, false, true);
    }

    public default List<String> listFilesRecursive(String globPattern) throws IOException {
        return list(globPattern, true, true, false);
    }

    public boolean exists(String relativePath) throws IOException;

    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass);

    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction) throws IOException;

    public URI asURI(String path);
}
