
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

package de.unijena.bioinf.ftalign.analyse;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.treealign.scoring.Scoring;

/**
 * @author Kai Dührkop
 */
public class TreeSizeNormalizer implements Normalizer {

    private final double c;


    public TreeSizeNormalizer(double c) {
        this.c = c;
    }

    @Override
    public double normalize(FTree left, FTree right, Scoring<Fragment> scoring, float score) {
        final double f = Math.min(score(left, scoring), score(right, scoring));
        assert f > 0d;
        assert Math.pow(f, c) > 0d;
        return ((double) score / Math.pow(f, c));
    }

    public double score(FTree tree, final Scoring<Fragment> scoring) {
        return scoring.selfAlignScore(tree.getRoot());
    }
}
