package de.unijena.bioinf.ms.rest.model.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

public class CanopusData {

    protected final MaskedFingerprintVersion maskedFingerprintVersion;
    protected final ClassyFireFingerprintVersion classyFireFingerprintVersion;

    public CanopusData(@NotNull MaskedFingerprintVersion maskedFingerprintVersion) {
        this.maskedFingerprintVersion = maskedFingerprintVersion;
        this.classyFireFingerprintVersion = (ClassyFireFingerprintVersion) maskedFingerprintVersion.getMaskedFingerprintVersion();
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return maskedFingerprintVersion;
    }

    public ClassyFireFingerprintVersion getClassyFireFingerprintVersion() {
        return classyFireFingerprintVersion;
    }

    public static CanopusData read(BufferedReader reader) throws IOException {
        final ClassyFireFingerprintVersion V = ClassyFireFingerprintVersion.getDefault();
        final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(V);
        builder.disableAll();


        FileUtils.readTable(reader, true, (row) -> {
            final int abs = Integer.parseInt(row[1]);
            builder.enable(abs);
        });

        return new CanopusData(
                builder.toMask()
        );
    }

    public static void write(@NotNull Writer writer, @NotNull final CanopusData canopusData) throws IOException {
        final String[] header = new String[]{"relativeIndex", "absoluteIndex", "id", "name", "parentId", "description"};
        final String[] row = header.clone();

        FileUtils.writeTable(writer, header, Arrays.stream(canopusData.getFingerprintVersion().allowedIndizes()).mapToObj(absoluteIndex -> {
            final ClassyfireProperty property = (ClassyfireProperty) canopusData.getFingerprintVersion().getMolecularProperty(absoluteIndex);
            final int relativeIndex = canopusData.getFingerprintVersion().getRelativeIndexOf(absoluteIndex);
            row[0] = String.valueOf(relativeIndex);
            row[1] = String.valueOf(absoluteIndex);
            row[2] = property.getChemontIdentifier();
            row[3] = property.getName();
            row[4] = property.getParent()!=null ? property.getParent().getChemontIdentifier() : "";
            row[5] = property.getDescription();
            return row;
        })::iterator);
    }
}
