package de.unijena.bioinf.ms.cli.parameters.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.confidence_score.ConfidenceScoreComputor;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public class FingeridSubToolJob extends InstanceJob {
    private final FingerIdOptions cliOptions;

    public FingeridSubToolJob(FingerIdOptions cliOptions) {
        this.cliOptions = cliOptions;
    }

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        System.out.println("I am FingerID on Experiment " + expRes.getSimplyfiedExperimentName());


        //todo Fill Me
        return expRes;
    }





}
