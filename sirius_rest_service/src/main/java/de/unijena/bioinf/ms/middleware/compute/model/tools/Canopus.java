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
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * User/developer friendly parameter subset for the CANOPUS tool
 * CANOPUS is parameter free, so this Object is just a flag that canopus should be executed.
 */
@Getter
@Setter
public class Canopus extends Tool<CanopusOptions> {
    public Canopus() {
        super(CanopusOptions.class);
    }


    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return Map.of();
    }
}
