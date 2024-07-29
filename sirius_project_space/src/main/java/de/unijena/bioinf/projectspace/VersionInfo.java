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

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.PSLocations.VERSION;

public class VersionInfo implements ProjectSpaceProperty {

    public final String siriusVersion;

    public VersionInfo(String siriusVersion) {
        this.siriusVersion = siriusVersion;
    }

    public static VersionInfo fromKeyValuePairs(Map<String, String> keyValues) {
        String string = keyValues.get("siriusVersion");
        if (string != null && !string.isBlank())
            return new VersionInfo(string);
        return null;
    }

    public static Map<String, String> toKeyValuePairs(VersionInfo versionInfo) {
        HashMap<String, String> map = new HashMap<>();
        if (versionInfo == null)
            return map;
        if (versionInfo.siriusVersion != null && !versionInfo.siriusVersion.isBlank())
            map.put("siriusVersion", versionInfo.siriusVersion);
        return map;
    }

    static class Serializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, VersionInfo> {

        @Override
        public @Nullable VersionInfo read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            if (reader.exists(VERSION))
                return VersionInfo.fromKeyValuePairs(reader.keyValues(VERSION));
            return null;
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<VersionInfo> optProp) throws IOException {
            if (optProp.isPresent()) {
                writer.deleteIfExists(VERSION);
                writer.keyValues(VERSION, VersionInfo.toKeyValuePairs(optProp.get()));
            }
        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.deleteIfExists(VERSION);
        }
    }
}
