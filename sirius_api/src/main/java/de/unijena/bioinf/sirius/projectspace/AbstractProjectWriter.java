package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.IOException;
import java.util.List;

public abstract class AbstractProjectWriter implements ProjectWriter {

    protected ExperimentResult experimentResult;

    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        this.experimentResult = result;
        writeInput(result, result.experiment);
        startWritingIdentificationResults(result, result.results);
        for (IdentificationResult r : result.results) {
            startWritingIdentificationResult(r);
            writeIdentificationResult(r);
            endWritingIdentificationResult(r);
        }
        endWritingIdentificationResults(result.results);
        endWritingExperiment(result.experiment);
    }

    protected abstract void endWritingExperiment(Ms2Experiment experiment) throws IOException ;

    protected abstract void endWritingIdentificationResults(List<IdentificationResult> results) throws IOException ;

    protected abstract void endWritingIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void writeIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void startWritingIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results)throws IOException ;

    protected abstract void writeInput(ExperimentResult result, Ms2Experiment experiment)throws IOException ;
}
