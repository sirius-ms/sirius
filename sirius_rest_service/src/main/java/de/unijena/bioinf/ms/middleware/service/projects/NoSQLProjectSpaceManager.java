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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class NoSQLProjectSpaceManager implements ProjectSpaceManager {

    @Getter
    private final SiriusProjectDatabaseImpl<? extends Database<?>> project;

    public NoSQLProjectSpaceManager(SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        this.project = project;
    }


    @Override
    public @NotNull Instance importInstanceWithUniqueId(Ms2Experiment inputExperiment) {
        // TODO
        return null;
    }

    @Override
    public @NotNull Optional<Instance> findInstance(String id) {
        // TODO
        return Optional.empty();
    }

    @Override
    public void writeFingerIdData(@NotNull FingerIdData pos, @NotNull FingerIdData neg) {

    }

    @Override
    public void deleteFingerIdData() {

    }

    @Override
    public @NotNull Optional<FingerIdData> getFingerIdData(int charge) {
        return Optional.empty();
    }

    @Override
    public void writeCanopusData(@NotNull CanopusCfData cfPos, @NotNull CanopusCfData cfNeg, @NotNull CanopusNpcData npcPos, @NotNull CanopusNpcData npcNeg) {

    }

    @Override
    public void deleteCanopusData() {

    }

    @Override
    public @NotNull Optional<CanopusCfData> getCanopusCfData(int charge) {
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<CanopusNpcData> getCanopusNpcData(int charge) {
        return Optional.empty();
    }

    @Override
    public @NotNull Iterator<Instance> iterator() {
        // TODO
        return null;
    }

    @SneakyThrows
    @Override
    public int countFeatures() {
        return (int) project.getStorage().countAll(AlignedFeatures.class);
    }

    @SneakyThrows
    @Override
    public int countCompounds() {
        return (int) project.getStorage().countAll(Compound.class);
    }

    @SneakyThrows
    @Override
    public long sizeInBytes() {
        return Files.size(project.getStorage().location());
    }

    @Override
    public void close() throws IOException {
        project.getStorage().close();
    }

    @Override
    public boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) throws TimeoutException, InterruptedException {
        return false;
    }

    @Override
    public String getName() {
        return project.getStorage().location().getFileName().toString();
    }

    @Override
    public String getLocation() {
        return project.getStorage().location().toAbsolutePath().toString();
    }

    @Override
    public void flush() throws IOException {
        project.getStorage().flush();
    }

}
