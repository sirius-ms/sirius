package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;

public interface ExperimentResultJJob extends JJob<ExperimentResult> {

    public Ms2Experiment getExperiment();
}
