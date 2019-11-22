package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.InputFiles;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.jjobs.BasicJJob;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.List;

/**
 * Only one preprocessing Subtool is allowed per Workflow. They can only run in first position
 * These tools are intended to transform some non standard input into a list of Instances
 * The default here is to write the results to the project space that will be used for th rest of the workflow
 */
public abstract class PreprocessingJob extends BasicJJob<Iterable<? extends Instance>> implements SubToolJob {
    protected InputFiles input;
    protected ProjectSpaceManager space;

    public PreprocessingJob() {
        this(null, null);
    }

    public PreprocessingJob(@Nullable InputFiles input, @Nullable ProjectSpaceManager space) {
        super(JobType.SCHEDULER);
        this.input = input;
        this.space = space;
    }

    public void setInput(InputFiles input) {
        this.input = input;
    }

    public void setProjectSpace(ProjectSpaceManager space) {
        this.space = space;
    }
}
