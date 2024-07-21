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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public abstract class AbstractProjectSpaceManager implements ProjectSpaceManager {

    public synchronized void writeFingerIdDataIfMissing(WebAPI<?> api) throws IOException {
        if (!hasFingerIdData(1) || !hasFingerIdData(-1)) {
            //load fingerid data
            final FingerIdData pos = api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE);
            final FingerIdData neg = api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
            writeFingerIdData(pos, neg);
        }
    }

    public synchronized void writeCanopusDataIfMissing(WebAPI<?> api) throws IOException {
        if (!hasCanopusCfData(1) || !hasCanopusCfData(-1)
                || !hasCanopusNpcData(1) || !hasCanopusNpcData(-1)
        ) {
            // load ClassyFire client data
            final CanopusCfData cfPos = api.getCanopusCfData(PredictorType.CSI_FINGERID_POSITIVE);
            final CanopusCfData cfNeg = api.getCanopusCfData(PredictorType.CSI_FINGERID_NEGATIVE);
            // load NPC client data
            final CanopusNpcData npcPos = api.getCanopusNpcData(PredictorType.CSI_FINGERID_POSITIVE);
            final CanopusNpcData npcNeg = api.getCanopusNpcData(PredictorType.CSI_FINGERID_NEGATIVE);
            //write to ps
            writeCanopusData(cfPos, cfNeg, npcPos, npcNeg);
        }
    }

    public synchronized boolean isCompatibleWithBackendData(@NotNull WebAPI<?> api) throws InterruptedException, TimeoutException {
        if (isDataIncompatible(getFingerIdData(1), () -> api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(getFingerIdData(-1), () -> api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        if (isDataIncompatible(getCanopusCfData(1), () -> api.getCanopusCfData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(getCanopusCfData(-1), () -> api.getCanopusCfData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        if (isDataIncompatible(getCanopusNpcData(1), () -> api.getCanopusNpcData(PredictorType.CSI_FINGERID_POSITIVE)))
            return false;
        if (isDataIncompatible(getCanopusNpcData(-1), () -> api.getCanopusNpcData(PredictorType.CSI_FINGERID_NEGATIVE)))
            return false;
        return true;
    }

    private <FP extends FingerprintVersion, Data extends FingerprintData<FP>> boolean isDataIncompatible(Optional<Data> psmData, IOFunctions.IOSupplier<Data> webCall) throws InterruptedException, TimeoutException {
        if (psmData.isPresent()) {
            Data webData = NetUtils.tryAndWait(webCall::get, () -> NetUtils.checkThreadInterrupt(Thread.currentThread()));
            return !psmData.get().identical(webData);
        }
        return false;
    }
}
