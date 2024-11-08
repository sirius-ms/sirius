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

import com.github.f4b6a3.tsid.TsidCreator;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.storage.db.nosql.Database;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

@Slf4j
public class NitriteProjectSpaceManagerFactory implements ProjectSpaceManagerFactory<NoSQLProjectSpaceManager> {

    @Override
    public NoSQLProjectSpaceManager createOrOpen(@Nullable Path projectLocation) throws IOException {

        if (projectLocation == null) {
            projectLocation = FileUtils.newTempFile("sirius-tmp-project-" + TsidCreator.getTsid(), SIRIUS_PROJECT_SUFFIX);
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
        return projectSpaceManager;
    }

    @SneakyThrows
    private static void updateProjectType(SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        if (project.findProjectType().isEmpty()) {
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
        }
    }
}
