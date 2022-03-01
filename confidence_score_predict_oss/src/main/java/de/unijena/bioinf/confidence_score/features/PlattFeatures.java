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
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class PlattFeatures implements FeatureCreator {
    int min_quartil=1;
    int max_quartil=99;
    private double[] quantiles = new double[]{0.50, 0.75, 0.90};
    //private double[] quantilesAbs = new double[]{0.5, 0.10, 0.25, 0.45};
    private int featureSize;

//    private int[] unbiasedPositions;

    public PlattFeatures() {
        this.featureSize= quantiles.length+1;
        //this.featureSize = quantiles.length+quantilesAbs.length+1;
    }

    @Override
    public int weight_direction() {
        return 0;
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
    public double[] computeFeatures(ParameterStore queryPara) {
        final ProbabilityFingerprint query = queryPara.getFP().orElseThrow();
        final double[] scores = new double[featureSize];
        final double[] platt = query.toProbabilityArray();
        Arrays.sort(platt);

        for (int i = 0; i < quantiles.length; i++) {
            final double quantile = quantiles[i];
            scores[i] = quantile(platt, quantile);
        }

        //deviation from 0.5
        for (int i = 0; i < platt.length; i++) platt[i] = Math.abs(platt[i]-0.5);
        Arrays.sort(platt);

      /*  for (int i = 0; i < quantilesAbs.length; i++) {
            final double quantile = quantilesAbs[i];
            scores[quantiles.length+i] = quantile(platt, quantile);
        }*/

        //deviation from middle as platt scores where transformed
        scores[scores.length-1] = getStdDev(platt);

        return scores;
    }

    @Override
    public int getFeatureSize() {
        return featureSize;
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
        return true;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames() {
        final String[] names = new String[featureSize];
        for (int i = 0; i < quantiles.length; i++) {
            names[i] = "quantile"+quantiles[i];
        }

      /*  for (int i = 0; i < quantilesAbs.length; i++) {
            names[quantiles.length+i] = "quantileAbs"+quantilesAbs[i];
        }*/

        names[names.length-1] = "devFromMiddle";

        return names;
    }

    //ToDo test
    private double quantile(double[] sortedNumbers, double quantile){
        int n = (int)Math.round((quantile * sortedNumbers.length + 0.5));
        return sortedNumbers[n];
    }

    private double getStdDev(double[] scores){
        double sum = 0.0;
        for (int i = 0; i < scores.length; i++) {
            sum += Math.pow(scores[i], 2);
        }
        return Math.sqrt(sum/(scores.length));
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L quantilesList = document.getListFromDictionary(dictionary, "quantiles");
        int size = document.sizeOfList(quantilesList);
        double[] quantiles = new double[size];
        for (int i = 0; i < size; i++) quantiles[i] = document.getDoubleFromList(quantilesList, i);
        this.quantiles = quantiles;
        L quantilesAbsList = document.getListFromDictionary(dictionary, "quantilesAbs");
        size = document.sizeOfList(quantilesAbsList);
        double[] quantilesAbs = new double[size];
        for (int i = 0; i < size; i++) quantilesAbs[i] = document.getDoubleFromList(quantilesAbsList, i);
       //this.quantilesAbs = quantilesAbs;
        this.featureSize = quantiles.length+quantilesAbs.length+1;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (double d : quantiles) document.addToList(list, d);
        document.addListToDictionary(dictionary, "quantiles", list);
        list = document.newList();
       // for (double d : quantilesAbs) document.addToList(list, d);
        document.addListToDictionary(dictionary, "quantilesAbs", list);
    }
}


