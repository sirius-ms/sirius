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
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.features.*;
import de.unijena.bioinf.fingerid.blast.CovarianceScoringMethod;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by martin on 22.08.18.
 */
public class CombinedFeatureCreatorALL extends CombinedFeatureCreator{


    public FeatureCreator[] featureCreators;
    private int featureCount;
    private double[] computed_features;

    //Scorer for pubchem list (unfiltered)
    public CombinedFeatureCreatorALL(Scored<FingerprintCandidate>[] scored_array, Scored<FingerprintCandidate>[] scored_array_covscore, PredictionPerformance[] performance, CovarianceScoringMethod.Scoring covscore){

        long all =0;
        ArrayList<FeatureCreator> creators = new ArrayList<>(Arrays.asList(//new PlattFeatures(),
              //  new LogPvalueDistanceFeatures(scored_array,scored_array,1),
               // new LogPvalueDistanceFeatures(scored_array_covscore,scored_array_covscore,1),


                //these are pubchem features and cannot have a filtered list as input
                new ScoreFeatures(covscore,scored_array,scored_array),
                new ScoreFeatures(covscore,scored_array_covscore,scored_array_covscore),
                new ScoreFeatures(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array,scored_array),
                new ScoreFeatures(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array_covscore,scored_array_covscore),

                new LogDistanceFeatures(scored_array,scored_array,1),
                new LogDistanceFeatures(scored_array_covscore,scored_array_covscore,1),
                new DistanceFeatures(scored_array,scored_array,1),
                new DistanceFeatures(scored_array_covscore,scored_array_covscore,1),

                //new LogPvalueFeatures(scored_array,scored_array),
               // new LogPvalueFeatures(scored_array_covscore,scored_array_covscore),

                new LogPvalueKDEFeatures(scored_array,scored_array),
                new LogPvalueKDEFeatures(scored_array_covscore,scored_array_covscore),

                new PvalueScoreDiffScorerFeatures(scored_array_covscore,scored_array_covscore,scored_array[0],covscore),
                new FptLengthFeature(),
                new FptLengthFeatureHit(scored_array),
                new SiriusScoreFeatures(),
                new ExplIntFeatures(),
                new PredictionQualityFeatures(),
                //new MassFeatures(),
                new CandlistSizeFeatures(scored_array_covscore),

        new TanimotoDistanceFeatures(scored_array,scored_array,1),
                new TanimotoToPredFeatures(scored_array,scored_array)
        ));

        featureCount=0;
        featureCreators = new FeatureCreator[creators.size()];

        for(int i=0;i< creators.size();i++){
            featureCreators[i]=creators.get(i);
            featureCount+=creators.get(i).getFeatureSize();
        }
    }


    @Override
    public void prepare(PredictionPerformance[] statistics) {

        for (FeatureCreator featureCreator : featureCreators) {
            featureCreator.prepare(statistics);
        }
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query,  IdentificationResult idresult) {
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
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        for (FeatureCreator featureCreator : featureCreators) {
            if (!featureCreator.isCompatible(query, rankedCandidates)) return false;
        }
        return true;
    }

    @Override
    public int getRequiredCandidateSize() {
        int max = -1;
        for (FeatureCreator featureCreator : featureCreators) max = Math.max(max, featureCreator.getRequiredCandidateSize());
        return max;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final String[] currentNames = featureCreator.getFeatureNames();
            for (int i = 0; i < currentNames.length; i++) names[pos++] = currentNames[i];
        }
        return names;
    }


}
