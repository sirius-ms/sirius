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

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.elgordo.TagStructuresByElGordo;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * User/developer friendly parameter subset for the CSI:FingerID structure db search tool.
 * Needs results from FingerprintPrediction and Canopus Tool
 */
@Getter
@Setter
public class StructureDbSearch extends Tool<FingerblastOptions> {
    /**
     * Structure databases to search in, If expansive search is enabled this DB selection will be expanded to PubChem
     * if not high confidence hit was found in the selected databases.
     *
     * Defaults to BIO + Custom Databases. Possible values are available to Database API.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> structureSearchDBs;

    /**
     * Candidates matching the lipid class estimated by El Gordo will be tagged.
     * The lipid class will only be available if El Gordo predicts that the MS/MS is a lipid spectrum.
     * If this parameter is set to 'false' El Gordo will still be executed and e.g. improve the fragmentation
     * tree, but the matching structure candidates will not be tagged if they match lipid class.
     */
    Boolean tagStructuresWithLipidClass;

    /**
     * Expansive search mode.
     * Expansive search will expand the search space to whole PubChem in case no hit with reasonable confidence was
     * found in one of the specified databases (structureSearchDBs).
     *
     * Possible Values
     * OFF - No expansive search is performed
     * EXACT - Use confidence score in exact mode: Only molecular structures identical to the true structure should count as correct identification.
     * APPROXIMATE - Use confidence score in approximate mode: Molecular structures hits that are close to the true structure should count as correct identification.
     */
    @Schema(enumAsRef = true)
    ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;


    public StructureDbSearch(@NotNull List<CustomDataSources.Source> structureSearchDBs) {
        this();
        this.structureSearchDBs = structureSearchDBs.stream().distinct().map(CustomDataSources.Source::name).toList();
        tagStructuresWithLipidClass = PropertyManager.DEFAULTS.
                createInstanceWithDefaults(TagStructuresByElGordo.class).value;
        expansiveSearchConfidenceMode = PropertyManager.DEFAULTS.
                createInstanceWithDefaults(ExpansiveSearchConfidenceMode.class).confidenceScoreSimilarityMode;
    }

    private StructureDbSearch() {
        super(FingerblastOptions.class);
    }


    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putNonNull("TagStructuresByElGordo", tagStructuresWithLipidClass)
                .putNonNullObj("StructureSearchDB", structureSearchDBs, db -> String.join(",", db).toLowerCase(Locale.ROOT))
                .putNonNull("ExpansiveSearchConfidenceMode.confidenceScoreSimilarityMode", expansiveSearchConfidenceMode)
                .toUnmodifiableMap();
    }
}
