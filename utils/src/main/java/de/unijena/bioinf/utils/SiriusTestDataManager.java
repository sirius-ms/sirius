/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.utils;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class SiriusTestDataManager implements Closeable, AutoCloseable {

    private static final String RES_ROOT = "com/brightgiant/resources";

    private FileSystem fileSystem;
    private final Path root;

    public SiriusTestDataManager() throws URISyntaxException, IOException {
        URL url = SiriusTestDataManager.class.getClassLoader().getResource(RES_ROOT);

        if (url != null) {
            URI uri = url.toURI();
            if (uri.getScheme().equals("jar")) {
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                root = fileSystem.getPath(RES_ROOT);
            } else {
                root = Path.of(uri);
            }
        } else {
            root = null;
        }

    }

    @Override
    public void close() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
        }
    }

    public List<String> getDataSets() throws IOException {
        if (root == null)
            return List.of();
        return walk(root, walk -> walk
                .filter(Files::isDirectory)
                .filter(path -> !path.endsWith(root))
                .map(Path::getFileName)
                .map(Path::toString).toList(), 1);
    }

    public List<Path> getPaths(String dataset) throws IOException {
        if (root == null)
            return List.of();
        Path dsRoot = root.resolve(dataset);
        return walk(dsRoot, walk -> walk
                .filter(path -> !path.endsWith(dsRoot))
                .map(Path::toAbsolutePath)
                .toList());
    }

    public List<URI> getURIs(String dataset) throws IOException {
        if (root == null)
            return List.of();
        Path dsRoot = root.resolve(dataset);
        return walk(dsRoot, walk -> walk
                .filter(path -> !path.endsWith(dsRoot))
                .map(Path::toUri)
                .toList());
    }

    private static <T> T walk(@Nullable Path path, Function<Stream<Path>, T> function) throws IOException {
        return walk(path, function, Integer.MAX_VALUE);
    }

    private static <T> T walk(@Nullable Path path, Function<Stream<Path>, T> function, int maxDepth) throws IOException {
        try (Stream<Path> walk = Files.walk(path, maxDepth)) {
            return function.apply(walk);
        }
    }

}
