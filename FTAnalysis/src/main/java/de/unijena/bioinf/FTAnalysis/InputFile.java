package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.io.File;

/**
 * Created by kaidu on 04.02.14.
 */
public class InputFile {

    private Ms2Experiment experiment;
    private File fileName;

    public InputFile(Ms2Experiment experiment, File fileName) {
        this.experiment = experiment;
        this.fileName = fileName;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Ms2Experiment experiment) {
        this.experiment = experiment;
    }

    public File getFileName() {
        return fileName;
    }

    public void setFileName(File fileName) {
        this.fileName = fileName;
    }
}
