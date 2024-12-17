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

package de.unijena.bioinf.ms.middleware.model.spectra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PeakAnnotation {
    /**
     * Identifier of the peak/fragment. Can be used to map fragments and peaks
     * among fragmentation trees and spectra.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer fragmentId;

    /**
     * Molecular formula that has been annotated to this peak
     */
    @Schema(nullable = true)
    private String molecularFormula;

    /**
     * Adduct that has been annotated to this peak
     */
    @Schema(nullable = true)
    private String adduct;

    /**
     * Exact mass of the annotated molecular formula and adduct
     */
    @Schema(nullable = true)
    private Double exactMass;

    /**
     * Absolute mass deviation of the exact mass to the measured peak mass in mDa
     */
    @Schema(nullable = true)
    private Double massDeviationMz;

    /**
     * Relative mass deviation of the exact mass to the measured peak mass in ppm
     */
    @Schema(nullable = true)
    private Double massDeviationPpm;

    /**
     * Absolute mass deviation of the exact mass to the recalibrated peak mass in mDa
     */
    @Schema(nullable = true)
    private Double recalibratedMassDeviationMz;

    /**
     * Relative mass deviation of the exact mass to the recalibrated peak mass in ppm
     */
    @Schema(nullable = true)
    private Double recalibratedMassDeviationPpm;

    /**
     * Link to the parent peak connected via the neutral loss from the fragmentation tree.
     */
    @Schema(nullable = true)
    private ParentPeak parentPeak;

    /**
     * Array/List of indices of the atoms of the structure candidate that are part of this fragments substructure
     * (highlighted atoms)
     */
    @Schema(nullable = true, title = "EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.")
    private int[] substructureAtoms;

    /**
     * Array/List of indices of the bonds of the structure candidate that are part of this fragments substructure
     * (highlighted bonds)
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true, title = "EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.")
    private int[] substructureBonds;

    /**
     * Array/List of indices of the bonds of the structure candidate that need to be cut to produce this fragments
     * substructure (highlighted cutted bonds).
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true, title = "EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.")
    private int[] substructureBondsCut;

    /**
     * This score roughly reflects the probability of this fragment forming.
     *
     * This is the score of the path from root to this node which has the maximal score or "profit".
     * The score of a path is equal to the sum of scores of its contained fragments and edges.
     * Note: Refers to 'totalScore' in CombinatorialNode
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true, title = "EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.")
    private Float substructureScore;

    /**
     * Number of hydrogens rearrangements needed to match the substructure to the fragment formula.
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true, title = "EXPERIMENTAL: This field is experimental and may be changed (or even removed) without notice until it is declared stable.")
    private Integer hydrogenRearrangements;




}
