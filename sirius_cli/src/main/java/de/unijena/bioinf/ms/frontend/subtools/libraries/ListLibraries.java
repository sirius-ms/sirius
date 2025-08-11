package de.unijena.bioinf.ms.frontend.subtools.libraries;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

@Slf4j
@CommandLine.Command(name = "list", description = "Print information about all available default libraries.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ListLibraries implements StandaloneTool<Workflow> {

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            try {
                ApplicationCore.WEB_API.listLibraries().forEach(System.out::println);
            } catch (IOException e) {
                log.error("Error getting remote libraries info.", e);
            }
        };
    }
}
