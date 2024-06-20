/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.configuration.project;

import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceProviderImpl;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
@Configuration
@ConditionalOnProperty(name = "sirius.middleware.project-space", havingValue = "PROJECT-DIRECTORY")
public class SiriusProjectSpaceConfig {

    @Bean
    public ProjectSpaceManagerFactory<? extends ProjectSpaceManager> projectSpaceManagerFactory() {
        ProjectSpaceConfiguration config = SiriusProjectSpaceManagerFactory.newDefaultConfig();
        config.registerComponent(FormulaResult.class, FBCandidatesTopK.class, new FBCandidatesSerializerTopK(FBCandidateNumber.GUI_DEFAULT));
        config.registerComponent(FormulaResult.class, FBCandidateFingerprintsTopK.class, new FBCandidateFingerprintSerializerTopK(FBCandidateNumber.GUI_DEFAULT));

        return new SiriusProjectSpaceManagerFactory(config);
    }

    @Bean
    @DependsOn({"jobManager"})
    public ProjectsProvider<?> projectsProvider(ComputeService computeService, EventService<?> eventService, ProjectSpaceManagerFactory<? extends ProjectSpaceManager> projectSpaceManagerFactory) {
        return new SiriusProjectSpaceProviderImpl((ProjectSpaceManagerFactory<SiriusProjectSpaceManager>) projectSpaceManagerFactory, eventService, computeService);
    }

}
