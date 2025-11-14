package de.unijena.bioinf.ms.frontend.subtools.custom_db_downloader;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.rest.client.databases.DownloadableDatabase;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

@Slf4j
@CommandLine.Command(name = "list", description = "Print information about all downloadable databases.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ListDownloadableDBsOptions implements StandaloneTool<Workflow> {

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            try {
                ApplicationCore.WEB_API().listDownloadableDatabases().forEach(this::printDatabaseInfo);
            } catch (IOException e) {
                log.error("Error getting downloadable databases.", e);
            }
        };
    }

    private void printDatabaseInfo(DownloadableDatabase db) {
        System.out.println("##########  BEGIN DATABASE INFO  ##########");
        System.out.println("ID: " + db.id());
        System.out.println("Size: " + FileUtils.sizeToReadableString(db.size()));
        System.out.println("Description:");
        System.out.println(db.description());
        System.out.println("#################  END  ##################");
        System.out.println();
    }
}
