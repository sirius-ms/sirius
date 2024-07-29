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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MsNovelistFingerblastResult extends AbstractFingerblastResult<TopMsNovelistScore> {
    private final double[] rnnScores;

    public MsNovelistFingerblastResult(List<Scored<FingerprintCandidate>> results, double[] rnnScores) {
        super(results, TopMsNovelistScore::new);
        this.rnnScores = rnnScores;
    }

    public double getRnnScore(int i){
        return rnnScores[i];
    }

    public MsNovelistFBCandidateFingerprints getCandidateFingerprints(){
        return new MsNovelistFBCandidateFingerprints(
                results.stream().map(SScored::getCandidate).map(FingerprintCandidate::getFingerprint)
                        .collect(Collectors.toList()));
    }

    public MsNovelistFBCandidates getCandidates() {
        List<Scored<MsNovelistCompoundCandidate>> msnCandidates = new ArrayList<>(results.size());
        for (int i = 0; i < rnnScores.length; i++){
            Scored<FingerprintCandidate> c = results.get(i);
            msnCandidates.add(new Scored<>(new MsNovelistCompoundCandidate(c.getCandidate(), rnnScores[i]), c.getScore()));
        }

        return new MsNovelistFBCandidates(msnCandidates);
    }
}
