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
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;

public class Zip4JProjectSpaceWriter extends Zip4JProjectSpaceIO implements ProjectWriter {
    protected Zip4JProjectSpaceWriter(ZipFile location, ReadWriteLock rwLock, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(location, rwLock, propertyGetter);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        withWriteLock(() -> addToZip(relativePath, out -> {
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out))) {
                func.accept(bw);
            }
        }));
    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<OutputStream> func) throws IOException {
        withWriteLock(() -> addToZip(relativePath, func::accept));
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        withWriteLock(() -> addToZip(relativePath, out -> {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out))) {
                FileUtils.writeKeyValues(w, map);
            }
        }));
    }

    @Override
    public void table(String relativePath, @Nullable String[] header, Iterable<String[]> rows) throws IOException {
        withWriteLock(() -> {
            addToZip(relativePath, out -> {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out))) {
                    FileUtils.writeTable(bw, header, rows);
                }
            });
        });
    }

    @Override
    public void delete(String relativePath) throws IOException {
        withWriteLock(() -> {
            FileHeader header = resolveHeader(relativePath);
            if (header != null)
                zipLocation.removeFile(header);
        });
    }

    @Override
    public void deleteIfExists(String relativePath) throws IOException {
        delete(relativePath);
    }

    @Override
    public void move(String directoryName, String newDirName) throws IOException {
        withWriteLock(() -> zipLocation.renameFile(resolveHeader(directoryName), resolve(newDirName).toString()));
    }

    protected void addToZip(String relativePath, IOFunctions.IOConsumer<ByteArrayOutputStream> doWrite) throws IOException {
        byte[] data;
        try (ByteArrayOutputStream tmpOut = new ByteArrayOutputStream()) {
            doWrite.accept(tmpOut);
            data = tmpOut.toByteArray();
        }
        ZipParameters paras = new ZipParameters();
        paras.setFileNameInZip(resolve(relativePath).toString());
        paras.setOverrideExistingFilesInZip(true);
        zipLocation.addStream(new ByteArrayInputStream(data), paras);
    }
}
