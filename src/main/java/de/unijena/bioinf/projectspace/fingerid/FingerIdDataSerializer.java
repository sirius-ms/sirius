package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERID_CLIENT_DATA;
import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERID_CLIENT_DATA_NEG;

public class FingerIdDataSerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, FingerIdDataProperty> {
    @Override
    public FingerIdDataProperty read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        boolean p = reader.exists(FINGERID_CLIENT_DATA);
        boolean n = reader.exists(FINGERID_CLIENT_DATA_NEG);
        if (!p && !n)
            return null;

        final FingerIdData pos = p ? reader.textFile(FINGERID_CLIENT_DATA, FingerIdData::read) : null;
        final FingerIdData neg = n ? reader.textFile(FINGERID_CLIENT_DATA_NEG, FingerIdData::read) : null;
        return new FingerIdDataProperty(pos, neg);
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<FingerIdDataProperty> optClientData) throws IOException {
        final FingerIdDataProperty clientData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CSI:ClientData to write for ID: " + id));
        writer.textFile(FINGERID_CLIENT_DATA, w -> FingerIdData.write(w, clientData.getPositive()));
        writer.textFile(FINGERID_CLIENT_DATA_NEG, w -> FingerIdData.write(w, clientData.getNegative()));
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(FINGERID_CLIENT_DATA);
        writer.delete(FINGERID_CLIENT_DATA_NEG);
    }
}
