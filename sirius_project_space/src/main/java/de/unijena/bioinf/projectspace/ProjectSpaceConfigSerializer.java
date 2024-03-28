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

import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class ProjectSpaceConfigSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, ProjectSpaceConfig> {
    @Override
    public ProjectSpaceConfig read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (reader.exists(SiriusLocations.COMPOUND_CONFIG)) {
            return reader.binaryFile(SiriusLocations.COMPOUND_CONFIG, s -> {
                try {
                    ParameterConfig c = PropertyManager.DEFAULTS.newIndependentInstance(s, ConfigType.PROJECT.name(),false, ConfigType.CLI.name());
                    return new ProjectSpaceConfig(c);
                } catch (ConfigurationException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when reading config for Compound with ID: " + id, e);
                    return null;
                }
            });
        }
        return null;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<ProjectSpaceConfig> optConf) throws IOException {
        if (optConf.isPresent())
            writer.textFile(SiriusLocations.COMPOUND_CONFIG, optConf.get().config::write);
        else
            LoggerFactory.getLogger("Could not find config/parameter info for this Compound: '" + id + "'. Project-Space will not contain parameter information");
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.deleteIfExists(SiriusLocations.COMPOUND_CONFIG);
    }
}
