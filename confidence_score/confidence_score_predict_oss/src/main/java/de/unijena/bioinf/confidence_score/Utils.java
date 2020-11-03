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

package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.ChemDBs;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.Arrays;
import java.util.function.Predicate;

public class Utils {
    private Utils() {
    }

    public static Scored<FingerprintCandidate>[] condenseCandidatesByFlag(Scored<FingerprintCandidate>[] candidates, final long flags) {
        return Arrays.stream(candidates).filter(ChemDBs.inFilter(s -> s.getCandidate().getBitset(),flags)).toArray(Scored[]::new);
    }
}
