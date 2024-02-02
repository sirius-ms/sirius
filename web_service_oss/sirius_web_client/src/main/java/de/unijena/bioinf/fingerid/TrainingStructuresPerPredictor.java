/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.webapi.WebAPI;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class TrainingStructuresPerPredictor {
    private static final Object lock = new Object();
    private static volatile TrainingStructuresPerPredictor singleton;

    private final Map<PredictorType, TrainingStructuresSet> predictorTypeToInchiKeys2D;

    private TrainingStructuresPerPredictor() {
        predictorTypeToInchiKeys2D = new ConcurrentHashMap<>();
    }

    static TrainingStructuresPerPredictor getInstance() {
        if (singleton == null) {
            synchronized (lock) {
                if (singleton == null)
                    singleton = new TrainingStructuresPerPredictor();
            }
        }
        return singleton;
    }


    TrainingStructuresSet getTrainingStructuresSet(PredictorType predictorType, @NotNull WebAPI.Clients api, @NotNull OkHttpClient client) throws IOException {
        try {
            return predictorTypeToInchiKeys2D.computeIfAbsent(predictorType, pt -> {
                try {
                    return TrainingStructuresSet.of(
                            api.fingerprintClient().getTrainingStructuresAll(predictorType, client));
                } catch (IOException e) {
                    //todo nightsky: remove fallback to old version
                    LoggerFactory.getLogger(getClass()).warn("Could not load new TrainingData file. Falling back to old version without extra dnn structures.", e);
                    try {
                        return TrainingStructuresSet.builder()
                                .kernelInchiKeys(
                                        Arrays.stream(api.fingerprintClient().getTrainingStructures(predictorType, client).getTrainingStructures())
                                                .map(InChI::key2D).collect(Collectors.toSet()))
                                .extraInchiKeys(Set.of())
                                .build();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw e;
        }
    }
}
