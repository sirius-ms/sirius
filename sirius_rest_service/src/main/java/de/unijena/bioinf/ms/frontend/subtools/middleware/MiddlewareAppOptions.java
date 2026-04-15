/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.middleware;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.splash.Splash;
import de.unijena.bioinf.ms.frontend.subtools.OutputOptions;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.SiriusProjectSpaceInstance;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

@Slf4j
@CommandLine.Command(name = "service", aliases = {"rest", "REST"}, description = "@|bold <STANDALONE>|@ Starts SIRIUS as a background (REST) service that can be requested via a REST-API. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions<I extends SiriusProjectSpaceInstance> implements StandaloneTool<MiddlewareAppOptions<I>.Flow> {
    @Setter
    private ProjectsProvider<?> projectsProvider;
    @Setter
    private GuiService guiService;

    private Splash splash;

    public MiddlewareAppOptions(Splash splashScreen) {
        this.splash = splashScreen;
    }

    @CommandLine.Option(names = {"--port", "-p"}, description = "Specify the port on which the SIRIUS REST Service should run (Default: autodetect).")
    private void setPort(int port) {
        System.setProperty("server.port", String.valueOf(port));
    }

    @CommandLine.Option(names = {"--enable-rest-shutdown", "-s"}, description = "Allows to shut down the SIRIUS REST Service via a rest api call (/actuator/shutdown)", defaultValue = "false")
    private void setShutdown(boolean enableRestShutdown) {
        if (enableRestShutdown) {
            System.setProperty("management.endpoints.web.exposure.include", "health,shutdown");
            System.setProperty("management.endpoint.shutdown.access", "unrestricted");
        } else {
            System.setProperty("management.endpoints.web.exposure.include", "health");
            System.setProperty("management.endpoint.shutdown.access", "none");
        }
    }

    public enum ApiDocMode {STABLE, BASIC, STABLE_ADVANCED, ADVANCED}

    private final static List<String> INTERNAL_EXCLUSIONS = List.of(
            "/api/databases/downloadable",
            "/api/projects/*/aligned-features/*/formulas/*/sirius-fragtree",
            "/api/projects/*/copy",
            "/api/projects/*/import/ms-data-local-files-job",
            "/api/projects/*/import/ms-local-data-files",
            "/api/projects/*/import/preprocessed-local-data-files",
            "/api/projects/*/import/preprocessed-local-data-files-job",
            "/api/projects/*/jobs/run-command"
    );

    private final static List<String> DEPRECATED_EXCLUSIONS = List.of(
            "/api/databases",
            "/api/databases/downloadable",
            "/api/job-config-names",
            "/api/projects/*/copy",
            "/api/projects/*/import/ms-data-local-files-job",
            "/api/projects/*/import/ms-local-data-files",
            "/api/projects/*/import/preprocessed-local-data-files",
            "/api/projects/*/import/preprocessed-local-data-files-job",
            "/api/projects/*/jobs/run-command"
    );

    private final static List<String> EXPERIMENTAL_EXCLUSIONS = List.of(
            "/api/projects/*/aligned-features/*/adducts",
            "/api/projects/*/aligned-features/*/denovo-structures",
            "/api/projects/*/aligned-features/*/formulas/*/structures/*/annotated-msmsdata",
            "/api/projects/*/aligned-features/*/formulas/*/structures/*/annotated-spectrum",
            "/api/projects/*/aligned-features/*/quality-report",
            "/api/projects/*/aligned-features/*/quant-table-row",
            "/api/projects/*/aligned-features/*/spectral-library-matches/*/annotated",
            "/api/projects/*/aligned-features/*/traces",
            "/api/projects/*/aligned-features/grouped",
            "/api/projects/*/aligned-features/quant-table",
            "/api/projects/*/aligned-features/statistics/foldchange/compute",
            "/api/projects/*/aligned-features/statistics/foldchanges",
            "/api/projects/*/aligned-features/statistics/foldchanges/*",
            "/api/projects/*/aligned-features/statistics/foldchanges/stats-table",
            "/api/projects/*/aligned-features/tagged",
            "/api/projects/*/aligned-features/tags/*",
            "/api/projects/*/aligned-features/tags/*/*",
            "/api/projects/*/compounds/*/quant-table-row",
            "/api/projects/*/compounds/*/traces",
            "/api/projects/*/compounds/grouped",
            "/api/projects/*/compounds/quant-table",
            "/api/projects/*/compounds/statistics/foldchange/compute",
            "/api/projects/*/compounds/statistics/foldchanges",
            "/api/projects/*/compounds/statistics/foldchanges/*",
            "/api/projects/*/compounds/statistics/foldchanges/stats-table",
            "/api/projects/*/compounds/tagged",
            "/api/projects/*/compounds/tags/*",
            "/api/projects/*/compounds/tags/*/*",
            "/api/projects/*/groups",
            "/api/projects/*/groups/*",
            "/api/projects/*/runs/*",
            "/api/projects/*/runs/blanksubtract/compute",
            "/api/projects/*/runs/grouped",
            "/api/projects/*/runs/page",
            "/api/projects/*/runs/tagged",
            "/api/projects/*/runs/tags/*",
            "/api/projects/*/runs/tags/*/*",
            "/api/projects/*/tags",
            "/api/projects/*/tags/*"
    );

    private static final List<String> ADVANCED_EXCLUSIONS = List.of("/api/projects/*/gui/advanced");


    @CommandLine.Option(names = {"--api-doc-mode", "--stableDocOnly"}, description = "Show only the stable und non deprecated api endpoints in swagger gui and openapi spec.", hidden = true)
    private void setStableDocOnly(boolean stableDocOnly) {
        if (stableDocOnly)
            setApiDocMode(ApiDocMode.STABLE);
    }

    @CommandLine.Option(names = {"--api-mode"}, description = "Specify api endpoints shown in swagger gui and openapi spec.", defaultValue = "BASIC", hidden = true)
    private void setApiDocMode(ApiDocMode apiDocMode) {
        switch (apiDocMode) {
            case STABLE ->
                    System.setProperty("springdoc.pathsToExclude",
                            Stream.of(INTERNAL_EXCLUSIONS, EXPERIMENTAL_EXCLUSIONS, ADVANCED_EXCLUSIONS)
                                    .flatMap(List::stream)
                                    .collect(Collectors.joining(",")));
            case BASIC ->
                    System.setProperty("springdoc.pathsToExclude", String.join(",", ADVANCED_EXCLUSIONS));
            case STABLE_ADVANCED ->
                    System.setProperty("springdoc.pathsToExclude", Stream.of(INTERNAL_EXCLUSIONS, EXPERIMENTAL_EXCLUSIONS)
                            .flatMap(List::stream)
                            .collect(Collectors.joining(",")));
            case ADVANCED ->
                    System.getProperties().remove("springdoc.pathsToExclude");
        }
    }

    @CommandLine.Option(names = {"--enums-as-ref"}, description = "Specify whether enums in the api model should be represented as object reference.", hidden = true, defaultValue = "false")
    private void setEnumsAsRef(boolean enumsAsRef) {
        if (enumsAsRef)
            System.setProperty("enums-as-ref", String.valueOf(true));
        else
            System.getProperties().remove("enums-as-ref");
    }

    public boolean isStartGui() {
        return guiSupport.startGui;
    }

    public boolean isHeadless() {
        return guiSupport.headless;
    }

    @CommandLine.ArgGroup
    private GuiSupport guiSupport = new GuiSupport(false, false);

    @NoArgsConstructor
    @AllArgsConstructor
    private static class GuiSupport {
        @CommandLine.Option(names = {"--gui", "-g"}, description = "Start GUI on specified project or on temporary project otherwise.")
        private boolean startGui;

        @CommandLine.Option(names = {"--headless"}, description = {"Enforce headless or gui mode (default) for SIRIUS service.", "Headless mode Prevents loading features that are not available on headless systems. This is usually auto-detected but in case this gives not the expected behavior this parameter can be used to enforce it."}, defaultValue = "false", order = 1000, negatable = true)
        private boolean headless;
    }

    @Override
    public Flow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new Flow(rootOptions.getOutput());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public class Flow implements Workflow {
        private final OutputOptions output;

        private Flow(OutputOptions output) {
            this.output = output;
        }

        @Override
        public void run() {
            //do the project importing from the commandline
            Optional<Path> location = Optional.ofNullable(output).map(OutputOptions::getOutputProjectLocation);
            if (location.isPresent() || MiddlewareAppOptions.this.isStartGui()) {
                try {
                    final ProjectInfo startPs;
                    if (location.isEmpty()) {
                        //open default project if given
                        startPs = projectsProvider.createTempProject(EnumSet.noneOf(ProjectInfo.OptField.class));
                    } else {
                        String psid = location.get().getFileName().toString();
                        if (psid.endsWith(SIRIUS_PROJECT_SUFFIX))
                            psid = psid.substring(0, psid.length() - SIRIUS_PROJECT_SUFFIX.length());
                        psid = FileUtils.sanitizeFilename(psid);

                        startPs = projectsProvider.createProject(
                                psid,
                                location.get().toAbsolutePath().toString(),
                                EnumSet.noneOf(ProjectInfo.OptField.class), false);
                    }

                    if (guiService != null) {
                        if (isStartGui())
                            guiService.createGuiInstance(startPs.getProjectId());

                        //increase priority of GUI thread.
                        Jobs.runEDTLater(() -> Thread.currentThread().setPriority(9));
                    } else {
                        log.info("No GUI service found. Skipping GUI startup, likely due to headless mode!");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (splash != null)
                Jobs.runEDTLater(splash::dispose);
        }
    }
}
