package de.unijena.bioinf.ms.cli.workflow;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.cli.parameters.AddConfigsJob;
import de.unijena.bioinf.ms.cli.parameters.DataSetJob;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.ms.cli.parameters.RootOptions;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.properties.DefaultParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Workflow {
    private final DefaultParameterConfig parameters;
    private final SiriusProjectSpace project;

    private Iterator<ExperimentResult> inputIterator;
    private List<Object> toolchain;

    //todo we need the buffer infos here
    public Workflow(RootOptions.IO inputOutput, DefaultParameterConfig parameters, List<Object> toolchain) {
        project = inputOutput.projectSpace;
        inputIterator = inputOutput.inputIterator;
        this.parameters = parameters;
        this.toolchain = toolchain;
    }

    public void run() throws ExecutionException {
        final List<InstanceJob.Factory> instanceJobChain = new ArrayList<>(toolchain.size());
        //job factory for job that add config annotations to an instance
        instanceJobChain.add(makeConfigJob());

        //other jobs
        for (Object o : toolchain) {
            if (o instanceof InstanceJob.Factory) {
                instanceJobChain.add((InstanceJob.Factory) o);
            } else if (o instanceof DataSetJob) {
                final DataSetJob dataSetJob = (DataSetJob) o;
                final WorkflowJobSubmitter submitter = new WorkflowJobSubmitter(inputIterator, project, instanceJobChain, dataSetJob);
                submitter.start(10, 10); //todo how to wait on this? blocking?
                inputIterator = submitter.jobManager().submitJob(dataSetJob).awaitResult().iterator();
                instanceJobChain.clear(); //todo do we need to keep the jobs for something?
            } else {
                throw new IllegalArgumentException("Illegal job Type submitted. Only InstanceJobs and DataSetJobs are allowed");
            }
        }
        if (!instanceJobChain.isEmpty()) {
            final WorkflowJobSubmitter submitter = new WorkflowJobSubmitter(inputIterator, project, instanceJobChain, null);
            submitter.start(10, 10); //todo how to wait on this? blocking?
        }
    }

    protected InstanceJob.Factory<AddConfigsJob> makeConfigJob() {
        return () -> new AddConfigsJob(parameters.createInstancesWithDefaults(Ms2ExperimentAnnotation.class));
    }


}
