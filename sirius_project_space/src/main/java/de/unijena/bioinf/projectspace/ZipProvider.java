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

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public enum ZipProvider {
    ZIP_FS, ZIP_FS_TMP, ZIP4J, ZIP4JVM;

    public static ProjectIOProvider<?, ?, ?> newInstance(@NotNull Path location, @Nullable String providerType) {
        return newInstance(location, providerType == null ? null : ZipProvider.valueOf(providerType));
    }

    public static ProjectIOProvider<?, ?, ?> newInstance(@NotNull Path location, @Nullable ZipProvider providerType) {
        if (providerType == null) {
            LoggerFactory.getLogger(ZipProvider.class).debug("Zip Provider is NULL, using ZipFS with in-memory Cache as default.");
            providerType = ZIP_FS;
        }
        switch (providerType) {
            case ZIP_FS:
                return new ZipFSProjectSpaceIOProvider(location, false);
            case ZIP_FS_TMP:
                return new ZipFSProjectSpaceIOProvider(location, true);
            case ZIP4J:
                return new Zip4JProjectSpaceIOProvider(location);
            case ZIP4JVM:
                return new Zip4jvmProjectSpaceIOProvider(location);
            default: {
                LoggerFactory.getLogger(ZipProvider.class).debug("Unknown Zip Provider using ZipFS with in-memory Cache as fallback!");
                return new ZipFSProjectSpaceIOProvider(location, false);
            }
        }
    }

    public static CompressionFormat getDefaultCompressionFormat() {
        return CompressionFormat.of(
                PropertyManager.getProperty("de.unijena.bioinf.sirius.zipfs.compressionLevels", null, "1"),
                PropertyManager.getProperty("de.unijena.bioinf.sirius.zipfs.compression")
        );
    }
}
