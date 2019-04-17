package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.annotations.Annotated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ExperimentResult implements Annotated<ResultAnnotation> {
    public static enum ErrorCause {TIMEOUT, NORESULTS, ERROR, NOERROR};

    protected Ms2Experiment experiment;
    protected ErrorCause error;
    protected String errorMessage;
    private final Annotations<ResultAnnotation> annotations = new Annotations<>();


    public ExperimentResult(Ms2Experiment experiment) {
        this.experiment = experiment;
        this.error = ErrorCause.NOERROR;
    }

    public ExperimentResult(@NotNull Ms2Experiment experiment, @Nullable IdentificationResults results) {
        this(experiment);
        if (results != null)
            setAnnotation(IdentificationResults.class, results);
    }


    public ExperimentResult(@NotNull Ms2Experiment experiment, @Nullable Iterable<IdentificationResult> results) {
        this(experiment, results != null ? new IdentificationResults(results) : null);
    }


    @Deprecated
    public ExperimentResult(@NotNull Ms2Experiment experiment, @Nullable IdentificationResults results, @Nullable String errorString) {
        this(experiment, results, (errorString == null) ? ErrorCause.ERROR : ErrorCause.valueOf(errorString));
    }

    public ExperimentResult(@NotNull Ms2Experiment experiment, @Nullable IdentificationResults results, @NotNull ErrorCause error) {
        this(experiment,results);
        this.error = error;
    }

    public ExperimentResult(@NotNull Ms2Experiment experiment, @Nullable IdentificationResults results, @NotNull ErrorCause error, @Nullable String errorMessage) {
        this(experiment,results);
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public boolean hasError(){
        return !error.equals(ErrorCause.NOERROR);
    }

    public String getErrorString() {
        return error.toString();
    }

    public ErrorCause getError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSimplyfiedExperimentName() {
        if (experiment.getName() == null)
            return "";
        return simplify(experiment.getName());
    }

    public String getSimplyfiedExperimentSource() {
        if (experiment.getSource() == null)
            return "";
        return simplifyURL(experiment.getSource().getFile());
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    public IdentificationResults getResults() {
        return getAnnotation(IdentificationResults.class);
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

    @Override
    public Annotations<ResultAnnotation> annotations() {
        return annotations;
    }
}
