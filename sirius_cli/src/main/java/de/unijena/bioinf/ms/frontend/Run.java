package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;


/**
 * This is our Commandline tool
 * <p>
 * Here we parse parameters, configure technical stuff,
 * read input, merge workspace, configure Algorithms/Workflows and write output.
 * <p>
 * Basic Idea:
 * <p>
 * Note: A Workspace can be input and output at the same time!
 * Some methods will use it as input and check whether
 * the needed input is present (e.g Zodiac).
 * Other Methods only produce output to the Workspace (e.g. SIRIUS).
 * So they need to merge their results with the existing ones.
 */
public class Run extends ApplicationCore {
    protected final static Logger logger = LoggerFactory.getLogger(Run.class);
    protected Workflow flow;
    private final WorkflowBuilder<?> builder;

    public Run(WorkflowBuilder<?> builder) {
        this.builder = builder;
        this.builder.initRootSpec();
    }


    public void compute() {
        if (!isWorkflowDefined())
            throw new IllegalStateException("No Workflow defined for computation.");
        flow.run();
    }

    public boolean isWorkflowDefined(){
        return flow != null;
    }

    /*Returns true if a workflow was parsed*/
    public boolean parseArgs(String[] args) {
        if (args == null || args.length < 1)
            args = new String[]{"--help"};
        logger.info("Running with following arguments: " + Arrays.toString(args));
        final CommandLine commandline = new CommandLine(builder.getRootSpec());
        commandline.setCaseInsensitiveEnumValuesAllowed(true);
        commandline.registerConverter(DefaultParameter.class, new DefaultParameter.Converter());
        flow = commandline.parseWithHandler(builder.makeParseResultHandler(), args);
        return flow != null; //todo maybe workflow validation would be nice here???
    }

    public void cancel() {
        if (flow != null)
            flow.cancel();
    }

    public Workflow getFlow() {
        return flow;
    }
}
