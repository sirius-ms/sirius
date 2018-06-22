/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;

public class LossSizeScorer implements LossScorer, PeakPairScorer, MolecularFormulaScorer{

    public static final DensityFunction LEARNED_DISTRIBUTION = LogNormalDistribution.withMeanAndSd(4.057753844479435d, 0.6386804182255676d);
    public static final double LEARNED_NORMALIZATION = -5.860753214730718d;

    private DensityFunction distribution;
    private double normalization;

    private boolean adjustNormalizationBasedOnData;

    public LossSizeScorer() {
        this(LEARNED_DISTRIBUTION, LEARNED_NORMALIZATION);
    }

    public LossSizeScorer(DensityFunction distribution, double normalization) {
        this.distribution = distribution;
        this.normalization = normalization;
        this.adjustNormalizationBasedOnData = false;
    }

    public boolean isAdjustNormalizationBasedOnData() {
        return adjustNormalizationBasedOnData;
    }

    public void setAdjustNormalizationBasedOnData(boolean adjustNormalizationBasedOnData) {
        this.adjustNormalizationBasedOnData = adjustNormalizationBasedOnData;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setDistribution(DensityFunction distribution) {
        this.distribution = distribution;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public DensityFunction getDistribution() {
        return distribution;
    }

    private final double scoring(double mass) {
        return Math.log(Math.max(1e-12, distribution.getDensity(mass))) - normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        return scoring(formula.getMass());
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
        final TDoubleArrayList lossSizeScores = adjustNormalizationBasedOnData ? new TDoubleArrayList() : null;
        for (int fragment=0; fragment < peaks.size(); ++fragment) {
            final double fragmentMass = peaks.get(fragment).getMass();
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int parent=fragment+1; parent < peaks.size(); ++parent) {
                final double parentMass = peaks.get(parent).getMass();
                final double score = scoring(parentMass-fragmentMass);
                scores[parent][fragment] += score;
                if (adjustNormalizationBasedOnData) {
                    bestScore = Math.max(bestScore, score);
                }
            }
            if (adjustNormalizationBasedOnData && !Double.isInfinite(bestScore)) {
                lossSizeScores.add(bestScore);
            }
        }
        if (adjustNormalizationBasedOnData && lossSizeScores.size() > 0) {
            final double avgScore = lossSizeScores.sum()/((double)lossSizeScores.size());
            if (avgScore < 0 && !Double.isInfinite(avgScore)) {
                for (int fragment=0; fragment < peaks.size(); ++fragment) {
                    for (int parent=fragment+1; parent < peaks.size(); ++parent) {
                        scores[parent][fragment] += -avgScore;
                    }
                }
            }
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.distribution = (DensityFunction)helper.unwrap(document, document.getFromDictionary(dictionary, "distribution"));
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        if (document.hasKeyInDictionary(dictionary, "adjustNormalizationBasedOnData"))
            this.adjustNormalizationBasedOnData = document.getBooleanFromDictionary(dictionary, "adjustNormalizationBasedOnData");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "distribution", helper.wrap(document, distribution));
        document.addToDictionary(dictionary, "normalization", normalization);
        document.addToDictionary(dictionary, "adjustNormalizationBasedOnData", adjustNormalizationBasedOnData);
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getFormula());
    }
}
