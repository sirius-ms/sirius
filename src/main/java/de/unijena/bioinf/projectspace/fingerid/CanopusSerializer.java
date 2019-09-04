package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;

public class CanopusSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, CanopusResult> {
    @Override
    public CanopusResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final double[] probabilities = reader.doubleVector(FingerIdLocations.CanopusResults.apply(id));
        return new CanopusResult(new ProbabilityFingerprint())
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, CanopusResult component) throws IOException {

    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {

    }
}
