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

package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.FeatureCreator;

import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import org.jetbrains.annotations.Nullable;

/**
 * Created by martin on 16.07.18.
 */
public class FptLengthFeatureHit implements FeatureCreator {
    int min_quartil=1;
    int max_quartil=99;

    Scored<FingerprintCandidate>[] rankedCandidates_filtered;

    public FptLengthFeatureHit(Scored<FingerprintCandidate>[] rankedCandidates_filtered){
        this.rankedCandidates_filtered=rankedCandidates_filtered;
    }

    @Override
    public int weight_direction() {
        return 1;
    }

    @Override
    public int min_quartil() {
        return min_quartil;
    }

    @Override
    public int max_quartil() {
        return max_quartil;
    }

    @Override
    public double[] computeFeatures(@Nullable ParameterStore ignored) {

        double[] length =  new double[1];

        length[0]= rankedCandidates_filtered[0].getCandidate().getFingerprint().cardinality();

        return length;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public void setMinQuartil(int quartil) {
        min_quartil=quartil;
    }

    @Override
    public void setMaxQuartil(int quartil) {
        max_quartil=quartil;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 0;
    }

    @Override
    public String[] getFeatureNames() {

        String[] name = new String[1];
        name[0] = "fptLengthHit";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
