package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.subtools.config.AddConfigsJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.annotaions.RecomputeResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolChainWorkflow implements Workflow {
    protected final static Logger LOG = LoggerFactory.getLogger(ToolChainWorkflow.class);
    protected final ParameterConfig parameters;
    private final PreprocessingJob preprocessingJob;

    protected List<Object> toolchain;
    protected int initialInstanceNum, maxBufferSize = 0;

    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private InstanceBuffer submitter = null;
    private final ProjectSpaceManager project;

    public ToolChainWorkflow(ProjectSpaceManager projectSpace, PreprocessingJob preprocessingJob, ParameterConfig parameters, List<Object> toolchain) {
        this.preprocessingJob = preprocessingJob;
        this.parameters = parameters;
        this.toolchain = toolchain;
        this.project = projectSpace;
    }

    @Override
    public void cancel() {
        canceled.set(true);
        submitter.cancel();
    }

    protected void checkForCancellation() throws InterruptedException {
        if (canceled.get())
            throw new InterruptedException("Workflow was canceled");
    }

    //todo allow dataset jobs da do not have to put all exps into memory
    //todo low io mode: if instance buffer is infinity we do never have to read instances from disk (write only)
    @Override
    public void run() {
        try {
            checkForCancellation();
            // prepare input
            Iterable<Instance> iteratorSource = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();
            // build toolchain
            final List<InstanceJob.Factory> instanceJobChain = new ArrayList<>(toolchain.size());
            //job factory for job that add config annotations to an instance
            instanceJobChain.add(() -> new AddConfigsJob(parameters));
            //other jobs
            for (Object o : toolchain) {
                checkForCancellation();
                if (o instanceof InstanceJob.Factory) {
                    instanceJobChain.add((InstanceJob.Factory) o);
                } else if (o instanceof DataSetJob.Factory) {
                    final DataSetJob dataSetJob = ((DataSetJob.Factory) o).makeJob();
                    submitter = new SimpleInstanceBuffer(maxBufferSize, iteratorSource.iterator(), instanceJobChain, dataSetJob);
                    submitter.start();
                    iteratorSource = SiriusJobs.getGlobalJobManager().submitJob(dataSetJob).awaitResult();

                    checkForCancellation();
                    //todo we do this now directly in the jobs
                    /*// writing each experiment to add results to projectSpace
                    // for instance jobs this is done by the buffer
                    iteratorSource.forEach(it -> {
                        try {
                            project.projectSpace().writeExperiment(it);
                        } catch (IOException e) {
                            LoggerFactory.getLogger(getClass()).error("Error writing instance: " + it.getExperiment().getAnnotation(MsFileSource.class));
                        }
                    });*/

                    instanceJobChain.clear();
                } else {
                    throw new IllegalArgumentException("Illegal job Type submitted. Only InstanceJobs and DataSetJobs are allowed");
                }
            }

            // we have no dataset job that ends the chain, so we have to collect resuts from
            // disk to not waste memory -> otherwise the whole buffer thing is useless.
            checkForCancellation();
            if (!instanceJobChain.isEmpty()) {
                submitter = new SimpleInstanceBuffer(maxBufferSize, iteratorSource.iterator(), instanceJobChain, null);
                submitter.start();
                iteratorSource = project;
            }
            LOG.info("Workflow has been finished! Writing Project-Space summaries...");

            checkForCancellation();
            try {
                //remove recompute annotation since it should be cli only option
                iteratorSource.forEach(it -> it.getExperiment().setAnnotation(RecomputeResults.class,null));
                //use all experiments in workspace to create summaries
                //todo write summaries
//                project.writeSummaries(iteratorSource, (cur, max, mess) -> System.out.println((((((double) cur) / (double) max)) * 100d) + "% " + mess));
                project.projectSpace().close();
                LOG.info("Project-Space successfully written!");
            } catch (IOException e) {
                LOG.error("Error when closing workspace. Workspace summaries may be incomplete");
            }
        } catch (ExecutionException e) {
            LOG.error("Error When Executing ToolChain");
        } catch (InterruptedException e) {
            LOG.info("Workflow successfully canceled!", e);
        }
    }

    public void setInstanceBuffer(int initialInstanceNum, int maxBufferSize) {
        this.initialInstanceNum = initialInstanceNum;
        this.maxBufferSize = maxBufferSize;
    }
}
