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


package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.middleware.compounds.model.CompoundClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompoundClasses {

    protected CompoundClass npcPathway;

    protected CompoundClass npcSuperclass;

    protected CompoundClass npcClass;

    protected CompoundClass classyFireMostSpecific;

    protected CompoundClass classyFireLevel5;

    protected CompoundClass classyFireClass;
    protected CompoundClass classyFireSubClass;

    protected CompoundClass classyFireSuperClass;

    public static CompoundClasses of(CanopusResult canopusResult) {
        //todo implement
        return new CompoundClasses();
    }
}
