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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ProjectIO {

    /**
     * Returns a list of all files in the current directory, filtered by the given globPattern
     * @param globPattern a glob-like pattern for the file. Does not support sub-directories!!!
     * @return list of files in the current directory that match globPattern
     * @throws IOException if io error occurs
     */
    public List<String> list(String globPattern) throws IOException;

    public boolean exists(String relativePath) throws IOException;

    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass);

    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction) throws IOException;

    public default URL asURL(String path) {
        try {
            return asPath(path).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Path asPath(String path);

}
