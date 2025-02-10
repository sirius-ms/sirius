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
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectProviderImpl;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.middleware.service.search.FakeLuceneSearchService;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.projectspace.NitriteProjectSpaceManagerFactory;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnProperty(name = "sirius.middleware.project-space", havingValue = "NITRITE-NOSQL")
public class NitriteNoSqlProjectConfig {

    @Bean
    public ProjectSpaceManagerFactory<? extends ProjectSpaceManager> projectSpaceManagerFactory() {
        return new NitriteProjectSpaceManagerFactory();
    }

    @Bean
    public SearchService searchService() {
        return new FakeLuceneSearchService();
    }

    @Bean
    @DependsOn({"jobManager"})
    @SuppressWarnings("unchecked")
    public ProjectsProvider<?> projectsProvider(ComputeService computeService,
                                                SearchService searchService,
                                                EventService<?> eventService,
                                                ProjectSpaceManagerFactory<? extends ProjectSpaceManager> projectSpaceManagerFactory
    ) {
        return new NoSQLProjectProviderImpl((ProjectSpaceManagerFactory<NoSQLProjectSpaceManager>) projectSpaceManagerFactory, eventService, computeService, searchService);
    }
}
