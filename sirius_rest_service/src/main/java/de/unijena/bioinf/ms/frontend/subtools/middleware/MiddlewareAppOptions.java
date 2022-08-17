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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import picocli.CommandLine;

@CommandLine.Command(name = "asService", aliases = {"rest", "REST"}, description = "EXPERIMENTAL/UNSTABLE: Starts SIRIUS as a background (REST) service that can be requested via a REST-API", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true)
public class MiddlewareAppOptions<I extends Instance, P extends ProjectSpaceManager<I>> implements StandaloneTool<MiddlewareAppOptions.Flow<I, P>> {

    @CommandLine.Option(names = {"--port", "-p"}, description = "Specify the port on which the SIRIUS REST Service should run (Default: 8080).", defaultValue = "8080")
    private void setPort(int port) {
        System.getProperties().setProperty("server.port", String.valueOf(port));
    }

    @CommandLine.Option(names = {"--enable-rest-shutdown", "-s"}, description = "Allows to shut down the SIRIUS REST Service via a rest api call (/actuator/shutdown)", defaultValue = "false")
    private void setShutdown(boolean enableRestShutdown) {
        if (enableRestShutdown)
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health,shutdown");
        else
            System.getProperties().setProperty("management.endpoints.web.exposure.include", "info,health");

    }

    @Override
    public Flow<I, P> makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new Flow<>((RootOptions<I, P, PreprocessingJob<P>, ?>) rootOptions, config);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static class Flow<I extends Instance, P extends ProjectSpaceManager<I>> implements Workflow {

        private final PreprocessingJob<P> preproJob;
        private final ParameterConfig config;


        private Flow(RootOptions<I, P, PreprocessingJob<P>, ?> opts, ParameterConfig config) {
            this.preproJob = opts.makeDefaultPreprocessingJob();
            this.config = config;
        }

        @Override
        public void run() {
            //do the project importing from the commandline
            SiriusJobs.getGlobalJobManager().submitJob(preproJob).takeResult();
        }
    }
}
