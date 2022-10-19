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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractWebAPI<D extends AbstractChemicalDatabase> implements WebAPI<D> {

    protected final AuthService authService;

    protected AbstractWebAPI(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthService getAuthService() {
        return authService;
    }


    //caches predictors so that we do not have to download the statistics and fingerprint info every time
    private final Map<PredictorType, StructurePredictor> fingerIdPredictors = new ConcurrentHashMap<>();


    public @NotNull StructurePredictor getStructurePredictor(@NotNull PredictorType predictorType) throws IOException {
        try {
            return fingerIdPredictors.computeIfAbsent(predictorType, pt -> {
                try {
                    final CSIPredictor p = new CSIPredictor(pt);
                    p.initialize(this);
                    return p;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw e;
        }
    }


    private final Map<PredictorType, FingerIdData> fingerIdData = new ConcurrentHashMap<>();


    public FingerIdData getFingerIdData(@NotNull PredictorType predictorType) throws IOException {
        try {
            return fingerIdData.computeIfAbsent(predictorType, pt -> {
                try {
                    return getFingerIdDataUncached(pt);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw e;
        }
    }

    protected abstract FingerIdData getFingerIdDataUncached(@NotNull PredictorType predictorType) throws IOException;


    private final Map<PredictorType, CanopusCfData> cfData = new ConcurrentHashMap<>();

    public final CanopusCfData getCanopusCfData(@NotNull PredictorType predictorType) throws IOException {
        try {
            return cfData.computeIfAbsent(predictorType, pt -> {
                try {
                    return getCanopusCfDataUncached(pt);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw e;
        }
    }

    protected abstract CanopusCfData getCanopusCfDataUncached(@NotNull PredictorType predictorType) throws IOException;

    private final Map<PredictorType, CanopusNpcData> npcData = new ConcurrentHashMap<>();

    public final CanopusNpcData getCanopusNpcData(@NotNull PredictorType predictorType) throws IOException {
        try {
            return npcData.computeIfAbsent(predictorType, pt -> {
                try {
                    return getCanopusNpcDataUncached(pt);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw e;
        }
    }

    protected abstract CanopusNpcData getCanopusNpcDataUncached(@NotNull PredictorType predictorType) throws IOException;

}
