/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FoldChangeMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.sirius.ComputedSubtools;
import de.unijena.bioinf.projectspace.IndexField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information
 * that might be displayed in some summary view.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlignedFeature implements Taggable {
    // topAnnotationsSummary are the indext summary field that can be retrieved very fast and are needed for quick gui loading.
    @Schema(name = "AlignedFeatureOptField", nullable = true)
    public enum OptField {none, msData, topAnnotationsSummary, topAnnotations, topAnnotationsDeNovo, computedTools, qualities, tags}

    public static final EnumSet<OptField> INDEXED_OPT_FIELDS =  EnumSet.of(
            OptField.tags, OptField.computedTools, OptField.topAnnotationsSummary, OptField.qualities);

    // identifier
    @IndexField(documentId = true, sortable = true, defaultSearchField = true)
    @NotNull
    protected String alignedFeatureId;

    @IndexField
    protected String compoundId;

    // identifier source
    @IndexField(fullTextSearch = true, defaultSearchField = true, sortable = true)
    protected String name;

    /**
     * Externally provided FeatureId (e.g. by some preprocessing tool).
     * This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source.
     */
    @IndexField(defaultSearchField = true)
    protected String externalFeatureId;

    // additional attributes
    @IndexField(sortable = true)
    protected Double ionMass;

    /**
     * Ion mode (charge) this feature has been measured in.
     */
    @IndexField
    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected int charge;

    /**
     * Adducts of this feature that have been detected during preprocessing.
     * Never empty: if no adduct could be detected, the unknown ion type matching the feature's
     * charge ([M+?]+ or [M+?]-) is reported instead, so every feature is filterable by adduct.
     */
    @IndexField
    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected Set<String> detectedAdducts;

    @IndexField
    @Schema(nullable = true)
    protected Double rtStartSeconds;
    @IndexField
    @Schema(nullable = true)
    protected Double rtEndSeconds;
    @IndexField(sortable = true)
    @Schema(nullable = true)
    protected Double rtApexSeconds;

    /**
     * Overall Quality of this feature.
     * If no Quality data are available for this feature the value is NOT_APPLICABLE
     */
    @IndexField(sortable = true)
    @Schema
    @Builder.Default
    @NotNull
    protected DataQuality quality = DataQuality.NOT_APPLICABLE;
    /**
     * If true, the feature has at least one MS1 spectrum
     */
    @IndexField
    protected boolean hasMs1;
    /**
     * If true, the feature has at least one MS/MS spectrum
     */
    @IndexField
    protected boolean hasMsMs;

    /**
     * Mass Spec data of this feature (input data)
     */
    @Schema(nullable = true)
    protected MsData msData;

    /**
     * Top annotations of this feature.
     * If a CSI:FingerID structureAnnotation is available, the FormulaCandidate that corresponds to the
     * structureAnnotation is returned. Otherwise, it's the FormulaCandidate with the highest SiriusScore is returned.
     * CANOPUS Compound classes correspond to the FormulaCandidate no matter how it was selected
     *
     * Null if it was not requested and non-null otherwise.
     */
    @IndexField
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotations;

    /**
     * Top de novo annotations of this feature.
     * The FormulaCandidate with the highest SiriusScore is returned. MSNovelist structureAnnotation and
     * CANOPUS compoundClasses correspond to the FormulaCandidate.
     *
     * Null if it was not requested and non-null otherwise.
     */
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotationsDeNovo;


    /**
     * Write lock for this feature. If the feature is locked no write operations are possible.
     * True if any computation is modifying this feature or its results
     */
    protected boolean computing;

    @IndexField
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED, description =
                    "Specifies which tools have been executed for this feature. " +
                    "Can be used to estimate which results can be expected. " +
                            "Null if it was not requested and non-null otherwise.")
    @JsonIgnoreProperties(value = { "alignedFeatureId" })
    protected ComputedSubtools computedTools;


    /**
     * Qualities per top level quality category.
     */
    @IndexField
    @Schema(nullable = true)
    protected Map<String, DataQuality> qualities;

    /**
     * Key: tagName, value: tag
     */
    @IndexFieldWithMapper(mapper = TagMapper.class)
    @Schema(nullable = true)
    protected Map<String, Tag> tags;


    /**
     * Aggregated fold change of sample runs vs blank runs for this aligned feature
     * NULL of not a sample run or no fold change exists
     * NOTE: This field is mainly for search index building and therefore hidden from the api
     */
    @IndexFieldWithMapper(mapper = FoldChangeMapper.AlignedFeatureFoldChange.class)
    @Schema(nullable = true, hidden = true)
    protected List<Statistics> stats;
}
