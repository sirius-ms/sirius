package de.unijena.bioinf.projectspace.canopus;

import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CANOPUS_CLIENT_DATA;
import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CANOPUS_CLIENT_DATA_NEG;

public class CanopusDataSerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CanopusDataProperty> {


    @Override
    public CanopusDataProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        boolean p = reader.exists(CANOPUS_CLIENT_DATA);
        boolean n = reader.exists(CANOPUS_CLIENT_DATA_NEG);
        if (!p && !n)
            return null;

        final CanopusData pos = p ? reader.textFile(CANOPUS_CLIENT_DATA, CanopusData::read) : null;
        final CanopusData neg = n ? reader.textFile(CANOPUS_CLIENT_DATA_NEG, CanopusData::read) : null;
        return new CanopusDataProperty(pos, neg);
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CanopusDataProperty> optClientData) throws IOException {
        final CanopusDataProperty canopusData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CanopusClientData to write for ID: " + id));
        writer.textFile(CANOPUS_CLIENT_DATA, w -> CanopusData.write(w, canopusData.getPositive()));
        writer.textFile(CANOPUS_CLIENT_DATA_NEG, w -> CanopusData.write(w, canopusData.getNegative()));
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(CANOPUS_CLIENT_DATA);
        writer.delete(CANOPUS_CLIENT_DATA_NEG);
    }
}
