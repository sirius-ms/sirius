package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.CANOPUS_CLIENT_DATA;

public class CanopusClientSerializer  implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CanopusClientData> {


    @Override
    public CanopusClientData read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        final ClassyFireFingerprintVersion V = ClassyFireFingerprintVersion.getDefault();
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(V);
        builder.disableAll();
        if (!reader.exists(CANOPUS_CLIENT_DATA))
            return null;

        reader.table(CANOPUS_CLIENT_DATA, true, (row) -> {
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
        });
        return new CanopusClientData(
                builder.toMask()
        );
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CanopusClientData> optClientData) throws IOException {
        final CanopusClientData canopusClientData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CanopusClientData to write for ID: " + id));

        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "id", "name", "parentId", "description"};
        final String[] row = header.clone();
        writer.table(CANOPUS_CLIENT_DATA, header, Arrays.stream(canopusClientData.getFingerprintVersion().allowedIndizes()).mapToObj(absoluteIndex -> {
            final ClassyfireProperty property = (ClassyfireProperty) canopusClientData.getFingerprintVersion().getMolecularProperty(absoluteIndex);
            final int relativeIndex = canopusClientData.getFingerprintVersion().getRelativeIndexOf(absoluteIndex);
            row[0] = String.valueOf(relativeIndex);
            row[1] = String.valueOf(absoluteIndex);
            row[2] = property.getChemontIdentifier();
            row[3] = property.getName();
            row[4] = property.getParent().getChemontIdentifier();
            row[5] = property.getDescription();
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(CANOPUS_CLIENT_DATA);
    }
}
