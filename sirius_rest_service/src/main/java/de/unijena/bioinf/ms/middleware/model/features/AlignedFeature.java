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

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information
 * that might be displayed in some summary view.
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlignedFeature {
    @Schema(enumAsRef = true, name = "AlignedFeatureOptField", nullable = true)
    public enum OptField {none, msData, topAnnotations, topAnnotationsDeNovo}

    // identifier
    @NotNull
    protected String alignedFeatureId;

    protected String compoundId;

    // identifier source
    protected String name;

    protected String tag;

    /**
     * Externally provided FeatureId (e.g. by some preprocessing tool).
     * This FeatureId is NOT used by SIRIUS but is stored to ease mapping information back to the source.
     */
    protected String externalFeatureId;

    // additional attributes
    protected Double ionMass;

    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected int charge;

    @Schema(nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    protected Set<String> detectedAdducts;

    @Schema(nullable = true)
    protected Double rtStartSeconds;
    @Schema(nullable = true)
    protected Double rtEndSeconds;

    /**
     * Quality of this feature.
     */
    protected DataQuality quality;
    /**
     * If true, the feature has at lease one MS1 spectrum
     */
    protected boolean hasMs1;
    /**
     * If true, the feature has at lease one MS/MS spectrum
     */
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
     * Null if it was not requested und non-null otherwise.
     */
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotations;

    /**
     * Top de novo annotations of this feature.
     * The FormulaCandidate with the highest SiriusScore is returned. MSNovelist structureAnnotation and
     * CANOPUS compoundClasses correspond to the FormulaCandidate.
     *
     * Null if it was not requested und non-null otherwise.
     */
    @Schema(nullable = true)
    protected FeatureAnnotations topAnnotationsDeNovo;


    /**
     * Write lock for this feature. If the feature is locked no write operations are possible.
     * True if any computation is modifying this feature or its results
     */
    protected boolean computing;
}
