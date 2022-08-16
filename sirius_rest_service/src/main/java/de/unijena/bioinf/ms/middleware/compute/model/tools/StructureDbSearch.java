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
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.elgordo.InjectElGordoCompounds;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User/developer friendly parameter subset for the CSI:FingerID structure db search tool.
 */
@Getter
@Setter
public class StructureDbSearch extends Tool<FingerblastOptions> {
    //todo make custom database support
    /**
     * Structure databases to search in
     */
    List<DataSource> structureSearchDBs;
    //todo add lipid class support to api
    /**
     * Candidates matching the lipid class estimated by El Gordo will be tagged.
     * The lipid class will only be available if El Gordo predicts that the MS/MS is a lipid spectrum.
     * If this parameter is set to 'false' El Gordo will still be executed and e.g. improve the fragmentation
     * tree, but the matching structure candidates will not be tagged if they match lipid class.
     */
    boolean tagLipids;

    public StructureDbSearch() {
        super(FingerblastOptions.class);
        structureSearchDBs = List.of(DataSource.BIO);
        tagLipids = PropertyManager.DEFAULTS.createInstanceWithDefaults(InjectElGordoCompounds.class).value;
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return Map.of(
                "InjectElGordoCompounds", String.valueOf(tagLipids),
                "StructureSearchDB", structureSearchDBs.stream().map(DataSource::name).collect(Collectors.joining(","))
        );
    }
}
