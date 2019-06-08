package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.MsFileSource;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.parameters.DataSetJob;
import de.unijena.bioinf.ms.frontend.parameters.InstanceJob;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

class WorkflowJobSubmitter extends BufferedJJobSubmitter<ExperimentResult> {
    private final List<InstanceJob.Factory> tasks;
    private final DataSetJob dependJob;
    private final SiriusProjectSpace projectSpace;


    public WorkflowJobSubmitter(@NotNull Iterator<ExperimentResult> instances, @NotNull SiriusProjectSpace space, @NotNull List<InstanceJob.Factory> tasks, @Nullable DataSetJob dependJob) {
        super(instances);
        this.projectSpace = space;
        this.tasks = tasks;
        this.dependJob = dependJob;
    }

    @Override
    protected void submitJobs(final JobContainer instanceProvider) {
        ExperimentResult instance = instanceProvider.sourceInstance;
        JJob<ExperimentResult> jobToWaitOn = (DymmyExpResultJob) () -> instance;
        for (InstanceJob.Factory task : tasks) {
            jobToWaitOn = task.createToolJob(jobToWaitOn);
            submitJob(jobToWaitOn, instanceProvider);
        }
        if (dependJob != null)
            dependJob.addInputProvidingJob(jobToWaitOn);
    }

    @Override // this is handled in main thread
    protected void handleResults(JobContainer watcher) {
        try {
            projectSpace.writeExperiment(watcher.sourceInstance);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error writing instance: " + watcher.sourceInstance.getExperiment().getAnnotation(MsFileSource.class));
        }
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}
