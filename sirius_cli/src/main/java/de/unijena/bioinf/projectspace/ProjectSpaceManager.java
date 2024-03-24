/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.rest.NetUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public interface ProjectSpaceManager extends IterableWithSize<Instance> {
    @NotNull Instance importInstanceWithUniqueId(Ms2Experiment inputExperiment);

    //    @NotNull Instance importInstanceWithUniqueId(AlignedFeatures inputExperiment);

    @NotNull Optional<Instance> findInstance(String id);

    void writeFingerIdData(@NotNull FingerIdData pos, @NotNull FingerIdData neg);
    void deleteFingerIdData();

    @NotNull Optional<FingerIdData> getFingerIdData(int charge);
    default boolean hasFingerIdData(int charge){
        return getFingerIdData(charge).isPresent();
    }

    void writeCanopusData(@NotNull CanopusCfData cfPos, @NotNull CanopusCfData cfNeg, @NotNull CanopusNpcData npcPos, @NotNull CanopusNpcData npcNeg);
    void deleteCanopusData();

    @NotNull Optional<CanopusCfData> getCanopusCfData(int charge);
    default boolean hasCanopusCfData(int charge){
        return getCanopusCfData(charge).isPresent();
    }

    @NotNull Optional<CanopusNpcData> getCanopusNpcData(int charge);
    default boolean hasCanopusNpcData(int charge){
        return getCanopusNpcData(charge).isPresent();
    }

    @NotNull
    @Override
    Iterator<Instance> iterator();

    int countFeatures();

    int countCompounds();

    long sizeInBytes();

    @Override
    default int size() {
        return countFeatures();
    }

    void close() throws IOException;

    /**
     * This checks whether the data files are compatible with them on the server. Since have had versions of the PS with
     * incomplete data files it also loads missing files from the server but only if the existing ones are compatible.
     * <p>
     * Results are cached!
     *
     * @param interrupted Tell the waiting job how it can check if it was interrupted
     * @return true if data files are  NOT incompatible with the Server version (compatible or not existent)
     * @throws TimeoutException     if server request times out
     * @throws InterruptedException if waiting for server request is interrupted
     */
    boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) throws TimeoutException, InterruptedException;

    String getName();

    String getLocation();

    void flush() throws IOException;
}
