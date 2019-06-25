package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.RootOptionsCLI;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;


/**
 * This is our Commandline tool
 * <p>
 * Here we parse parameters, configure technical stuff,
 * read input, merge workspace, configure Algorithms/Workflows and wirte output.
 * <p>
 * Basic Idea:
 * <p>
 * Note: A Workspace can be input and output at the same time!
 * Some methods will use it as input and check whether
 * the needed input is present (e.g Zodiac).
 * Other Methods only produce output to the Workspace (e.g. SIRIUS).
 * So they need to merge their results with the existing ones.
 */
public class CLIRun extends ApplicationCore {
    protected final static Logger logger = LoggerFactory.getLogger(CLIRun.class);
    private de.unijena.bioinf.ms.frontend.workflow.Workflow flow;


    public void compute() {
        if (flow == null)
            throw new IllegalStateException("No Workflow defined for computation.");
        flow.run();
    }

    /*Returns true if a workflow was parsed*/
    public boolean parseArgs(String[] args) throws IOException {
        final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
        final WorkflowBuilder<RootOptionsCLI> builder = new WorkflowBuilder<>(new RootOptionsCLI(configOptionLoader), configOptionLoader);

        flow = new CommandLine(builder.rootSpec).parseWithHandler(builder.makeParseResultHandler(), args);
        return flow != null; //todo maybe workflow validation would be nice here???
    }
}
