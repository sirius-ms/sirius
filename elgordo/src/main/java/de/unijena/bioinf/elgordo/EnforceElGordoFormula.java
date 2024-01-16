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

package de.unijena.bioinf.elgordo;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * El Gordo may predict that an MS/MS spectrum is a lipid spectrum.
 * The corresponding molecular formula may be enforeced or additionally added to the set of molecular formula candidates.
 */
public class EnforceElGordoFormula implements Ms2ExperimentAnnotation {
    public static final EnforceElGordoFormula TRUE = new EnforceElGordoFormula(true);
    public static final EnforceElGordoFormula FALSE = new EnforceElGordoFormula(false);

    public final boolean value;

    private EnforceElGordoFormula(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static EnforceElGordoFormula newInstance(@DefaultProperty boolean value){
        return value ? TRUE : FALSE;
    }
}
