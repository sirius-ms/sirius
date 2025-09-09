package de.unijena.bioinf.ms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.NitriteProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class CLITest {

    protected static final String ASSERT_TOOL = "ASSERT";
    protected static Run run;
    protected static CommandLine.Model.CommandSpec rootCLISpec;

    protected Path projectLocation;
    @AutoClose
    protected NitriteSirirusProject ps;

    @BeforeAll
    public static void initApplication() throws IOException {
        if (run == null) {
            ApplicationCore.VERSION(); // execute static init code
            DefaultParameterConfigLoader defaultConfigOptions = new DefaultParameterConfigLoader();
            NitriteProjectSpaceManagerFactory spaceManagerFactory = new NitriteProjectSpaceManagerFactory();
            CLIRootOptions rootOptions = new CLIRootOptions(defaultConfigOptions, spaceManagerFactory);
            WorkflowBuilder workflowBuilder = new WorkflowBuilder(rootOptions);
            run = new Run(workflowBuilder, false);
            rootCLISpec = workflowBuilder.getRootSpec();
        }
    }

    @BeforeEach
    public void createProject() throws IOException {
        projectLocation = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(projectLocation);
    }

    protected void runAssert(AssertTool assertTool, String... testArgs) {
        rootCLISpec.removeSubcommand(ASSERT_TOOL);
        rootCLISpec.addSubcommand(ASSERT_TOOL, CommandLine.Model.CommandSpec.forAnnotatedObject(assertTool));

        List<String> args = new ArrayList<>(List.of("-p", projectLocation.toString()));
        Collections.addAll(args, testArgs);
        args.add(ASSERT_TOOL);
        runWithArguments(args.toArray(String[]::new));
    }

    protected void runWithArguments(String... args) {
        run.parseArgs(args);
        run.makeWorkflow();
        run.compute();
    }

    /**
     * Subtool for asserting that a test CLI run made the expected effects
     */
    @CommandLine.Command(name = ASSERT_TOOL)
    public static abstract class AssertTool implements StandaloneTool<Workflow> {

        @Override
        public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
            return () -> assertWorkflow(rootOptions, config);
        }

        /**
         * Assertion logic goes here
         */
        protected abstract void assertWorkflow(RootOptions<?> rootOptions, ParameterConfig config);
    }

    /**
     * Assert tool for checking instances retrieved from the project
     */
    public static class InstanceAssertTool extends AssertTool {

        protected Consumer<List<Instance>> assertions;

        public InstanceAssertTool(Consumer<List<Instance>> assertions) {
            this.assertions = assertions;
        }

        @Override
        protected void assertWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
            try {
                PreprocessingJob<?> prepJob = rootOptions.makeDefaultPreprocessingJob();
                ProjectSpaceManager psm = (ProjectSpaceManager) SiriusJobs.getGlobalJobManager().submitJob(prepJob).awaitResult();

                List<Instance> instanceList = new ArrayList<>();
                psm.forEach(instanceList::add);
                assertions.accept(instanceList);
                psm.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return AssertTool that checks the instance ids are as expected (ignoring order)
     */
    protected InstanceAssertTool expectedInstanceIds(String... expectedIds) {
        return new InstanceAssertTool(instances -> assertEquals(Set.of(expectedIds), instances.stream().map(Instance::getId).collect(Collectors.toSet())));
    }
}
