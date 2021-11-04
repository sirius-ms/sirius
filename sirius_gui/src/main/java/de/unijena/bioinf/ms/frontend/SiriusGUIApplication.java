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
import de.unijena.bioinf.ms.frontend.splash.Splash;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workfow.GuiWorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.ProjectSpaceConfiguration;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateFingerprintSerializerGUI;
import de.unijena.bioinf.projectspace.fingerid.FBCandidateFingerprintsGUI;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesGUI;
import de.unijena.bioinf.projectspace.fingerid.FBCandidatesSerializerGUI;
import de.unijena.bioinf.projectspace.FormulaResult;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class SiriusGUIApplication extends SiriusCLIApplication {

    public static void main(String[] args) {
        final Splash splash = Arrays.stream(args).anyMatch(it -> it.equalsIgnoreCase("gui")) ? new Splash() : null;
        if (splash == null) {
            SiriusCLIApplication.main(args);
        } else {
            final @NotNull Supplier<ProjectSpaceConfiguration> dc = ProjectSpaceManager.DEFAULT_CONFIG;
            ProjectSpaceManager.DEFAULT_CONFIG = () -> {
                final ProjectSpaceConfiguration config = dc.get();
                config.registerComponent(FormulaResult.class, FBCandidatesGUI.class, new FBCandidatesSerializerGUI());
                config.registerComponent(FormulaResult.class, FBCandidateFingerprintsGUI.class, new FBCandidateFingerprintSerializerGUI());
                return config;
            };

            if (TIME)
                t1 = System.currentTimeMillis();

            try {
                TinyBackgroundJJob<Object> j = new TinyBackgroundJJob<>() {
                    @Override
                    protected Object compute() throws Exception {
                        ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");
                        updateProgress(0, 7, 1, "Starting Application Core...");
                        measureTime("Init Swing Job Manager");
                        SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(Math.min(defaultThreadNumber(), cpuThreads), 1));
                        ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());
                        updateProgress(0, 7, 2, "Configure shutdown hooks...");

                        configureShutDownHook(() -> {
                            Jobs.cancelALL();
                            shutdownWebservice().run();
                        });

                        measureTime("Start Run method");
                        updateProgress(0, 7, 3, "Configure Workflows... ");
                        run(args, () -> {
                            final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
                            CLIRootOptions rootOptions = new CLIRootOptions<>(configOptionLoader, new GuiProjectSpaceManagerFactory());
                            updateProgress(0, 7, 4, "Firing up SIRIUS... ");
                            removePropertyChangeListener(splash);
                            return new GuiWorkflowBuilder<>(rootOptions, configOptionLoader, new GuiInstanceBufferFactory(), splash);
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

    public static int defaultThreadNumber(){
        int threadsAv = PropertyManager.getNumberOfThreads();
        return Math.max(1, threadsAv <= 8 ? threadsAv - 2 : threadsAv - threadsAv / 20);
    }
}
