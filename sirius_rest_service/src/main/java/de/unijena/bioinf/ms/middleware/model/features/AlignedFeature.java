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
import de.unijena.bioinf.ms.middleware.model.annotations.FeatureAnnotations;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * The AlignedFeature contains the ID of a feature (aligned over runs) together with some read-only information
 * that might be displayed in some summary view.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlignedFeature {
    @Schema(enumAsRef = true, name = "AlignedFeatureOptField", nullable = true)
    public enum OptField {none, msData, topAnnotations, topAnnotationsDeNovo}

    // identifier
    @NotNull
    protected String alignedFeatureId;

    // identifier source
    protected String name;
    protected long index;

    // additional attributes
    protected Double ionMass;
    @Deprecated
    protected String adduct; //todo remove

    @Schema(nullable = true)
    protected Double rtStartSeconds;
    @Schema(nullable = true)
    protected Double rtEndSeconds;

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
