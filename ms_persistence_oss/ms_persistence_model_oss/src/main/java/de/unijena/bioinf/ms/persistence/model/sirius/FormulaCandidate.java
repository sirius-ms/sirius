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

package de.unijena.bioinf.ms.persistence.model.sirius;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FormulaCandidate extends AlignedFeatureAnnotation {
    /**
     * Unique identifier of this formula candidate
     */
    @Id
    protected long formulaId;
    /**
     * molecular formula of this formula candidate
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    @NotNull
    protected MolecularFormula molecularFormula;
    /**
     * Adduct of this formula candidate
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
    @NotNull
    protected PrecursorIonType adduct;

    /**
     * Rank of this FormulaCandidates among all other FormulaCandidates of the corresponding AlignedFeature.
     * This ranking is created on the zodiacScore (if available) or alternatively on the siriusScore.
     */
    @Nullable
    protected Integer formulaRank;
    /**
     * Sirius Score (isotope + tree score) of the formula candidate.
     * If NULL result is not available
     */
    @Nullable
    protected Double siriusScore;
    @Nullable
    protected Double isotopeScore;
    @Nullable
    protected Double treeScore;
    /**
     * Zodiac Score of the formula candidate.
     * If NULL result is not available
     */
    @Nullable
    protected Double zodiacScore;

    @JsonIgnore
    public MolecularFormula getPrecursorFormula() {
        return adduct.neutralMoleculeToPrecursorIon(molecularFormula);
    }

    @JsonIgnore
    public String getPrecursorFormulaWithCharge() {
        return getPrecursorFormula().toString() + (adduct.isPositive() ? "+":"-");
    }
}
