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

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

@Called("Intensity")
public class PeakIsNoiseScorer2 implements PeakScorer {

    double lambda;
    double xmin = 0d;

    public PeakIsNoiseScorer2() {
        this(10);
    }

    public PeakIsNoiseScorer2(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {

        double sumIntensity = 0d;
        for (ProcessedPeak p : input.getMergedPeaks())
            sumIntensity += Math.max(0d,p.getRelativeIntensity()-xmin);

        for (int i=0; i < peaks.size(); ++i) {
            final double score = lambda * (Math.max(0, peaks.get(i).getRelativeIntensity()-xmin)/sumIntensity);
            scores[i] += score;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.lambda =document.getDoubleFromDictionary(dictionary, "lambda");
        this.xmin = document.getDoubleFromDictionary(dictionary, "min");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "lambda", lambda);
        document.addToDictionary(dictionary, "min", xmin);
    }
}
