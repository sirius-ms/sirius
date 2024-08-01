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

package de.unijena.bioinf.ms.stores.model;

import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.storage.blob.Compressible;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface CanopusClientDataStore extends Compressible {

    /**
     * Get CANOPUS ClassyFire client data (fingerprint definition/version) for the given predictor
     * @param type Positive or negative predictor type
     * @return CANOPUS ClassyFire client data in tsv format
     */
    Optional<InputStream> getCanopusCfClientData(PredictorType type) throws IOException;
    /**
     * Get CANOPUS NPC client data (fingerprint definition/version) for the given predictor
     * @param type Positive or negative predictor type
     * @return CANOPUS NPC client data in tsv format
     */
    Optional<InputStream> getCanopusNpcClientData(PredictorType type) throws IOException;

}
