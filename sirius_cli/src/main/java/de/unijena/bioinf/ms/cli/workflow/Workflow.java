package de.unijena.bioinf.ms.cli.workflow;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.cli.parameters.AddConfigsJob;
import de.unijena.bioinf.ms.cli.parameters.DataSetJob;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.ms.cli.parameters.RootOptions;
import de.unijena.bioinf.ms.io.projectspace.ConfigSummaryWriter;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Workflow {
    protected final static Logger LOG = LoggerFactory.getLogger(Workflow.class);
    protected final ParameterConfig parameters;
    protected final SiriusProjectSpace project;

    private Iterator<ExperimentResult> inputIterator;
    protected List<Object> toolchain;

    protected int initialInstanceNum, maxBufferSize = 0;

    //todo we need the buffer infos here
    public Workflow(RootOptions.IO inputOutput, ParameterConfig parameters, List<Object> toolchain) {
        project = inputOutput.projectSpace;
        inputIterator = inputOutput.inputIterator;

        this.parameters = parameters;
        this.toolchain = toolchain;

        project.registerSummaryWriter(new ConfigSummaryWriter(parameters));
    }

    public void run() throws ExecutionException {
        final Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> parameterInstancesCache =
                parameters.createInstancesWithDefaults(Ms2ExperimentAnnotation.class);
        final List<InstanceJob.Factory> instanceJobChain = new ArrayList<>(toolchain.size());
        //job factory for job that add config annotations to an instance
        instanceJobChain.add(() -> new AddConfigsJob(parameters,parameterInstancesCache));

        //other jobs
        for (Object o : toolchain) {
            if (o instanceof InstanceJob.Factory) {
                instanceJobChain.add((InstanceJob.Factory) o);
            } else if (o instanceof DataSetJob.Factory) {
                final DataSetJob dataSetJob = ((DataSetJob.Factory) o).makeJob();
                final WorkflowJobSubmitter submitter = new WorkflowJobSubmitter(inputIterator, project, instanceJobChain, dataSetJob);
                submitter.start(initialInstanceNum, maxBufferSize); //todo how to wait on this? blocking?
                inputIterator = submitter.jobManager().submitJob(dataSetJob).awaitResult().iterator();
                instanceJobChain.clear(); //todo do we need to keep the jobs for something?
            } else {
                throw new IllegalArgumentException("Illegal job Type submitted. Only InstanceJobs and DataSetJobs are allowed");
            }
        }

        if (!instanceJobChain.isEmpty()) {
            CollectorJob collector = new CollectorJob();
            final WorkflowJobSubmitter submitter = new WorkflowJobSubmitter(inputIterator, project, instanceJobChain, collector);
            submitter.start(initialInstanceNum, maxBufferSize);
            inputIterator = submitter.jobManager().submitJob(collector).awaitResult().iterator();
        }
        System.out.println("workflow finished");

        try {
            project.writeSummaries(() -> inputIterator, (cur, max, mess) -> System.out.println((((((double) cur) / (double) max)) * 100d) + "% " + mess));
            project.close();
        } catch (IOException e) {
            LOG.error("Error when closing workspace. Workspace summaries may be incomplete");
        }
    }

    public void setInstanceBuffer(int initialInstanceNum, int maxBufferSize) {
        this.initialInstanceNum = initialInstanceNum;
        this.maxBufferSize = maxBufferSize;
    }

}
