package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.List;

public class ExperimentResult {

    protected Ms2Experiment experiment;
    protected List<IdentificationResult> results;

    public ExperimentResult(Ms2Experiment experiment, List<IdentificationResult> results) {
        this.experiment = experiment;
        this.results = results;
    }
}
