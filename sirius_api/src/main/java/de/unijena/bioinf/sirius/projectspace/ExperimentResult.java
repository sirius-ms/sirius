package de.unijena.bioinf.sirius.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.File;
import java.util.List;

public class ExperimentResult {
    protected String experimentName, experimentSource;
    protected Ms2Experiment experiment;
    protected List<IdentificationResult> results;
    protected String errorString;

    public ExperimentResult(Ms2Experiment experiment, List<IdentificationResult> results, String source, String name) {
        this.experiment = experiment;
        this.results = results;
        this.experimentName = name;
        this.experimentSource = source;
    }

    public ExperimentResult(Ms2Experiment experiment, List<IdentificationResult> results) {
        this.experiment = experiment;
        this.results = results;
        this.experimentName = simplify(experiment.getName());
        this.experimentSource = simplifyURL(experiment.getSource().getFile());
    }

    public ExperimentResult(Ms2Experiment experiment, List<IdentificationResult> results, String errorString) {
        this(experiment,results);
        this.errorString = errorString;
    }

    public String getErrorString() {
        return errorString;
    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public String getExperimentSource() {
        return experimentSource;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    public List<IdentificationResult> getResults() {
        return results;
    }

    private static String simplify(String name) {
        if (name.length()>64)
            name = name.substring(0,48);
        return name.replaceAll("[^A-Za-z0-9,\\-]+", "");
    }
    private static String simplifyURL(String filename) {
        filename = new File(filename).getName();
        int i = Math.min(48,filename.lastIndexOf('.'));
        return filename.substring(0,i);
    }
}
