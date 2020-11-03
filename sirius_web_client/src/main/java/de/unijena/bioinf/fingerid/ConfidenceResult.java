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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceResult implements ResultAnnotation {
    public static final ConfidenceResult NaN = new ConfidenceResult(Double.NEGATIVE_INFINITY, null);

    // bio confidence
    // pubchem confidence
    public final ConfidenceScore score;
    public Scored<FingerprintCandidate> top_hit;


    public ConfidenceResult(ConfidenceScore score) {
        this.score = score;
    }

    public ConfidenceResult(double confidence, Scored<FingerprintCandidate> top_hit){
        //this is just to supress the warning
        this.score = Double.isNaN(confidence) ? FormulaScore.NA(ConfidenceScore.class) : new ConfidenceScore(confidence);
        this.top_hit=top_hit;
    }
}
