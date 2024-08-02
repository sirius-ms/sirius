/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * use this parameter if you want to force to report at least
 * numberOfResultsToKeepPerIonization results per ionization.
 * if le 0, this parameter will have no effect and just the top
 * numberOfResultsToKeep results will be reported.
 * */
@DefaultProperty
public class NumberOfCandidatesPerIonization implements Ms2ExperimentAnnotation {
    public static final NumberOfCandidatesPerIonization ZERO = new NumberOfCandidatesPerIonization(0);
    public static final NumberOfCandidatesPerIonization MIN_VALUE = new NumberOfCandidatesPerIonization(Integer.MIN_VALUE);
    public static final NumberOfCandidatesPerIonization MAX_VALUE = new NumberOfCandidatesPerIonization(Integer.MAX_VALUE);
    public static final NumberOfCandidatesPerIonization ONE = new NumberOfCandidatesPerIonization(1);

    public final int value;

    private NumberOfCandidatesPerIonization() {
       this(0);
    }
    public NumberOfCandidatesPerIonization(int value) {
        this.value = value;
    }
}
