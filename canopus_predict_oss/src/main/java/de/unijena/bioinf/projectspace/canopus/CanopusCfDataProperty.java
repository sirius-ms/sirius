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

package de.unijena.bioinf.projectspace.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CF_CLIENT_DATA;
import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CF_CLIENT_DATA_NEG;

public class CanopusCfDataProperty extends PosNegFpProperty<ClassyFireFingerprintVersion, CanopusCfData> {
    public CanopusCfDataProperty(CanopusCfData positive, CanopusCfData negative) {
        super(positive, negative);
    }

    public static class Serializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CanopusCfDataProperty> {

        @Override
        public CanopusCfDataProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            boolean p = reader.exists(CF_CLIENT_DATA);
            boolean n = reader.exists(CF_CLIENT_DATA_NEG);
            if (!p && !n)
                return null;

            final CanopusCfData pos = p ? reader.textFile(CF_CLIENT_DATA, CanopusCfData::read) : null;
            final CanopusCfData neg = n ? reader.textFile(CF_CLIENT_DATA_NEG, CanopusCfData::read) : null;
            return new CanopusCfDataProperty(pos, neg);
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CanopusCfDataProperty> optClientData) throws IOException {
            final CanopusCfDataProperty canopusData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CanopusCfClientData (ClassyFire data) to write for ID: " + id));
            writer.textFile(CF_CLIENT_DATA, w -> CanopusCfData.write(w, canopusData.getPositive()));
            writer.textFile(CF_CLIENT_DATA_NEG, w -> CanopusCfData.write(w, canopusData.getNegative()));
        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.delete(CF_CLIENT_DATA);
            writer.delete(CF_CLIENT_DATA_NEG);
        }
    }
}
