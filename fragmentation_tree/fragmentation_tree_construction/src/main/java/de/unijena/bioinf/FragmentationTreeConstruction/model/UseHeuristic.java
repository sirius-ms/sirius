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

package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import lombok.Getter;

@Getter
public class UseHeuristic implements Ms2ExperimentAnnotation {

    public final int mzToUseHeuristic;
    public final int mzToUseHeuristicOnly;

    private UseHeuristic() {
        this(0,0);
    }


    private UseHeuristic(int mzToUseHeuristic, int mzToUseHeuristicOnly) {
        this.mzToUseHeuristic = mzToUseHeuristic;
        this.mzToUseHeuristicOnly = mzToUseHeuristicOnly;
    }

    /**
     * @param mzToUsHeuristic     Set minimum m/z to enable heuristic preprocessing. The heuristic will be used to initially rank the formula candidates. The Top (NumberOfCandidates) candidates will then be computed exactly by solving the ILP.
     * @param mzToUsHeuristicOnly Set minimum m/z to only use heuristic tree computation. No exact tree computation (ILP) will be performed for this compounds.
     */
    @DefaultInstanceProvider
    public static UseHeuristic newInstance(
            @DefaultProperty(propertyKey = "mzToUseHeuristic") int mzToUsHeuristic,
            @DefaultProperty(propertyKey = "mzToUseHeuristicOnly") int mzToUsHeuristicOnly
    ) {
        return new UseHeuristic(mzToUsHeuristic, mzToUsHeuristicOnly);
    }
}
