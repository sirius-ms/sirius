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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Optional;

@Getter
public class NoSQLProjectSpaceManager extends AbstractProjectSpaceManager {

    private final SiriusProjectDatabaseImpl<? extends Database<?>> project;

    /**
     * Filter to apply when iterating over instances. Has no effect on counting compounds.
     */
    @Setter
    private Filter alignedFeaturesFilter = null;

    public NoSQLProjectSpaceManager(SiriusProjectDatabaseImpl<? extends Database<?>> project) {
        this.project = project;
    }


    @SneakyThrows
    @Override
    public @NotNull NoSQLInstance importInstanceWithUniqueId(Ms2Experiment inputExperiment) {
        AlignedFeatures alignedFeature = getProject().importMs2ExperimentAsAlignedFeature(inputExperiment);
        return new NoSQLInstance(alignedFeature, this);
    }



    @SneakyThrows
    @Override
    public @NotNull Optional<NoSQLInstance> findInstance(Object id) {
        if (id instanceof Number numId)
            return findInstance(numId.longValue());
        else if (id instanceof String strId){
            return findInstance(Long.valueOf(strId));
        }
        throw new IllegalArgumentException("Id must be a long value or a String that represents a long value!");
    }

    @SneakyThrows
    public @NotNull Optional<NoSQLInstance> findInstance(long id) {
        return getProject().getStorage().getByPrimaryKey(id, AlignedFeatures.class)
                .map(af -> new NoSQLInstance(af, this));
    }

    @Override
    public void writeFingerIdData(@NotNull FingerIdData pos, @NotNull FingerIdData neg) {
        getProject().insertFingerprintData(pos, 1);
        getProject().insertFingerprintData(neg, -1);
    }

    @Override
    public @NotNull Optional<FingerIdData> getFingerIdData(int charge) {
        return getProject().findFingerprintData(FingerIdData.class, charge);
    }

    @Override
    public void writeCanopusData(@NotNull CanopusCfData cfPos, @NotNull CanopusCfData cfNeg,
                                 @NotNull CanopusNpcData npcPos, @NotNull CanopusNpcData npcNeg) {
        getProject().insertFingerprintData(cfPos, 1);
        getProject().insertFingerprintData(cfNeg, -1);
        getProject().insertFingerprintData(npcPos, 1);
        getProject().insertFingerprintData(npcNeg, -1);
    }

    @Override
    @SneakyThrows
    public void deleteFingerprintData() {
        getProject().getStorage().removeAll(SiriusProjectDocumentDatabase.FP_DATA_COLLECTION, (Filter) null);
    }

    @Override
    public @NotNull Optional<CanopusCfData> getCanopusCfData(int charge) {
        return getProject().findFingerprintData(CanopusCfData.class, charge);
    }

    @Override
    public @NotNull Optional<CanopusNpcData> getCanopusNpcData(int charge) {
        return getProject().findFingerprintData(CanopusNpcData.class, charge);
    }

    @SneakyThrows
    @Override
    public @NotNull Iterator<Instance> iterator() {
        return getProject().getAlignedFeatures(alignedFeaturesFilter).map(af -> (Instance) new NoSQLInstance(af, this)).iterator();
    }

    @SneakyThrows
    @Override
    public int countFeatures() {
        if (alignedFeaturesFilter == null) {
            return (int) project.getStorage().countAll(AlignedFeatures.class);
        } else {
            return (int) project.getStorage().count(alignedFeaturesFilter, AlignedFeatures.class);
        }
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
    public @NotNull Optional<ProjectType> getType() {
        return project.findProjectType();
    }

    @Override
    public boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) {
        //todo remove all fingerprint data related results and de fingerprint data itself
        return true;
    }

    @Override
    public String getName() {
        String filename = project.getStorage().location().getFileName().toString();
        return filename.substring(0, filename.lastIndexOf('.'));
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
