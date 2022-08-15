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

package de.unijena.bioinf.ms.middleware.compounds.model;

import de.unijena.bioinf.ms.middleware.formulas.model.CompoundClasses;
import de.unijena.bioinf.ms.middleware.formulas.model.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.formulas.model.StructureCandidate;
import lombok.Getter;
import lombok.Setter;

/**
 * Summary of the results of a Compound. Can be added to a CompoundId.
 * It is not null within a CompoundId if it was not requested und non null otherwise
 * The different summary fields within this summary are null if the corresponding
 * compound does not contain the represented results. The content of  non NULL
 * summary field id the result was computed but is empty.
 * */
@Getter
@Setter
public class CompoundAnnotation {
    //result previews
    protected FormulaCandidate formulaAnnotation; // SIRIUS + ZODIAC
    protected StructureCandidate structureAnnotation; // CSI:FingerID
    protected CompoundClasses compoundClassAnnotation; // CANOPUS
}
