/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.frontend.splash.Splash;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceConfiguration;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.fingerid.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class SiriusGUIApplication extends SiriusCLIApplication {

    public static void main(String[] args) {
        System.setProperty(APP_TYPE_PROPERTY_KEY, "GUI");
        //run gui if not parameter ist given, to get rid of a second launcher
        if (args == null || args.length == 0)
            args = new String[]{"gui"};

        if (Arrays.stream(args).noneMatch(it -> it.equalsIgnoreCase("gui")) ) {
            SiriusCLIApplication.runMain(args, List.of(new GuiAppOptions(null))); //inject for help message
        } else {
            {
                Path propsFile = Workspace.siriusPropsFile;
                //override VM defaults from OS
                if (!System.getProperties().containsKey("sun.java2d.uiScale"))
                    System.setProperty("sun.java2d.uiScale", "1");
                //override with stored value if available
                if (Files.exists(propsFile)) {
                    Properties props = new Properties();
                    try (BufferedReader r = Files.newBufferedReader(propsFile)) {
                        props.load(r);
                        if (props.containsKey("sun.java2d.uiScale"))
                            System.setProperty("sun.java2d.uiScale", props.getProperty("sun.java2d.uiScale"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            final Splash splash = new Splash();

            final @NotNull Supplier<ProjectSpaceConfiguration> dc = ProjectSpaceManager.DEFAULT_CONFIG;
            ProjectSpaceManager.DEFAULT_CONFIG = () -> {
                final ProjectSpaceConfiguration config = dc.get();
                config.registerComponent(FormulaResult.class, FBCandidatesTopK.class, new FBCandidatesSerializerTopK(FBCandidateNumber.GUI_DEFAULT));
                config.registerComponent(FormulaResult.class, FBCandidateFingerprintsTopK.class, new FBCandidateFingerprintSerializerTopK(FBCandidateNumber.GUI_DEFAULT));
                return config;
            };

            if (TIME)
                t1 = System.currentTimeMillis();

            try {
                final String[] finalArgs = args;
                TinyBackgroundJJob<Object> j = new TinyBackgroundJJob<>() {
                    @Override
                    protected Object compute() throws Exception {
                        ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");
                        updateProgress(0, 7, 1, "Starting Application Core...");
                        measureTime("Init Swing Job Manager");
                        // The spring app classloader seems not to be correctly inherited to sub thread
                        // So we need to ensure that the apache.configuration2 libs gets access otherwise.
                        final boolean springSupport = Boolean.parseBoolean(System.getProperty("de.unijena.bioinf.sirius.springSupport", "false"));
                        SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(
                                cpuThreads,
                                Math.min(PropertyManager.getNumberOfThreads(), 4),
                                springSupport ? Thread.currentThread().getContextClassLoader() : null
                        ));
                        ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());
                        updateProgress(0, 7, 2, "Configure shutdown hooks...");

                        measureTime("Setting GUI Instance Buffer factory");
                        BackgroundRuns.setBufferFactory(new GuiInstanceBufferFactory());

                        configureShutDownHook(() -> {
                            Jobs.cancelAllRuns();
                            shutdownWebservice().run();
                        });

                        measureTime("Start Run method");
                        updateProgress(0, 7, 3, "Configure Workflows... ");
                        run(finalArgs, () -> {
                            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
                            CLIRootOptions<?,?> rootOptions = new CLIRootOptions<>(configOptionLoader, new GuiProjectSpaceManagerFactory());
                            updateProgress(0, 7, 4, "Firing up SIRIUS... ");
                            removePropertyChangeListener(splash);
                            return new WorkflowBuilder<>(rootOptions, configOptionLoader,
                                    new GuiInstanceBufferFactory(), List.of(new GuiAppOptions(splash)));
                        });
                        return null;
                    }
                };

                j.addPropertyChangeListener(splash);
                j.call();
            } catch (Exception e){
                e.printStackTrace();
                System.exit(0);
            }finally {
                if (! (RUN.getFlow() instanceof GuiAppOptions.Flow))
                    System.exit(0);
            }
        }
    }
}
