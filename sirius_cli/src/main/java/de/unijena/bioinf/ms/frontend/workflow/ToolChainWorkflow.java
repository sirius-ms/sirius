package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ms.frontend.parameters.AddConfigsJob;
import de.unijena.bioinf.ms.frontend.parameters.DataSetJob;
import de.unijena.bioinf.ms.frontend.parameters.InstanceJob;
import de.unijena.bioinf.ms.io.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ToolChainWorkflow implements Workflow {
    protected final static Logger LOG = LoggerFactory.getLogger(ToolChainWorkflow.class);
    protected final ParameterConfig parameters;
    protected final SiriusProjectSpace project;

    private Iterator<ExperimentResult> inputIterator;
    protected List<Object> toolchain;

    protected int initialInstanceNum, maxBufferSize = 0;

    public ToolChainWorkflow(SiriusProjectSpace projectSpace, Iterator<ExperimentResult> inputIterator, ParameterConfig parameters, List<Object> toolchain) {
        this.project = projectSpace;
        this.inputIterator = inputIterator;

        this.parameters = parameters;
        this.toolchain = toolchain;
    }

    @Override
    public void run() {
        try {
//            final Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> parameterInstancesCache =
//                    parameters.createInstancesWithDefaults(Ms2ExperimentAnnotation.class); //todo cache instances???
            final List<InstanceJob.Factory> instanceJobChain = new ArrayList<>(toolchain.size());
            //job factory for job that add config annotations to an instance
            instanceJobChain.add(() -> new AddConfigsJob(parameters));

            //other jobs
            for (Object o : toolchain) {
                if (o instanceof InstanceJob.Factory) {
                    instanceJobChain.add((InstanceJob.Factory) o);
                } else if (o instanceof DataSetJob.Factory) {
                    final DataSetJob dataSetJob = ((DataSetJob.Factory) o).makeJob();
                    final WorkflowJobSubmitter submitter = new WorkflowJobSubmitter(inputIterator, project, instanceJobChain, dataSetJob);
                    submitter.start(initialInstanceNum, maxBufferSize);
                    inputIterator = submitter.jobManager().submitJob(dataSetJob).awaitResult().iterator();
                    instanceJobChain.clear();
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
        } catch (ExecutionException e) {
            LOG.error("Error When Executing ToolChain");
        }
    }

    public void setInstanceBuffer(int initialInstanceNum, int maxBufferSize) {
        this.initialInstanceNum = initialInstanceNum;
        this.maxBufferSize = maxBufferSize;
    }

}
