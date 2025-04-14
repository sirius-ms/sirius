/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.AnalogueSearchSettings;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.IdentitySearchSettings;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import de.unijena.bioinf.ms.middleware.model.compute.NullCheckMapBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * User/developer friendly parameter subset for the Spectral library search tool.
 */
@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpectralLibrarySearch extends Tool<SpectraSearchOptions> {
    /**
     * Structure Databases with Reference spectra to search in.
     * <p>
     * Defaults to BIO + Custom Databases. Possible values are available to Database API.
     */
    @Schema(nullable = true)
    List<String> spectraSearchDBs;

    /**
     * Maximum allowed mass deviation in ppm for matching the precursor. If not specified, the same value as for the peaks is used.
     */
    @Schema(nullable = true)
    private Double precursorDeviationPpm;

    /**
     * Minimal spectral similarity of a spectral match to be considered a hit.
     */
    @Schema(nullable = true)
    private Float minSimilarity;

    /**
     * Minimal number of matching peaks of a spectral match to be considered a hit.
     */
    @Schema(nullable = true)
    private Integer minNumOfPeaks;



    /**
     * Enable analogue search in addition to the identity spectral library search
     */
    @Schema
    private Boolean enableAnalogueSearch = false;

    /**
     * Minimal spectral similarity of a spectral match to be considered an analogue hit.
     */
    @Schema(nullable = true)
    private Float minSimilarityAnalogue;

    /**
     * Minimal number of matching peaks of a spectral match to be considered an analogue hit.
     */
    @Schema(nullable = true)
    private Integer minNumOfPeaksAnalogue;







    /**
     * NO LONGER SUPPORTED (IGNORED)
     * Specify scoring method to match spectra
     * INTENSITY: Intensity weighted. Each peak matches at most one peak in the other spectrum.
     * GAUSSIAN: Treat peaks as (un-normalized) Gaussians and score overlapping areas of PDFs. Each peak might score against multiple peaks in the other spectrum.
     * MODIFIED_COSINE:  This algorithm requires that there is at most one pair of peaks (u,v) where the m/z of u and v are within the allowed mass tolerance. To be used for analog search with different precursor masses.
     */
    @Schema(nullable = true)
    @Deprecated(forRemoval = true)
    private SpectralMatchingType scoring = null;

    /**
     * NO LONGER SUPPORTED (IGNORED)
     * Maximum allowed mass deviation in ppm for matching peaks.
     */
    @Schema(nullable = true)
    @Deprecated(forRemoval = true)
    private Double peakDeviationPpm = null;


    private SpectralLibrarySearch() {
        super(SpectraSearchOptions.class);
    }

    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putIfNonNullObj("SpectralSearchDB", spectraSearchDBs, db -> String.join(",", db))

                .putIfNonNull("IdentitySearchSettings.precursorDeviation", precursorDeviationPpm, it -> it + " ppm")
                .putIfNonNull("IdentitySearchSettings.minSimilarity", minSimilarity)
                .putIfNonNull("IdentitySearchSettings.minNumOfPeaks", minNumOfPeaks)

                .putIfNonNull("AnalogueSearchSettings.enabled", enableAnalogueSearch)
                .putIfNonNull("AnalogueSearchSettings.minSimilarity", minSimilarityAnalogue)
                .putIfNonNull("AnalogueSearchSettings.minNumOfPeaks", minNumOfPeaksAnalogue)

                .putIfNonNull("SpectralSearchLog", 0)
                .toUnmodifiableMap();
    }

    public static SpectralLibrarySearch buildDefault() {
        return builderWithDefaults().build();
    }

    public static SpectralLibrarySearch.SpectralLibrarySearchBuilder<?, ?> builderWithDefaults() {
        IdentitySearchSettings libSearchSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class);
        AnalogueSearchSettings analogueSearchSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AnalogueSearchSettings.class);

        return SpectralLibrarySearch.builder()
                .enabled(true)
                .spectraSearchDBs(null)
                .precursorDeviationPpm(libSearchSettings.getPrecursorDeviation().getPpm())
                .minSimilarity(libSearchSettings.getMinSimilarity())
                .minNumOfPeaks(libSearchSettings.getMinNumOfPeaks())
                .enableAnalogueSearch(analogueSearchSettings.isEnabled())
                .minSimilarityAnalogue(analogueSearchSettings.getMinSimilarity())
                .minNumOfPeaksAnalogue(analogueSearchSettings.getMinNumOfPeaks())
                //deprecated fields are set to null.
                .peakDeviationPpm(null)
                .scoring(null);
    }
}
