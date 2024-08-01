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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public abstract class AbstractFingerblastResult<S extends FormulaScore> implements ResultAnnotation {

    protected final List<Scored<FingerprintCandidate>> results;
    private final Function<Double, S> scoreCreator;

    public AbstractFingerblastResult(List<Scored<FingerprintCandidate>> results, Function<Double, S> scoreCreator) {
        this.results = results;
        this.scoreCreator = scoreCreator;
    }

    public List<Scored<FingerprintCandidate>> getResults() {
        return Collections.unmodifiableList(results);
    }

    public S getTopHitScore() {
        if (results == null || results.isEmpty())
            return null;
        return scoreCreator.apply(results.get(0).getScore());
    }
}
