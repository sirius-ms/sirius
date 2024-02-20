/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class SpectralLibrarySearch extends Tool<SpectraSearchOptions>  {
    /**
     * Structure Databases with Reference spectra to search in.
     *
     * Defaults to BIO + Custom Databases. Possible values are available to Database API.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> spectraSearchDBs;

    /**
     * Maximum allowed mass deviation in ppm for matching peaks.
     */
    private Double peakDeviationPpm;

    /**
     * Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used.
     */
    private Double precursorDeviationPpm;


    public SpectralLibrarySearch(@NotNull List<CustomDataSources.Source> spectraSearchDBs) {
        this();
        this.spectraSearchDBs = spectraSearchDBs.stream().distinct().map(CustomDataSources.Source::name).toList();
    }

    private SpectralLibrarySearch() {
        super(SpectraSearchOptions.class);
    }

    @Override
    public Map<String, String> asConfigMap() {
        return null;
    }
}
