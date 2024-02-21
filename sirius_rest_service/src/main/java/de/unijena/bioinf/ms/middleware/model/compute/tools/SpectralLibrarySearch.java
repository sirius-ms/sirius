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

import de.unijena.bioinf.chemdb.annotations.SpectralAlignmentScorer;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.spectraldb.SpectraMatchingMassDeviation;
import de.unijena.bionf.spectral_alignment.SpectralAlignmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpectralLibrarySearch extends Tool<SpectraSearchOptions> {
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
    @Schema(nullable = true)
    private Double peakDeviationPpm;

    /**
     * Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used.
     */
    @Schema(nullable = true)
    private Double precursorDeviationPpm;

    /**
     * Specify scoring method to match spectra
     * INTENSITY: Intensity weighted. Each peak matches at most one peak in the other spectrum.
     * GAUSSIAN: Treat peaks as (un-normalized) Gaussians and score overlapping areas of PDFs. Each peak might score against multiple peaks in the other spectrum.
     * MODIFIED_COSINE:  This algorithm requires that there is at most one pair of peaks (u,v) where the m/z of u and v are within the allowed mass tolerance. To be used for analog search with different precursor masses.
     */
    @Schema(nullable = true, enumAsRef = true)
    private SpectralAlignmentType scoring;

    public SpectralLibrarySearch(@NotNull List<CustomDataSources.Source> spectraSearchDBs) {
        this();
        this.spectraSearchDBs = spectraSearchDBs.stream().distinct().map(CustomDataSources.Source::name).toList();
        this.peakDeviationPpm = PropertyManager.DEFAULTS.createInstanceWithDefaults(SpectraMatchingMassDeviation.class)
                .allowedPeakDeviation.getPpm();
        this.precursorDeviationPpm = PropertyManager.DEFAULTS.createInstanceWithDefaults(SpectraMatchingMassDeviation.class)
                .allowedPrecursorDeviation.getPpm();
        this.scoring = PropertyManager.DEFAULTS.createInstanceWithDefaults(SpectralAlignmentScorer.class).spectralAlignmentType;
    }

    private SpectralLibrarySearch() {
        super(SpectraSearchOptions.class);
    }

    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putNonNullObj("SpectralSearchDB", spectraSearchDBs, db -> String.join(",", db).toLowerCase(Locale.ROOT))
                .putNonNull("SpectraMatchingMassDeviation.allowedPeakDeviation", peakDeviationPpm, it -> it + " ppm")
                .putNonNull("SpectraMatchingMassDeviation.allowedPrecursorDeviation", precursorDeviationPpm, it -> it + " ppm")
                .putNonNull("SpectralAlignmentScorer", scoring)
                .toUnmodifiableMap();
    }
}
