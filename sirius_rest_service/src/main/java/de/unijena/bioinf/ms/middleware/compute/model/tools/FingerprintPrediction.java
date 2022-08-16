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

package de.unijena.bioinf.ms.middleware.compute.model.tools;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.fingerid.annotations.FormulaResultThreshold;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * User/developer friendly parameter subset for the CSI:FingerID Fingerprint tool
 */

@Getter
@Setter
public class FingerprintPrediction extends Tool<FingerprintOptions> {


        /**
         * If true, an adaptive soft threshold will be applied to only compute Fingerprints for promising formula candidates
         * Enabling is highly recommended.
         */
        boolean useScoreThreshold = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaResultThreshold.class).useThreshold();

        public FingerprintPrediction() {
                super(FingerprintOptions.class);
        }

        @JsonIgnore
        @Override
        public Map<String, String> asConfigMap() {
                return Map.of("FormulaResultThreshold", String.valueOf(useScoreThreshold));
        }
}
