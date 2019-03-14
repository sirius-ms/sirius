package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MsFileSource;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.jjobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public class JobSubmitter extends BufferedJJobSubmitter<Ms2Experiment> {
    private static final Logger LOG = LoggerFactory.getLogger(JobSubmitter.class);

    public JobSubmitter(Iterator<Ms2Experiment> instances) {
        super(instances);
    }

    @Override
    protected void submitJobs(final JobContainer watcher) {
        Ms2Experiment instance = watcher.sourceInstance;
        //todo build workflow

         /*ExperimentResultJJob siriusJob = siriusInstanceProcessor.makeSiriusJob(instance);
            submitJob(siriusJob, watcher);
            if (options.isFingerid()){
                FingerIDJJob putfingerIDJob = fingerIdInstanceProcessor.makeFingerIdJob(instance, siriusJob);
                if (fingerIDJob!=null)
                    submitJob(fingerIDJob, watcher);
            }*/
    }

    @Override
    protected void handleResults(JobContainer watcher) {
        /*try {
            //todo handle output
            //handleJobs(watcher);
        } catch (IOException e) {
            LOG.error("Error processing instance: " + watcher.sourceInstance.getAnnotation(MsFileSource.class));
        }*/
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }
}