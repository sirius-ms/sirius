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

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Zip4JProjectSpaceIO implements ProjectIO {
    protected final ZipFile zipLocation;
    private final ReadWriteLock rwLock;
    protected Path dir = null;
    protected final Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter;

    protected Zip4JProjectSpaceIO(ZipFile location, ReadWriteLock rwLock, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        this.zipLocation = location;
        this.rwLock = rwLock;
        this.propertyGetter = propertyGetter;
    }

    @Override
    public List<String> list(String globPattern, final boolean recursive, final boolean includeFiles, final boolean includeDirs) throws IOException {
        return withReadLock(() -> {
            Stream<String> s = zipLocation.getFileHeaders().stream()
                    .filter(h -> (includeDirs && h.isDirectory()) || (includeFiles && !h.isDirectory()))
                    .map(FileHeader::getFileName)
                    .filter(n -> dir == null || n.startsWith(dir.toString())); // filter for correct sub dir

            if (!recursive)
                s = s.filter(n -> { //filter for flat list.
                    Path p = Path.of(n);
                    if (dir != null)
                        p = dir.relativize(p);
                    return p.equals(p.getFileName());
                });

            if (globPattern != null && !globPattern.equals("*")) { //check if we can skip glob stuff (match all)
                // do glob filtering by regex conversion
                final Pattern matcher = FileUtils.compileGlobToRegex(globPattern);
                s = s.filter(n -> matcher.matcher(n).matches());
            }

            return s.collect(Collectors.toList());
        });
    }


    @Override
    public boolean exists(String relativePath) throws IOException {
        return withReadLock(() -> zipLocation.getFileHeader(resolve(relativePath).toString()) != null);
    }

    @Override //no log because it is not persistent
    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass) {
        return (Optional<A>) propertyGetter.apply((Class<ProjectSpaceProperty>) klass);
    }

    @Override
    public <T> T inDirectory(String relativePath, IOFunctions.IOCallable<T> ioAction) throws IOException {
        return withReadLock(() -> {
            final Path newDir = resolve(relativePath);
            final Path oldDir = dir;
            try {
                dir = newDir;
                return ioAction.call();
            } finally {
                dir = oldDir;
            }
        });
    }


    protected Path resolve(String relativePath) {
        if (dir == null)
            return Path.of(relativePath);
        return dir.resolve(relativePath);

    }

    protected FileHeader resolveHeader(String relativePath) throws ZipException {
        return zipLocation.getFileHeader(resolve(relativePath).toString());
    }

    @Override
    public URI asURI(String path) {
        return URI.create("jar:file:" + URLEncoder.encode(zipLocation.getFile().getAbsolutePath(), StandardCharsets.UTF_8) + "!/" + URLEncoder.encode(resolve(path).toString(), StandardCharsets.UTF_8));
    }

    protected void withReadLock(IOFunctions.IORunnable doWithLock) throws IOException {
        rwLock.readLock().lock();
        try {
            doWithLock.run();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    protected <R> R withReadLock(IOFunctions.IOCallable<R> doWithLock) throws IOException {
        rwLock.readLock().lock();
        try {
            return doWithLock.call();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    protected void withWriteLock(IOFunctions.IORunnable doWithLock) throws IOException {
        rwLock.writeLock().lock();
        try {
            doWithLock.run();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    protected <R> R withWriteLock(IOFunctions.IOCallable<R> doWithLock) throws IOException {
        rwLock.writeLock().lock();
        try {
            return doWithLock.call();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
