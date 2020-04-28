package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERPRINTS;

public class FingerprintSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FingerprintResult> {

    @Override
    public FingerprintResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(FINGERPRINTS.relFilePath(id)))
            return null;

        return reader.inDirectory(FINGERPRINTS.relDir(), () -> {
            final FingerIdData fingerIdData = reader.getProjectSpaceProperty(FingerIdDataProperty.class)
                    .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

            final double[] probabilities = reader.doubleVector(FINGERPRINTS.fileName(id));
            return new FingerprintResult(new ProbabilityFingerprint(fingerIdData.getFingerprintVersion(), probabilities));
        });
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FingerprintResult> optPrint) throws IOException {
        final FingerprintResult fingerprintResult = optPrint.orElseThrow(() -> new IllegalArgumentException("Could not find finderprint to write for ID: " + id));
        writer.inDirectory(FINGERPRINTS.relDir(), () -> {
            writer.doubleVector(FINGERPRINTS.fileName(id), fingerprintResult.fingerprint.toProbabilityArray());
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FINGERPRINTS.relFilePath(id));
    }
}
