/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workfow.GuiWorkflowBuilder;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManagerFactory;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class SiriusGUIApplication extends SiriusCLIApplication {


    public static void main(String[] args) {
        ApplicationCore.DEFAULT_LOGGER.info("Starting Application Core");

        if (TIME)
            t1 = System.currentTimeMillis();
        try {
            measureTime("Init Swing Job Manager");
            SiriusJobs.setJobManagerFactory((cpuThreads) -> new SwingJobManager(Math.min(defaultThreadNumber(), cpuThreads), 1));
            ApplicationCore.DEFAULT_LOGGER.info("Swing Job MANAGER initialized! " + SiriusJobs.getGlobalJobManager().getCPUThreads() + " : " + SiriusJobs.getGlobalJobManager().getIOThreads());

            configureShutDownHook(() -> {
                Jobs.cancelALL();
                shutdownWebservice();
            });

            measureTime("Start Run method");

            run(args, () -> {
                final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
                CLIRootOptions rootOptions = new CLIRootOptions<>(configOptionLoader, new GuiProjectSpaceManagerFactory());
                return new GuiWorkflowBuilder<>(rootOptions, configOptionLoader, new GuiInstanceBufferFactory());
            });
        } catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }finally {
            if (! (RUN.getFlow() instanceof GuiAppOptions.Flow))
                System.exit(0);
        }


/*
        try {
            if (RUN != null)
                throw new IllegalStateException("Application can only run Once!");
            measureTime("init Run");
            RUN = new Run();
            measureTime("Start Parse args");
            boolean b = RUN.parseArgs(args);
            measureTime("Parse args Done!");
            //att some point it would be great to use headless mode for headless apps
            if (b) {
                measureTime("Configure Boot Environment");
                //configure boot app
                final SpringApplicationBuilder appBuilder = new SpringApplicationBuilder(SiriusGUIApplication.class)
                        .web(WebApplicationType.NONE)
                        .headless(false)
                        .bannerMode(Banner.Mode.OFF);
                measureTime("Start Workflow");
                if (RUN.getFlow() instanceof MiddlewareAppOptions.Flow) {//run rest service
//                    System.out.println(System.getProperty("management.endpoints.web.exposure.include"));
                  appContext = appBuilder.web(WebApplicationType.SERVLET).run(args);
                }else if (RUN.getFlow() instanceof GuiAppOptions.Flow){
                    appContext = appBuilder.run(args);
                }else {
                    appContext = appBuilder.run(args);
                    appContext.close();
                    System.exit(0);
                }
                measureTime("Workflow DONE!");
            } else {
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
*/
    }

    public static int defaultThreadNumber(){
        int threadsAv = PropertyManager.getNumberOfThreads();
        return Math.max(1, threadsAv <= 8 ? threadsAv - 2 : threadsAv - threadsAv / 20);
    }
}
