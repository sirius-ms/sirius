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

import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.NPC_CLIENT_DATA;
import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.NPC_CLIENT_DATA_NEG;

public class CanopusNpcDataProperty extends PosNegFpProperty<NPCFingerprintVersion, CanopusNpcData> {
    public CanopusNpcDataProperty(CanopusNpcData positive, CanopusNpcData negative) {
        super(positive, negative);
    }

    public static class Serializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CanopusNpcDataProperty> {

        @Override
        public CanopusNpcDataProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
            boolean p = reader.exists(NPC_CLIENT_DATA);
            boolean n = reader.exists(NPC_CLIENT_DATA_NEG);
            if (!p && !n)
                return null;

            final CanopusNpcData pos = p ? reader.textFile(NPC_CLIENT_DATA, CanopusNpcData::read) : null;
            final CanopusNpcData neg = n ? reader.textFile(NPC_CLIENT_DATA_NEG, CanopusNpcData::read) : null;
            return new CanopusNpcDataProperty(pos, neg);
        }

        @Override
        public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CanopusNpcDataProperty> optClientData) throws IOException {
            final CanopusNpcDataProperty canopusData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CanopusNpcClientData (NPC data) to write for ID: " + id));
            writer.textFile(NPC_CLIENT_DATA, w -> CanopusNpcData.write(w, canopusData.getPositive()));
            writer.textFile(NPC_CLIENT_DATA_NEG, w -> CanopusNpcData.write(w, canopusData.getNegative()));
        }

        @Override
        public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
            writer.delete(NPC_CLIENT_DATA);
            writer.delete(NPC_CLIENT_DATA_NEG);
        }
    }
}
