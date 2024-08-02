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
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.features.*;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by martin on 06.08.18.
 */
public class CombinedFeatureCreatorBIODISTANCE2TO5 extends CombinedFeatureCreator {

    //all confidence means the pubchem confidence score for this input. Has to be always computet
    // same is the tophit in pubchem the same as in the reduced db.
    public CombinedFeatureCreatorBIODISTANCE2TO5(Scored<FingerprintCandidate>[] scored_array_all, Scored<FingerprintCandidate>[] scored_array_filtered, FingerblastScoring<?> covscore, ProbabilityFingerprint canopusFptPred, ProbabilityFingerprint canopusFptTop, CombinatorialSubtree[] epiTrees, Map<Fragment, ArrayList<CombinatorialFragment>>[] map, FTree[] fTrees) {
        super(
                /*
                //new PlattFeatures(),
                //  new LogPvalueDistanceFeatures(scored_array,scored_array,1),
                // new LogPvalueDistanceFeatures(scored_array_covscore,scored_array_covscore,1),


                //these are pubchem features and cannot have a filtered list as input
                // new ScoreFeatures(covscore,scored_array,scored_array),
                //new ScoreFeatures(covscore,scored_array_covscore,scored_array_covscore),
                //new ScoreFeatures(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array,scored_array),
                //new ScoreFeatures(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array_covscore,scored_array_covscore),

                //  new LogDistanceFeatures(scored_array,scored_array,1),
                // new LogDistanceFeatures(scored_array_covscore,scored_array_covscore,1),

                // new LogPvalueFeatures(scored_array,scored_array),
                //  new LogPvalueFeatures(scored_array_covscore,scored_array_covscore),

                // new PvalueScoreDiffScorerFeatures(scored_array_covscore,scored_array_covscore,scored_array[0],covscore),

                //new ScoreFeaturesNonBio(covscore,scored_array_covscore,scored_array_covscore),
                OLD STUFF

                */


                new ScoreFeaturesNonBio<>(covscore,scored_array_all,scored_array_filtered),

                new FptLengthFeature(),
                new SiriusScoreFeatures(),
                new ExplIntFeatures(),
                new PredictionQualityFeatures(),
                new CandlistSizeFeatures(scored_array_all),

                //these are bio features and can have a filtered list as input
                new FptLengthFeatureHit(scored_array_filtered),

                new CandlistSizeFeatures(scored_array_filtered),


                //   new LogDistanceFeatures(scored_array,scored_array_filtered,1),
                new LogDistanceFeatures(scored_array_filtered,1),
                //  new DistanceFeatures(scored_array,scored_array_filtered,1),
                new DistanceFeatures(scored_array_filtered,1),


                //   new ScoreFeatures<>(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array,scored_array_filtered),
                //   new ScoreFeatures<>(ScoringMethodFactory.getCSIFingerIdScoringMethod(performance).getScoring(),scored_array_covscore,scored_array_covscore_filtered),
                //new ScoreFeatures<>(covscore, scored_array, scored_array_filtered),
                new ScoreFeatures<>(covscore,  scored_array_filtered),


                //  new LogPvalueKDEFeatures(scored_array, scored_array_filtered),
                new LogPvalueKDEFeatures(scored_array_all, scored_array_filtered),
                new SqrtPvalueKDEFeatures(scored_array_all,scored_array_filtered),
                new Sqrt3PvalueKDEFeatures(scored_array_all,scored_array_filtered),
                //  new PvalueScoreDiffScorerFeatures<>(scored_array_covscore, scored_array_covscore_filtered, scored_array[0], covscore),
                new TanimotoDistanceFeatures(scored_array_filtered, 1),
                new TanimotoToPredFeatures( scored_array_filtered),
                new CanopusDiffFeatures(canopusFptPred, canopusFptTop),

                new EpiPeakSetFeatures(epiTrees,map,fTrees),
                new EpiPeakSetIntFeatures(epiTrees,map,fTrees),
                new EpiExplIntFeatures(epiTrees[0]),
                new EpiFragmenterScoreFeatures(epiTrees[0]),
                new EpiRatioExplPeaksFeatures(epiTrees[0]),
                new EpiUnexplainedPeaksFeatures(epiTrees[0],map[0])

        );

        for (FeatureCreator creator : featureCreators){
            if (creator instanceof LogDistanceFeatures) creator.setMaxQuartil(50);
            if (creator instanceof  DistanceFeatures) creator.setMaxQuartil(50);

        }
    }
}
