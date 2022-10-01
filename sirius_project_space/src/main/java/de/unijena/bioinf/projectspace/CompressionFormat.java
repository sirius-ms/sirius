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

import de.unijena.bioinf.ChemistryBase.utils.ZipCompressionMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.PSLocations.COMPRESSION;

public class CompressionFormat implements ProjectSpaceProperty {
    public final int[] compressionLevels;
    @NotNull
    public final ZipCompressionMethod compressionMethod;

    public CompressionFormat(int[] compressionLevels, @NotNull ZipCompressionMethod method) {
        this.compressionMethod = method;
        this.compressionLevels = compressionLevels == null ? new int[0] : compressionLevels;
        Arrays.sort(this.compressionLevels);

    }

    public int[] compressionLevels() {
        return compressionLevels;
    }

    public boolean hasNoLevels() {
        return compressionLevels.length == 0;
    }

    public int getCompressedLevel() {
        if (hasNoLevels())
            return -1;
        return compressionLevels[compressionLevels.length - 1];
    }

    public ZipCompressionMethod getRootCompression() {
        return getCompressedLevel() < 0 ? compressionMethod : ZipCompressionMethod.STORED;
    }

    public ZipCompressionMethod getCompression(int level) {
        return level == getCompressedLevel() ? compressionMethod : ZipCompressionMethod.STORED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompressionFormat)) return false;
        CompressionFormat format = (CompressionFormat) o;
        return Arrays.equals(compressionLevels, format.compressionLevels) && compressionMethod == format.compressionMethod;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(compressionMethod);
        result = 31 * result + Arrays.hashCode(compressionLevels);
        return result;
    }

    public static CompressionFormat fromKeyValuePairs(Map<String, String> pairs) {
        String string = pairs.get("compressionLevels");
        int[] compressionLevels = null;
        if (string != null && !string.isBlank())
            compressionLevels = Arrays.stream(string.split("\\s*,\\s*")).mapToInt(Integer::parseInt).toArray();
        string = pairs.get("compressionMethod");
        ZipCompressionMethod compressionMethod = string == null || string.isBlank() ? ZipCompressionMethod.DEFLATED : ZipCompressionMethod.valueOf(string);
        return new CompressionFormat(compressionLevels, compressionMethod);
    }

    public static Map<String, String> toKeyValuePairs(CompressionFormat format) {
        HashMap<String, String> map = new HashMap<>();
        if (format == null)
            return map;
        if (format.compressionLevels != null)
            map.put("compressionLevels", Arrays.stream(format.compressionLevels).mapToObj(String::valueOf).collect(Collectors.joining(",")));
        map.put("compressionMethod", format.compressionMethod.name());
        return map;
    }

    public static CompressionFormat of(String compressionLevels, String compressionMethod) {
        HashMap<String, String> map = new HashMap<>();
        if (compressionLevels != null)
            map.put("compressionLevels", compressionLevels);

        if (compressionMethod != null)
            map.put("compressionMethod", compressionMethod);

        return fromKeyValuePairs(map);
    }

    static class Serializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CompressionFormat> {
        @Override
        @Nullable
        public CompressionFormat read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            if (reader.exists(COMPRESSION))
                return CompressionFormat.fromKeyValuePairs(reader.keyValues(COMPRESSION));

            return null;
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CompressionFormat> optProp) throws IOException {
            if (optProp.isPresent()) {
                writer.deleteIfExists(COMPRESSION);
                writer.keyValues(COMPRESSION, CompressionFormat.toKeyValuePairs(optProp.get()));
            }
        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.deleteIfExists(COMPRESSION);
        }
    }
}
