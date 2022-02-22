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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import static de.unijena.bioinf.projectspace.PSLocations.FORMAT;

public interface FilenameFormatter extends Function<Ms2Experiment, String> {
    String getFormatExpression();

    class PSProperty implements ProjectSpaceProperty {
        public final String formatExpression;

        public PSProperty(FilenameFormatter formatter) {
            formatExpression = formatter.getFormatExpression();
        }

        public PSProperty(String formatExpression) {
            this.formatExpression = formatExpression;
        }
    }

    class PSPropertySerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, PSProperty> {

        @Override
        public PSProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            if (reader.exists(FORMAT))
                return reader.textFile(FORMAT, br -> br.lines().findFirst().map(PSProperty::new).orElse(null));
            return null;
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<PSProperty> optProp) throws IOException {
            if (optProp.isPresent()) {
                writer.deleteIfExists(FORMAT);
                writer.textFile(FORMAT, bf -> bf.write(optProp.get().formatExpression));
            } else {
                LoggerFactory.getLogger(getClass()).warn("Could not find Project Space formatting information!");
            }

        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.deleteIfExists(FORMAT);
        }
    }
}
