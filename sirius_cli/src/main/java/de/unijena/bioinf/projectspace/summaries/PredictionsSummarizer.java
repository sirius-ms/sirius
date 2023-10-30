package de.unijena.bioinf.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.subtools.export.tables.ExportPredictionsOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.tables.PredictionsOptions;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PredictionsSummarizer implements Summarizer {
    final JobProgressEventListener listener;
    final Iterable<? extends Instance> compounds;
    final int polarity;
    final String locationString;
    final PredictionsOptions predictionsOptions;

    public PredictionsSummarizer(JobProgressEventListener listener, Iterable<? extends Instance> compounds, int polarity, String locationString, PredictionsOptions predictionsOptions) {
        this.listener = listener;
        this.compounds = compounds;
        this.polarity = polarity;
        this.locationString = locationString;
        this.predictionsOptions = predictionsOptions;
    }


    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        List<Class<? extends DataAnnotation>> annotations = new ArrayList<>();
        if (predictionsOptions.requiresFingerprintResult()){
            annotations.add(FingerprintResult.class);
        }
        if (predictionsOptions.requiresFingerprintResult()){
            annotations.add(CanopusResult.class);
        }
        return annotations;
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter projectWriter, @NotNull CompoundContainer compoundContainer, List<? extends SScored<FormulaResult, ? extends FormulaScore>> list) throws IOException {

    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter projectWriter) throws IOException {
        projectWriter.textFile(locationString, this::write);
    }

    public void write(final Writer writer) throws IOException {
        ExportPredictionsOptions.ExportPredictionJJob posJob = new ExportPredictionsOptions.ExportPredictionJJob(
                predictionsOptions, polarity, compounds,
                () -> new BufferedWriter(writer));
        try {
            posJob.addJobProgressListener(listener);
            SiriusJobs.getGlobalJobManager().submitJob(posJob).awaitResult();
            posJob.removePropertyChangeListener(listener);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }

    }
}
