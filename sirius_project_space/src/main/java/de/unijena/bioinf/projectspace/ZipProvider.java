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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public enum ZipProvider {
    ZIP_FS, ZIP_FS_TMP, ZIP4J, ZIP4JVM;

    public static ProjectIOProvider<?, ?, ?> newInstance(@NotNull Path location, @Nullable String providerType) {
        return newInstance(location, providerType == null ? null : ZipProvider.valueOf(providerType));
    }

    public static ProjectIOProvider<?, ?, ?> newInstance(@NotNull Path location, @Nullable ZipProvider providerType) {
        if (providerType == null) {
            LoggerFactory.getLogger(ZipProvider.class).warn("Zip Provider is NULL, using ZipFS with Memory Cache as fallback!");
            providerType = ZIP_FS;
        }
        boolean createNew = Files.notExists(location);
        switch (providerType) {
            case ZIP_FS:
                return new ZipFSProjectSpaceIOProvider(location, createNew, false);
            case ZIP_FS_TMP:
                return new ZipFSProjectSpaceIOProvider(location, createNew, true);
            case ZIP4J:
                return new Zip4JProjectSpaceIOProvider(location);
            case ZIP4JVM:
                return new Zip4jvmProjectSpaceIOProvider(location);
            default: {
                LoggerFactory.getLogger(ZipProvider.class).debug("Unknown Zip Provider using ZipFS with Memory Cache as fallback!");
                return new ZipFSProjectSpaceIOProvider(location, createNew, false);
            }
        }
    }
}
