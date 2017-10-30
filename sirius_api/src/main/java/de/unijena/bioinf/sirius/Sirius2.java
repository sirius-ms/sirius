package de.unijena.bioinf.sirius;

import de.unijena.bioinf.jjobs.JobManager;

import java.io.IOException;

public class Sirius2 extends Sirius {

    protected JobManager jobManager;

    public Sirius2(String profileName) throws IOException {
        super(profileName);
    }
/*
    @Override
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        //return super.identify(uexperiment, numberOfCandidates, recalibrating, deisotope, formulaConstraints);
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager.)
    }

    */
}
