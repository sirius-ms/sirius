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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of a fingerblast job
 * We might add additional information in future like:
 * - used database
 * - used scoring method
 */
public class FingerblastResult extends AbstractFingerblastResult<TopCSIScore> {

    public FingerblastResult(List<Scored<FingerprintCandidate>> results) {
        super(results, TopCSIScore::new);
    }

    public FBCandidateFingerprints getCandidateFingerprints(){
        return new FBCandidateFingerprints(
                results.stream().map(SScored::getCandidate).map(FingerprintCandidate::getFingerprint)
                        .collect(Collectors.toList()));
    }

    public FBCandidates getCandidates(){
        return new FBCandidates(results.stream().map(s -> new Scored<>(new CompoundCandidate(s.getCandidate()),s.getScore())).collect(Collectors.toList()));
    }
}
