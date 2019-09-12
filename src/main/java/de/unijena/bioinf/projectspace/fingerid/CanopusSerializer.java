package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;
import java.util.Optional;

public class CanopusSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, CanopusResult> {
    @Override
    public CanopusResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        String loc = FingerIdLocations.CanopusResults.apply(id);
        if (!reader.exists(loc)) return null;
        final CanopusClientData canopusClientData = reader.getProjectSpaceProperty(CanopusClientData.class).orElseThrow();
        final double[] probabilities = reader.doubleVector(loc);
        final ProbabilityFingerprint probabilityFingerprint = new ProbabilityFingerprint(canopusClientData.maskedFingerprintVersion, probabilities);
        return new CanopusResult(probabilityFingerprint);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<CanopusResult> optCanopusResult) throws IOException {
        final CanopusResult canopusResult = optCanopusResult.orElseThrow(() -> new IllegalArgumentException("Could not find canopusResult to write for ID: " + id));

        writer.inDirectory(FingerIdLocations.CanopusDir, ()->{
           writer.doubleVector(id.fileName("fpt"), canopusResult.getCanopusFingerprint().toProbabilityArray());
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FingerIdLocations.CanopusResults.apply(id));
    }
}
