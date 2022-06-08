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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileSystemManager extends Closeable {

    void writeFile(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException;

    void writeFile(@Nullable String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException;

    void delete(String relative, boolean recursive) throws IOException;

    boolean exists(@Nullable String relative) throws IOException;

    void readFile(@Nullable String relative, IOFunctions.IOConsumer<Path> readWithFS) throws IOException;

    <R> R readFile(@Nullable String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException;

    <R> R withDir(@Nullable String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException;

    List<String> list(@Nullable String relative, @Nullable String globPattern, final boolean recursive, final boolean includeFiles, final boolean includeDirs) throws IOException;

    /**
     * Location of the managed filesystem/directory in the default filesystem (path of the default filesystem)
     * @return location of the managed resource
     */
    Path getLocation();
    /**
     * root path of the managed filesystem/directory (path of the manged filesystem)
     * @return root path of the managed resource
     */
    Path getRoot();

    default void flush() throws IOException{}

    @NotNull
    CompressionFormat getCompressionFormat();

    void setCompressionFormat(@Nullable CompressionFormat format);
}