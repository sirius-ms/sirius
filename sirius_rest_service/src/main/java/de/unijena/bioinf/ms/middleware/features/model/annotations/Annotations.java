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

package de.unijena.bioinf.ms.middleware.features.model.annotations;

import lombok.Getter;
import lombok.Setter;

/**
 * Summary of the results of a feature (aligned over runs). Can be added to a AlignedFeature.
 * It is null within a AlignedFeature if it was not requested und non-null otherwise.
 * The different annotation fields within this summary object are null if the corresponding
 * feature does not contain the represented results. If fields are non-null
 * the corresponding result has been computed but might still be empty.
 * */
@Getter
@Setter
public class Annotations {
    //result previews
    protected FormulaCandidate formulaAnnotation; // SIRIUS + ZODIAC
    protected StructureCandidate structureAnnotation; // CSI:FingerID
    protected CompoundClasses compoundClassAnnotation; // CANOPUS
}
