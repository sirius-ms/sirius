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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class FileBasedProjectSpaceIO implements ProjectIO {


    protected Path dir;
    protected final Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter;

    public FileBasedProjectSpaceIO(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        this.dir = dir;
        this.propertyGetter = propertyGetter;
    }

    @Override
    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass) {
        return (Optional<A>)propertyGetter.apply((Class<ProjectSpaceProperty>)klass);
    }


    @Override
    public List<String> list(String globPattern)  throws IOException {
        final ArrayList<String> content = new ArrayList<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, globPattern)) {
            for (Path p : stream)
                content.add(dir.relativize(p).toString());
        }
        return content;
    }


    @Override
    public boolean exists(String relativePath) {
        return Files.exists(asPath(relativePath));
    }


    @Override
    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction)  throws IOException {
        final Path newDir = asPath(relativePath);
        final Path oldDir = dir;
        try {
            dir = newDir;
            return ioAction.call();
        } finally {
            dir = oldDir;
        }
    }

    @Override
    public Path asPath(String relativePath) {
        return dir.resolve(relativePath);
    }
}
