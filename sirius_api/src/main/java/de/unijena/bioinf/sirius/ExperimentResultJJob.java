package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.JJob;

public interface ExperimentResultJJob extends JJob<ExperimentResult> {

    public Ms2Experiment getExperiment();
}
