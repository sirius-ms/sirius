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
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class CombinedFeatureCreator implements FeatureCreator {
    public ArrayList<FeatureCreator> featureCreators;
    protected int featureCount;
    protected double[] computed_features;


    public CombinedFeatureCreator(Collection<FeatureCreator> featureCreators){
        this(new ArrayList<>(featureCreators));
    }

    public CombinedFeatureCreator(FeatureCreator... featureCreators){
            this(Arrays.stream(featureCreators).collect(Collectors.toList()));
    }

    private CombinedFeatureCreator(ArrayList<FeatureCreator> featureCreators){
        this.featureCreators = featureCreators;
        this.featureCount = featureCreators.stream().mapToInt(FeatureCreator::getFeatureSize).sum();
    }




    @Override
    public int weight_direction() {
        return 0;
    }

    @Override
    public int min_quartil() {
        return 0;
    }

    @Override
    public int max_quartil() {
        return 0;
    }

    @Override
    public double[] computeFeatures(ParameterStore combinedParapeters) {
        computed_features= new double[getFeatureSize()];
        int pos = 0;
        for (FeatureCreator featureCreator : featureCreators) {
            final double[] currentScores = featureCreator.computeFeatures(combinedParapeters);
            for (double currentScore : currentScores) computed_features[pos++] = currentScore;
        }
        return computed_features;
    }

    @Override
    public int getFeatureSize() {
        return featureCount;
    }

    @Override
    public void setMinQuartil(int quartil) {

    }

    @Override
    public void setMaxQuartil(int quartil) {

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
            for (String currentName : currentNames) names[pos++] = currentName;
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.featureCreators = new ArrayList<>();
        fillList(this.featureCreators, helper, document, dictionary, "featureCreators");
        this.featureCount = featureCreators.stream().mapToInt(FeatureCreator::getFeatureSize).sum();
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
