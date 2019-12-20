package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.CANOPUS_CLIENT_DATA;

public class CanopusClientSerializer  implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CanopusData> {


    @Override
    public CanopusData read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        if (!reader.exists(CANOPUS_CLIENT_DATA))
            return null;
        return reader.textFile(CANOPUS_CLIENT_DATA, CanopusData::read);
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CanopusData> optClientData) throws IOException {
        final CanopusData canopusData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CanopusClientData to write for ID: " + id));
        writer.textFile(CANOPUS_CLIENT_DATA, w -> CanopusData.write(w, canopusData));
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(CANOPUS_CLIENT_DATA);
    }
}
