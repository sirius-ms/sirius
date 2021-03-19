
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
import de.unijena.bioinf.ChemistryBase.ms.ft.Beautified;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Scoring;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

public class TreeSizeScorer implements PeakScorer{


    public final static class TreeSizeBonus implements DataAnnotation {
        public final double score;

        public TreeSizeBonus(double score) {
            this.score = score;
        }
    }

    public double fastReplace(final ProcessedInput processedInput, final TreeSizeBonus newBonus) {
        // fast replace of peak scores. Dirty hack. be careful what you are doing!
        final Scoring scoring = processedInput.getAnnotationOrThrow(Scoring.class);
        final TreeSizeBonus oldBonus = processedInput.getAnnotation(TreeSizeBonus.class, () -> defaultBonus);
        final double diff = newBonus.score - oldBonus.score;
        if (Math.abs(diff) > 1e-12) {
            final double[] xs = scoring.getPeakScores();
            for (int i=0, n = xs.length-1; i<n; ++i) {
                xs[i] += diff;
            }
        }
        processedInput.setAnnotation(TreeSizeBonus.class, newBonus);
        return diff;

    }

    private TreeSizeBonus defaultBonus;

    public TreeSizeScorer() {
    }

    public TreeSizeScorer(double treeSizeScore) {
        this.defaultBonus = new TreeSizeBonus(treeSizeScore);
    }

    public double getTreeSizeScore() {
        return defaultBonus.score;
    }

    @Deprecated
    public void setTreeSizeScore(double treeSizeScore) {
        this.defaultBonus = new TreeSizeBonus(treeSizeScore);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        Beautified beauty = input.getAnnotation(Beautified.class, Beautified::ugly);
        final double bonus = beauty.isBeautiful() ? beauty.getNodeBoost() : input.getAnnotation(TreeSizeBonus.class, () -> defaultBonus).score;
        final double penalty = beauty!=null ? beauty.getBeautificationPenalty()/peaks.size() : 0d;
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] += bonus - penalty;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        defaultBonus = new TreeSizeBonus(document.getDoubleFromDictionary(dictionary, "score"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", defaultBonus.score);
    }
}
