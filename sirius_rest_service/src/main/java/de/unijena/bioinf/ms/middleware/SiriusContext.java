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

package de.unijena.bioinf.ms.middleware;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeServiceImpl;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbServiceImpl;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.events.SseEventService;
import de.unijena.bioinf.ms.middleware.service.gui.GuiService;
import de.unijena.bioinf.ms.middleware.service.gui.GuiServiceImpl;
import de.unijena.bioinf.ms.middleware.service.info.ConnectionChecker;
import de.unijena.bioinf.webapi.WebAPI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
public class SiriusContext{
    @Value("${de.unijena.bioinf.siriusNightsky.version}")
    @Getter
    private String apiVersion;

    @Bean
    public EventService<?> eventService(@Value("${de.unijena.bioinf.siriusNightsky.sse.timeout:#{120000}}") long emitterTimeout){
        return new SseEventService(emitterTimeout);
    }

    @Bean
    @DependsOn({"webAPI", "jobManager"})
    public ComputeService computeService(EventService<?> eventService, InstanceBufferFactory<?> instanceBufferFactory) {
        return new ComputeServiceImpl(eventService, instanceBufferFactory);
    }
    @Bean(destroyMethod = "shutdown")
    public GuiService guiService(EventService<?> eventService, WebServerApplicationContext applicationContext){
        return new GuiServiceImpl(eventService, applicationContext);
    }

    @Bean
    ConnectionChecker connectionMonitor(WebAPI<?> webAPI){
        return new ConnectionChecker(webAPI);
    }

    @Bean
    ChemDbService chemDbService(WebAPI<?> webAPI, IFingerprinterCache iFPCache){
        return new ChemDbServiceImpl(webAPI, iFPCache);
    }

    @Bean
    @DependsOn({"jobManager"})
    IFingerprinterCache iFPCache(){
        return ApplicationCore.IFP_CACHE();
    }

    @Bean(destroyMethod = "shutdown")
    @DependsOn({"jobManager"})
    public WebAPI<?> webAPI() {
        return ApplicationCore.WEB_API;
    }

    @Bean(destroyMethod = "shutDownNowAllInstances")
    public JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }

    @Bean
    public InstanceBufferFactory<?> instanceBufferFactory() {
        return (bufferSize, instances, tasks, dependJob, progressSupport) ->
                new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJob, progressSupport, new JobSubmitter() {
                    @Override
                    public <Job extends JJob<Result>, Result> Job submitJob(Job j) {
                        if (j instanceof ToolChainJob<?> tj) {
                            Jobs.submit((ProgressJJob<?>) j, j::identifier, tj::getProjectName, tj::getToolName);
                            return j;
                        } else {
                            return Jobs.MANAGER().submitJob(j);
                        }
                    }
                });
    }
}
