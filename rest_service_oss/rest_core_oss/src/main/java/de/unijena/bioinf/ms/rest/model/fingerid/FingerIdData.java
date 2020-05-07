package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.projectspace.ProjectSpaceProperty;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Has to be set by CLI when first running CSI via WebAPI call
 */
public class FingerIdData {

    protected MaskedFingerprintVersion fingerprintVersion;
    protected CdkFingerprintVersion cdkFingerprintVersion;
    protected PredictionPerformance[] performances;

    public FingerIdData(MaskedFingerprintVersion fingerprintVersion, PredictionPerformance[] performances) {
        this.fingerprintVersion = fingerprintVersion;
        this.cdkFingerprintVersion = (CdkFingerprintVersion) fingerprintVersion.getMaskedFingerprintVersion();
        this.performances = performances;
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    public CdkFingerprintVersion getCdkFingerprintVersion() {
        return cdkFingerprintVersion;
    }

    public PredictionPerformance[] getPerformances() {
        return performances;
    }

    public static FingerIdData read(BufferedReader reader) throws IOException {
        final ArrayList<PredictionPerformance> performances = new ArrayList<>();
        final CdkFingerprintVersion V = CdkFingerprintVersion.getDefault();
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(V);
        builder.disableAll();
        FileUtils.readTable(reader, true, (row) -> {
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
            final PredictionPerformance performance = new PredictionPerformance(
                    Double.parseDouble(row[3]), Double.parseDouble(row[4]), Double.parseDouble(row[5]), Double.parseDouble(row[6])
            );
            performances.add(performance);
        });
        return new FingerIdData(
                builder.toMask(), performances.toArray(PredictionPerformance[]::new)
        );
    }

    public static void write(@NotNull Writer writer, @NotNull final FingerIdData clientData) throws IOException {
        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "description", "TP", "FP", "TN", "FN", "Acc", "MCC", "F1", "Recall", "Precision", "Count"};
        final String[] row = header.clone();
        FileUtils.writeTable(writer, header, Arrays.stream(clientData.fingerprintVersion.allowedIndizes()).mapToObj(absoluteIndex -> {
            final MolecularProperty property = clientData.fingerprintVersion.getMolecularProperty(absoluteIndex);
            final int relativeIndex = clientData.fingerprintVersion.getRelativeIndexOf(absoluteIndex);
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

}