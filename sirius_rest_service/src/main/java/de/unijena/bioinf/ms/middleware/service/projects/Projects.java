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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;

public class Projects {
    public static LipidAnnotation asLipidAnnotation(FTree fTree) {
        return fTree.getAnnotation(LipidSpecies.class).map(ls -> LipidAnnotation.builder()
                .lipidSpecies(ls.toString())
                .lipidMapsId(ls.getLipidClass().getLipidMapsId())
                .lipidClassName(ls.getLipidClass().longName())
                .chainsUnknown(ls.chainsUnknown())
                .hypotheticalStructure(ls.generateHypotheticalStructure().orElse(null))
                .build()
        ).orElse(LipidAnnotation.builder().build());
    }
}
