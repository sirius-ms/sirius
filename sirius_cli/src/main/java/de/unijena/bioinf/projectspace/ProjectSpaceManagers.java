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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public final class ProjectSpaceManagers {
    private ProjectSpaceManagers() {
    }

    public static void writeFingerIdDataIfMissing(ProjectSpaceManager psm, WebAPI<?> api) throws IOException {
        if (!psm.hasFingerIdData(1) || !psm.hasFingerIdData(-1)) {
            //load fingerid data
            final FingerIdData pos = api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE);
            final FingerIdData neg = api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
            psm.writeFingerIdData(pos, neg);
        }
    }

    public static void writeCanopusDataIfMissing(ProjectSpaceManager psm, WebAPI<?> api) throws IOException {
        if (!psm.hasCanopusCfData(1) || !psm.hasCanopusCfData(-1)
                ||!psm.hasCanopusNpcData(1) || !psm.hasCanopusNpcData(-1)
        ) {
            // load ClassyFire client data
            final CanopusCfData cfPos = api.getCanopusCfData(PredictorType.CSI_FINGERID_POSITIVE);
            final CanopusCfData cfNeg = api.getCanopusCfData(PredictorType.CSI_FINGERID_NEGATIVE);
            // load NPC client data
            final CanopusNpcData npcPos = api.getCanopusNpcData(PredictorType.CSI_FINGERID_POSITIVE);
            final CanopusNpcData npcNeg = api.getCanopusNpcData(PredictorType.CSI_FINGERID_NEGATIVE);
            //write to ps
            psm.writeCanopusData(cfPos, cfNeg, npcPos, npcNeg);
        }
    }

    @Nullable
    public static Boolean isCompatibleWithBackendDataLazy(@NotNull ProjectSpaceManager psm) {
        return isCompatibleWithBackendDataLazy(psm, ApplicationCore.WEB_API);
    }
    public static Boolean isCompatibleWithBackendDataLazy(@NotNull ProjectSpaceManager psm, @NotNull WebAPI<?> api) {
        try {
            return isCompatibleWithBackendData(psm, api);
        } catch (InterruptedException | TimeoutException e) {
            LoggerFactory.getLogger(InstanceImporter.class).warn("Could finish compatibility check due to an error! Could not check for outdated fingerprint data. Error: " + e.getMessage());
            return null;
        }
    }

    public static boolean isCompatibleWithBackendData(@NotNull ProjectSpaceManager psm, @NotNull WebAPI<?> api) throws InterruptedException, TimeoutException {
        if (isDataIncompatible(psm.getFingerIdData(1), () -> api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(psm.getFingerIdData(-1), () -> api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        if (isDataIncompatible(psm.getCanopusCfData(1), () -> api.getCanopusCfData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(psm.getCanopusCfData(-1), () -> api.getCanopusCfData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        if (isDataIncompatible(psm.getCanopusNpcData(1), () -> api.getCanopusNpcData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(psm.getCanopusNpcData(-1), () -> api.getCanopusNpcData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        return true;
    }

    private static <FP extends FingerprintVersion, Data extends FingerprintData<FP>> boolean isDataIncompatible(Optional<Data> psmData, IOFunctions.IOSupplier<Data> webCall) throws InterruptedException, TimeoutException {
        if (psmData.isPresent()) {
            Data webData = NetUtils.tryAndWait(webCall::get, () -> NetUtils.checkThreadInterrupt(Thread.currentThread()));
            return !psmData.get().identical(webData);
        }
        return false;
    }
}


