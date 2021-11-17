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

package de.unijena.bioinf.webapi;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumMap;

public abstract class AbstractWebAPI<D extends AbstractChemicalDatabase> implements WebAPI<D> {

    protected final AuthService authService;

    protected AbstractWebAPI(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthService getAuthService() {
        return authService;
    }


    //caches predicors so that we do not have to download the statistics and fingerprint info every time
    private final EnumMap<PredictorType, StructurePredictor> fingerIdPredictors = new EnumMap<>(PredictorType.class);

    public @NotNull StructurePredictor getStructurePredictor(@NotNull PredictorType type) throws IOException {
        synchronized (fingerIdPredictors) {
            if (!fingerIdPredictors.containsKey(type)) {
                final CSIPredictor p = new CSIPredictor(type, this);
                p.initialize();
                fingerIdPredictors.put(type, p);
            }
        }
        return fingerIdPredictors.get(type);
    }


    private final EnumMap<PredictorType, FingerIdData> fingerIdData = new EnumMap<>(PredictorType.class);

    public FingerIdData getFingerIdData(@NotNull PredictorType predictorType) throws IOException {
        synchronized (fingerIdData) {
            if (!fingerIdData.containsKey(predictorType))
                fingerIdData.put(predictorType, getFingerIdDataUncached(predictorType));
        }
        return fingerIdData.get(predictorType);
    }
    protected abstract FingerIdData getFingerIdDataUncached(@NotNull PredictorType predictorType) throws IOException;


    private final EnumMap<PredictorType, CanopusCfData> cfData = new EnumMap<>(PredictorType.class);

    public final CanopusCfData getCanopusCfData(@NotNull PredictorType predictorType) throws IOException {
        synchronized (cfData) {
            if (!cfData.containsKey(predictorType))
                cfData.put(predictorType, getCanopusCfDataUncached(predictorType));
        }
        return cfData.get(predictorType);
    }

    protected abstract CanopusCfData getCanopusCfDataUncached(@NotNull PredictorType predictorType) throws IOException;

    private final EnumMap<PredictorType, CanopusNpcData> npcData = new EnumMap<>(PredictorType.class);

    public final CanopusNpcData getCanopusNpcData(@NotNull PredictorType predictorType) throws IOException {
        synchronized (npcData) {
            if (!npcData.containsKey(predictorType))
                npcData.put(predictorType, getCanopusNpcDataUncached(predictorType));
        }
        return npcData.get(predictorType);
    }

    protected abstract CanopusNpcData getCanopusNpcDataUncached(@NotNull PredictorType predictorType) throws IOException;

}
