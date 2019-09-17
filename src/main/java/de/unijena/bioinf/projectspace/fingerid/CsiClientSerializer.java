package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.projectspace.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERID_CLIENT_DATA;

public class CsiClientSerializer implements ComponentSerializer<ProjectSpaceContainerId, ProjectSpaceContainer<ProjectSpaceContainerId>, CSIClientData> {
    @Override
    public CSIClientData read(ProjectReader reader, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container) throws IOException {
        if (!reader.exists(FINGERID_CLIENT_DATA))
            return null;

        final ArrayList<PredictionPerformance> performances = new ArrayList<>();
        final CdkFingerprintVersion V = CdkFingerprintVersion.getDefault();
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(V);
        builder.disableAll();
        reader.table(FINGERID_CLIENT_DATA, true, (row)->{
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
            final PredictionPerformance performance = new PredictionPerformance(
                    Double.parseDouble(row[3]), Double.parseDouble(row[4]), Double.parseDouble(row[5]), Double.parseDouble(row[6])
            );
            performances.add(performance);
        });
        return new CSIClientData(
            builder.toMask(), performances.toArray(PredictionPerformance[]::new)
        );
    }

    @Override
    public void write(ProjectWriter writer, ProjectSpaceContainerId id, ProjectSpaceContainer<ProjectSpaceContainerId> container, Optional<CSIClientData> optClientData) throws IOException {
        final CSIClientData clientData = optClientData.orElseThrow(() -> new IllegalArgumentException("Could not find CSI:ClientData to write for ID: " + id));

        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "description", "TP", "FP", "TN", "FN", "Acc", "MCC", "F1", "Recall", "Precision", "Count"};
        final String[] row = header.clone();
        writer.table(FINGERID_CLIENT_DATA, header, Arrays.stream(clientData.fingerprintVersion.allowedIndizes()).mapToObj(absoluteIndex -> {
            final MolecularProperty property = clientData.fingerprintVersion.getMolecularProperty(absoluteIndex);
            final int relativeIndex = clientData.fingerprintVersion.getRelativeIndexOf(absoluteIndex);
            final String name = property.getDescription().replace('\t',' ');
            row[0] = String.valueOf(relativeIndex);
            row[1] = String.valueOf(absoluteIndex);
            row[2] = property.getDescription();
            PredictionPerformance P = clientData.performances[relativeIndex];
            row[3] = String.valueOf(P.getTp());
            row[4] = String.valueOf(P.getFp());
            row[5] = String.valueOf(P.getTn());
            row[6] = String.valueOf(P.getFn());
            row[7] = String.valueOf(P.getAccuracy());
            row[8] = String.valueOf(P.getMcc());
            row[9] = String.valueOf(P.getF());
            row[10] = String.valueOf(P.getRecall());
            row[11] = String.valueOf(P.getPrecision());
            row[12] = String.valueOf(P.getCount());
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, ProjectSpaceContainerId id) throws IOException {
        writer.delete(FINGERID_CLIENT_DATA);
    }
}
