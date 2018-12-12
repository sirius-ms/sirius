package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractProjectWriter implements ProjectWriter {

    protected ExperimentResult experimentResult;

    protected HashSet<String> surpressedOutputs = new HashSet<>();

    public void surpress(String output) {
        surpressedOutputs.add(output);
    }

    public boolean isSurpressed(String name) {
        return surpressedOutputs.contains(name);
    }

    public boolean isAllowed(String name) {
        return !isSurpressed(name);
    }

    protected abstract void startWriting();

    @Override
    public void close() throws IOException {
        endWriting();
    }

    protected abstract void endWriting();

    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        this.experimentResult = result;
        writeInput(result, result.getExperiment());
        if (result.getResults()!=null) {
            startWritingIdentificationResults(result, result.getResults());
            for (IdentificationResult r : result.getResults()) {
                startWritingIdentificationResult(r);
                writeIdentificationResult(r);
                endWritingIdentificationResult(r);
            }
            endWritingIdentificationResults(result.getResults());
        }
        endWritingExperiment(result);
    }

    protected abstract void endWritingExperiment(ExperimentResult result) throws IOException ;

    protected abstract void endWritingIdentificationResults(List<IdentificationResult> results) throws IOException ;

    protected abstract void endWritingIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void writeIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void startWritingIdentificationResult(IdentificationResult result)throws IOException ;

    protected abstract void startWritingIdentificationResults(ExperimentResult er, List<IdentificationResult> results)throws IOException ;

    protected abstract void writeInput(ExperimentResult result, Ms2Experiment experiment)throws IOException ;
}
