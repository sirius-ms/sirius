package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.io.IOException;

public class ProjectWriterJJob extends BasicJJob{

    ProjectWriter writer;
    ExperimentResult experiment;

    public ProjectWriterJJob(ProjectWriter writer, ExperimentResult experiment) {
        super(JobType.IO);
        this.writer = writer;
        this.experiment = experiment;
    }

    @Override
    protected Object compute() throws IOException {
        writer.writeExperiment(experiment);
        return null;
    }

}
