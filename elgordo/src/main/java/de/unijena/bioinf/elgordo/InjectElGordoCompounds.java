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
 * El-Gordo lipid species is added to CSI:FingerID candidate list if available.
 * The lipid species will only be available if El-Gordo predicts that the MS/MS is a lipid spectrum.
 * If this parameter is set to 'false' El-Gordo will still be executed and e.g. improve the fragmentation
 * tree, but the computed lipid species will not be added to the list of candidates.
 */
public class InjectElGordoCompounds implements Ms2ExperimentAnnotation {
    public static final InjectElGordoCompounds TRUE = new InjectElGordoCompounds(true);
    public static final InjectElGordoCompounds FALSE = new InjectElGordoCompounds(false);

    public final boolean value;

    private InjectElGordoCompounds(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static InjectElGordoCompounds newInstance(@DefaultProperty boolean value){
        return value ? TRUE : FALSE;
    }
}
