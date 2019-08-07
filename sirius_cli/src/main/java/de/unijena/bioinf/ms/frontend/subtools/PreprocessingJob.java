package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Only one preprocessing Subtool is allowed per Workflow. They can only run in first position
 * These tool are intended to transform some non standard input into a list of ExperimentResults
 * The default here is to write the results to the project space that will be used for th rest of the workflow
 */
public abstract class PreprocessingJob extends BasicJJob<Iterable<ExperimentResult>> implements SubToolJob {
    protected List<File> input;
    protected SiriusProjectSpace space;

    public PreprocessingJob() {
        this(null, null);
    }

    public PreprocessingJob(@Nullable List<File> input, @Nullable SiriusProjectSpace space) {
        super(JobType.SCHEDULER);
        this.input = input;
        this.space = space;
    }

    public void setInput(List<File> input) {
        this.input = input;
    }

    public void setProjectSpace(SiriusProjectSpace space) {
        this.space = space;
    }
}
