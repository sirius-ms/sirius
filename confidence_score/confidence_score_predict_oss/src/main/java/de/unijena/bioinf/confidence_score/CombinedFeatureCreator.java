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
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class CombinedFeatureCreator implements FeatureCreator{
    public FeatureCreator[] featureCreators;
    private int featureCount;
    private double[] computed_features;

    public CombinedFeatureCreator(){}

    public CombinedFeatureCreator(FeatureCreator... featureCreators){
        this.featureCreators = featureCreators;
        int count = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            count += featureCreator.getFeatureSize();
        }
        this.featureCount = count;
    }


    @Override
    public void prepare(PredictionPerformance[] statistics) {
        for (FeatureCreator featureCreator : featureCreators) {
            featureCreator.prepare(statistics);
        }
    }

    @Override
    public int weight_direction() {
        return 0;
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

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        List<FeatureCreator> featureCreatorList = new ArrayList<>();
        fillList(featureCreatorList, helper, document, dictionary, "featureCreators");
        this.featureCreators = featureCreatorList.toArray(new FeatureCreator[0]);
        int count = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            count += featureCreator.getFeatureSize();
        }
        this.featureCount = count;

    }

    private <T, G, D, L> void fillList(List<T> list, ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, String keyName) {
        if (!document.hasKeyInDictionary(dictionary, keyName)) return;
        Iterator<G> ls = document.iteratorOfList(document.getListFromDictionary(dictionary, keyName));
        while (ls.hasNext()) {
            final G l = ls.next();
            list.add((T) helper.unwrap(document, l));
        }
    }
    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (FeatureCreator featureCreator : featureCreators) document.addToList(list, helper.wrap(document, featureCreator));
        document.addListToDictionary(dictionary, "featureCreators", list);
    }


}
