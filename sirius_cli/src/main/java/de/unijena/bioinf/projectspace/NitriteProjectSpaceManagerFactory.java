/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.storage.db.nosql.Database;
import io.hypersistence.tsid.TSID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

@Slf4j
public class NitriteProjectSpaceManagerFactory implements ProjectSpaceManagerFactory<NoSQLProjectSpaceManager> {

    @Override
    public NoSQLProjectSpaceManager createOrOpen(@Nullable Path projectLocation) throws IOException {

        if (projectLocation == null) {
            projectLocation = FileUtils.newTempFile("sirius-tmp-project-" + TSID.fast(), SIRIUS_PROJECT_SUFFIX);
            log.warn("No unique output location found. Writing output to Temporary folder: {}", projectLocation.toString());
            if (Files.exists(projectLocation)) {
                throw new IOException("Could not create new Project '" + projectLocation + "' because it already exists");
            }
        }
        //add project file suffix if file not yet exists
        if (!projectLocation.toString().endsWith(SIRIUS_PROJECT_SUFFIX)) {
            if (!Files.exists(projectLocation)) {
                String old = projectLocation.getFileName().toString();
                String nu = old + SIRIUS_PROJECT_SUFFIX;
                log.warn("Given project file '{}' does not contain the `{}` file extension. Renamed file to '{}'.", old, SIRIUS_PROJECT_SUFFIX, nu);
                projectLocation = projectLocation.getParent().resolve(nu);
            }
        }
        NoSQLProjectSpaceManager projectSpaceManager = new NoSQLProjectSpaceManager(new NitriteSirirusProject(projectLocation));
        updateProjectType(projectSpaceManager.getProject());
        updateProjectSourceFormats(projectSpaceManager.getProject());
        return projectSpaceManager;
    }

    @SneakyThrows
    private static void updateProjectType(SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        if (project.findProjectType().isEmpty()) {
            StopWatch w = StopWatch.createStarted();
            log.info("Updating project type information...");
            if (project.getStorage().countAll(MergedLCMSRun.class) > 0) {
                project.upsertProjectType(ProjectType.ALIGNED_RUNS);
            } else if (project.getStorage().countAll(LCMSRun.class) > 0) {
                project.upsertProjectType(ProjectType.UNALIGNED_RUNS);
            } else {
                project.getAllAlignedFeatures().findAny().ifPresent(af -> {
                    if (project.getConfig(af.getAlignedFeatureId(), ConfigType.INPUT_FILE).isPresent())
                        project.upsertProjectType(ProjectType.PEAKLISTS);
                    else
                        project.upsertProjectType(ProjectType.DIRECT_IMPORT);
                });
            }
            log.info("Updating project type information done in {}.", w);
        }
    }

    @SneakyThrows
    private static void updateProjectSourceFormats(SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        if (project.findProjectSourceFormats().isEmpty()) {
            StopWatch w = StopWatch.createStarted();
            log.info("Updating project source information...");
            project.findProjectType().ifPresentOrElse(projectType -> {
                if (projectType == ProjectType.ALIGNED_RUNS || projectType == ProjectType.UNALIGNED_RUNS) {
                    try {
                        @NotNull Set<String> exts = project.getStorage().findAllStr(LCMSRun.class)
                                .map(LCMSRun::getSourceReference)
                                .flatMap(o -> o.getFileName().stream())
                                .map(FileUtils::getFileExt)
                                .filter(Objects::nonNull)
                                .filter(MsExperimentParser::isSupportedEnding)
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet());
                        project.upsertProjectSourceFormats(ProjectSourceFormats.fromFormats(exts));
                    } catch (IOException e) {
                        log.warn("Could not update project source formats for {}", projectType, e);
                        // there is not really a security measure that distiguises the different LCMS formats, so we
                        // can assume mzml as default even if it might be fake.
                        project.upsertProjectSourceFormats(ProjectSourceFormats.fromFormats(".mzml"));
                    }
                } else if (projectType == ProjectType.PEAKLISTS) {
                    try {
                        @NotNull Set<String> exts = project.getStorage().findAllStr(AlignedFeatures.class)
                                .flatMap(af -> af.getDataSource().stream())
                                .map(DataSource::getSource)
                                .map(FileUtils::getFileExt)
                                .filter(Objects::nonNull)
                                .filter(MsExperimentParser::isSupportedEnding)
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet());
                        project.upsertProjectSourceFormats(ProjectSourceFormats.fromFormats(exts));
                    } catch (IOException e) {
                        // if this is not readable, the project should be not usable anyway we just skip and try again next
                        // time in case it was a temproray io issue.
                        log.warn("Could not update project source formats for {}. try again next time!", projectType, e);
                    }
                } else if (projectType == ProjectType.DIRECT_IMPORT) {
                    // we do not update direct import projects because we cannot determine the source from historic projects
                    // and want to keep compatibility high
                }
                log.info("Updating project source information done in {}.", w);

            }, () -> log.info("Could not update project source information because project type info is missing!"));
            project.findProjectSourceFormats().ifPresent(projectSourceFormats -> {
                try {
                    System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(projectSourceFormats));
                } catch (JsonProcessingException e) {
                   log.error("Error while printing project source formats!", e);
                }
            });
        }
    }
}
