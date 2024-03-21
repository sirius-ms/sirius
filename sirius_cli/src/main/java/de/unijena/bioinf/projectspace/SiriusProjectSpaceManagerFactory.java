/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@Slf4j
public final class SiriusProjectSpaceManagerFactory implements ProjectSpaceManagerFactory<ProjectSpaceManager> {
    public ProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<?> factory, @Nullable Function<Ms2Experiment, String> formatter) {
        return new ProjectSpaceManager(space, factory, formatter);
    }

    public ProjectSpaceManager create(SiriusProjectSpace space) {
        return create(space, null);
    }


    public ProjectSpaceManager create(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter) {
        return create(space, new InstanceFactory.Default(), formatter);
    }

    @Override
    public ProjectSpaceManager createOrOpen(@Nullable Path projectLocation) throws IOException {

        if (projectLocation == null) {
            projectLocation = ProjectSpaceIO.createTmpProjectSpaceLocation();
            log.warn("No unique output location found. Writing output to Temporary folder: " + projectLocation.toString());
        }

        final SiriusProjectSpace psTmp;
        if (Files.notExists(projectLocation)) {
            psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(projectLocation, true);
        } else {
            psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(projectLocation);
        }

        //check for formatter

        return create(psTmp, new StandardMSFilenameFormatter());
    }
}
