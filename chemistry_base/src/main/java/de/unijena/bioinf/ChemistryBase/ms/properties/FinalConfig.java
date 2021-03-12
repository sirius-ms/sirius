/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.properties;

import de.unijena.bioinf.ms.properties.ParameterConfig;

/**
 * This config represents the merged config to
 * be converted into actual Objects which will
 * then be annotated to MS2Experiment for actual usage in the algorithms
 * This representation is NOT stored in the MS2Experiment since this might be dropped from Memory from time to time
 * but rather in the CompoundContainer and will
 * not be serialized. However it is the base for the serialization of the updated ProjectSpaceConfig.
 */
public class FinalConfig extends ConfigAnnotation {
    public FinalConfig(ParameterConfig config) {
        super(config);
    }
}
