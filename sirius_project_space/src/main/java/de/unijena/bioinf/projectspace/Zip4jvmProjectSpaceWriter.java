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
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import ru.olegcherednik.zip4jvm.UnzipIt;
import ru.olegcherednik.zip4jvm.ZipFile;
import ru.olegcherednik.zip4jvm.ZipIt;
import ru.olegcherednik.zip4jvm.ZipMisc;
import ru.olegcherednik.zip4jvm.exception.EntryNotFoundException;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;

public class Zip4jvmProjectSpaceWriter extends Zip4jvmProjectSpaceIO implements ProjectWriter {
    protected Zip4jvmProjectSpaceWriter(Path location, ReadWriteLock rwLock, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
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
        withWriteLock(() -> Zip4jvmProjectSpaceWriter.this.addToZip(relativePath, out -> {
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out))) {
                FileUtils.writeTable(bw, header, rows);
            }
        }));
    }

    @Override
    public void delete(String relativePath) throws IOException {
        withWriteLock(() -> {
            try {
                ZipMisc.zip(root).removeEntryByName(resolve(relativePath).toString());
            } catch (EntryNotFoundException e) {
                e.printStackTrace();//todo remove
               //ignore
            }
        });
    }

    @Override
    public void deleteIfExists(String relativePath) throws IOException {
        delete(relativePath);
    }

    @Override
    public void move(String directoryName, String newDirName) throws IOException {
        withWriteLock(() -> {

            byte[] bytes;
            try (InputStream in = UnzipIt.zip(root).stream(resolve(directoryName).toString());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()){
                 IOUtils.copyLarge(in, out);
                 bytes = out.toByteArray();
            }

            try (ZipFile.Writer zipFile = ZipIt.zip(root).open()){
                zipFile.add(ZipFile.Entry.builder()
                        .inputStreamSupplier(() -> new ByteArrayInputStream(bytes))
                        .fileName(resolve(newDirName).toString())
                        .uncompressedSize(bytes.length).build());
            }

            ZipMisc.zip(root).removeEntryByName(resolve(directoryName).toString());
        });
    }

    protected void addToZip(String relativePath, IOFunctions.IOConsumer<ByteArrayOutputStream> doWrite) throws IOException {
        byte[] data;
        try (ByteArrayOutputStream tmpOut = new ByteArrayOutputStream()) {
            doWrite.accept(tmpOut);
            data = tmpOut.toByteArray();
        }

        try (ZipFile.Writer zipFile = ZipIt.zip(root).open()){
            zipFile.add(ZipFile.Entry.builder()
                    .inputStreamSupplier(() -> new ByteArrayInputStream(data))
                    .fileName(resolve(relativePath).toString())
                    .uncompressedSize(data.length).build());
        }
    }
}