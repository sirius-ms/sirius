package de.unijena.bioinf.ms.frontend.subtools.project;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import picocli.CommandLine;

import java.util.concurrent.ExecutionException;

@Slf4j
@CommandLine.Command(name = "project", description = "<STANDALONE> Project space management.%n%n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ProjectOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--compact", description = "Compact project to reduce projects file size. "
            + "Reclaims unused space, and rewrites how records are laid out on disk if the project was "
            + "written by a version that laid them out wastefully.")
    boolean compact;

    @CommandLine.Option(names = "--compact-full", description = "Compact project and always rewrite how its "
            + "records are laid out on disk, even if that does not look necessary. Reads and writes the whole "
            + "project and needs free disk space for a second copy of it, so it takes considerably longer than "
            + "--compact. For projects whose waste --compact cannot detect.")
    boolean compactFull;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            ProjectSpaceManager psm;
            System.out.println("Opening project " + rootOptions.getOutput().getOutputProjectLocation() + "...");
            try {
                psm = (ProjectSpaceManager) SiriusJobs.getGlobalJobManager().submitJob(rootOptions.makeDefaultPreprocessingJob()).awaitResult();
            } catch (ExecutionException e) {
                log.error("Error opening project", e);
                throw new CommandLine.PicocliException("Could not open project " + rootOptions.getOutput().getOutputProjectLocation());
            }
            if (compact || compactFull) {
                if (compactFull && !(psm instanceof NoSQLProjectSpaceManager))
                    throw new CommandLine.PicocliException("--compact-full needs a project of the current format; "
                            + "use --compact for '" + psm.getName() + "'.");
                System.out.println((compactFull ? "Fully compacting project " : "Compacting project ")
                        + psm.getName() + "...");
                System.out.println("Size before compacting: " + FileUtils.sizeToReadableString(psm.sizeInBytes()));
                StopWatch w = StopWatch.createStarted();
                if (compactFull)
                    ((NoSQLProjectSpaceManager) psm).compactByRewriting();
                else
                    psm.compact();
                w.stop();
                System.out.println("Compacting finished in " + w);
                System.out.println("Size after compacting: " + FileUtils.sizeToReadableString(psm.sizeInBytes()));
            } else {
                throw new CommandLine.PicocliException("Missing project action.");
            }
        };
    }
}
