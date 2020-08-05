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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.confidence_score.features.SIRIUSTreeScoreFeatures;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.scores.SiriusScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by martin on 05.06.19.
 */
public class CombinedFeatureCreatorSIRIUS extends CombinedFeatureCreator{


    FeatureCreator[] featureCreators;
    private int featureCount;
    private double[] computed_features;

    public CombinedFeatureCreatorSIRIUS(List<IdentificationResult<SiriusScore>> idlist, Ms2Experiment exp) {

        ArrayList<FeatureCreator> creators = new ArrayList<>(Arrays.asList(new SIRIUSTreeScoreFeatures(idlist,exp)

        ));

        featureCount=0;
        featureCreators = new FeatureCreator[creators.size()];

        for(int i=0;i< creators.size();i++){
            featureCreators[i]=creators.get(i);
            featureCount+=creators.get(i).getFeatureSize();
        }

    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {
        computed_features= new double[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final double[] currentScores = featureCreator.computeFeatures(query,idresult);
            for (int i = 0; i < currentScores.length; i++) computed_features[pos++] = currentScores[i];
        }
        return computed_features;
    }

    @Override
    public int getFeatureSize() {
        return featureCount;
    }

    @Override
    public String[] getFeatureNames() {
        return super.getFeatureNames();
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        super.importParameters(helper, document, dictionary);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        super.exportParameters(helper, document, dictionary);
    }
}
