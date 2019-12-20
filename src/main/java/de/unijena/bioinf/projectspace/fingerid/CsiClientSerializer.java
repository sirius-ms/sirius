package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERID_CLIENT_DATA;

public class CsiClientSerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, FingerIdData> {
    @Override
    public FingerIdData read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        if (!reader.exists(FINGERID_CLIENT_DATA))
            return null;
        return reader.textFile(FINGERID_CLIENT_DATA, FingerIdData::read);
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<FingerIdData> optClientData) throws IOException {
        final FingerIdData clientData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CSI:ClientData to write for ID: " + id));
        writer.textFile(FINGERID_CLIENT_DATA, w -> FingerIdData.write(w, clientData));
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(FINGERID_CLIENT_DATA);
    }
}
