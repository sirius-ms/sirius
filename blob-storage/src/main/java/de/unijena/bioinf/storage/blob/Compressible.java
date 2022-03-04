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

package de.unijena.bioinf.storage.blob;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public interface Compressible {
    String TAG_COMPRESSION="compression";

    enum Compression {
        NONE(""), GZIP(".gz"), XZ(".xz"), LZ4(".lz4"), BZIP2(".bz2");
        public final String ext;

        Compression(@NotNull String ext) {
            this.ext = ext;
        }

        public String ext() {
            return ext;
        }

        public boolean matches(String name) {
            return ext.substring(1).equalsIgnoreCase(name);
        }

        public static @NotNull Compression fromPath(@NotNull Path p){
            return fromName(p.toString());
        }
        public static @NotNull Compression fromName(@NotNull String s){
            s = s.toLowerCase();
            if (s.endsWith(GZIP.ext()))
                return GZIP;
            if (s.endsWith(XZ.ext()))
                return XZ;
            if (s.endsWith(LZ4.ext()))
                return LZ4;
            if (s.endsWith(BZIP2.ext()))
                return BZIP2;
            return NONE;
        }
    }

    boolean isDecompressStreams();

    void setDecompressStreams(boolean decompress);

    @NotNull Compression getCompression();


    // STATIC helpers
    static <T extends InputStream> Optional<T> wrap(@Nullable InputStream rawResource, @NotNull IOFunctions.IOFunction<InputStream, T> wrap) throws IOException {
        if (rawResource == null)
            return Optional.empty();
        return Optional.of(wrap.apply(rawResource));
    }


    static boolean checkStreamCompression(@NotNull final InputStream rawStream, @NotNull final Compression compression) throws IOException {
        try {
            return compression.matches(CompressorStreamFactory.detect(rawStream));
        } catch (CompressorException e) {
            throw new IOException(e);
        }
    }

    static Optional<InputStream> decompressRawStream(@Nullable final InputStream rawStream, @NotNull final Compression compression, final boolean decompress) throws IOException {
        if (!decompress)
            return Optional.ofNullable(rawStream);
        return decompressRawStream(rawStream, compression);
    }

    static Optional<InputStream> decompressRawStream(@Nullable final InputStream rawStream, @NotNull final Compression compression) throws IOException {
        switch (compression) {
            case GZIP:
                return wrap(rawStream, GZIPInputStream::new);
            case XZ:
                return wrap(rawStream, XZCompressorInputStream::new);
            case LZ4:
                return wrap(rawStream, FramedLZ4CompressorInputStream::new);
            case BZIP2:
                return wrap(rawStream, BZip2CompressorInputStream::new);
            default:
                return Optional.ofNullable(rawStream);
        }
    }

    static InputStream toCompressedStream(@NotNull final IOFunctions.IOConsumer<OutputStream> streamConsumer, @NotNull final Compression compression) throws IOException {
        byte[] buffer;
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            streamConsumer.accept(compress(s,compression));
            buffer = s.toByteArray();
        }
        return new ByteArrayInputStream(buffer);
    }

    static void withCompression(@NotNull OutputStream toCompress, @NotNull Compression compression, @NotNull IOFunctions.IOConsumer<OutputStream> withCompressed) throws IOException {
        try (OutputStream compressed = Compressible.compress(toCompress, compression)) {
            withCompressed.accept(compressed);
        }
    }


    static OutputStream compress(@NotNull OutputStream out, @NotNull Compression compression) throws IOException {
        switch (compression) {
            case GZIP:
                return new GzipCompressorOutputStream(out);
            case XZ:
                return new XZCompressorOutputStream(out);
            case LZ4:
                return new FramedLZ4CompressorOutputStream(out);
            case BZIP2:
                return new BZip2CompressorOutputStream(out);
            default:
                return out;
        }
    }
}
