package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.MsFileSource;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;

import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

class ExtendableInstanceBuffer extends BufferedJJobSubmitter<Instance> {
    private final List<InstanceJob.Factory> tasks;
    private final DataSetJob dependJob;
    private final SiriusProjectSpace projectSpace;


    public ExtendableInstanceBuffer(@NotNull Iterator<Instance> instances, @NotNull SiriusProjectSpace space, @NotNull List<InstanceJob.Factory> tasks, @Nullable DataSetJob dependJob) {
        super(instances);
        this.projectSpace = space;
        this.tasks = tasks;
        this.dependJob = dependJob;
    }

    @Override
    protected void submitJobs(final JobContainer instanceProvider) {
        Instance instance = instanceProvider.sourceInstance;
        JJob<Instance> jobToWaitOn = (DymmyExpResultJob) () -> instance;
        for (InstanceJob.Factory task : tasks) {
            jobToWaitOn = task.createToolJob(jobToWaitOn);
            submitJob(jobToWaitOn, instanceProvider);
        }
        if (dependJob != null)
            dependJob.addRequiredJob(jobToWaitOn);
    }

    @Override // this is handled in main thread
    protected void handleResults(JobContainer watcher) {
       //todo done by the jobs itself
        /* try {
            projectSpace.writeExperiment(watcher.sourceInstance);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error writing instance: " + watcher.sourceInstance.getExperiment().getAnnotation(MsFileSource.class));
        }*/
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}
