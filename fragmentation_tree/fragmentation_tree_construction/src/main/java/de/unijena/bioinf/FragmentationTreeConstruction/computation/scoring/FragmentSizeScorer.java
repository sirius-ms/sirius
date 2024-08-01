
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

/**
 * A tree get a bonus score if he can explain very small fragments.
 * The idea is: If there is a common loss path to the small fragments, this is a very strong indicator
 * for a correct tree. If there is a strange loss path to small fragments, the tree will be penalized (through
 * the loss scorer).
 */
public class FragmentSizeScorer implements PeakScorer {

    private double maxSize;
    private double maxScore;

    public FragmentSizeScorer() {
        this(200, 2);
    }

    public FragmentSizeScorer(double maxSize, double maxScore) {
        this.maxSize = maxSize;
        this.maxScore = maxScore;
    }

    public double getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(double maxSize) {
        this.maxSize = maxSize;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] += maxScore - Math.min(1,peaks.get(i).getMass()/maxSize)*maxScore;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        maxSize = document.getDoubleFromDictionary(dictionary, "maxSize");
        maxScore = document.getDoubleFromDictionary(dictionary, "maxScore");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "maxSize", maxSize);
        document.addToDictionary(dictionary, "maxScore", maxScore);
    }
}
