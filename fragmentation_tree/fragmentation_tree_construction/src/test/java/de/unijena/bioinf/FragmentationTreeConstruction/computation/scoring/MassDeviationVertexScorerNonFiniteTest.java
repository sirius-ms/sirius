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

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vertex score ends up in a loss weight (FragmentationPatternAnalysis.performGraphScoring), and
 * a non-finite loss weight breaks the critical path heuristic's memoization - see
 * CriticalPathInsertionHeuristicMemoizationTest. The clamp here has to catch NaN as well as
 * -Infinity; {@code score < -100} alone does not, because every comparison with NaN is false.
 */
public class MassDeviationVertexScorerNonFiniteTest {

    private static final MolecularFormula GLUCOSE = MolecularFormula.parseOrThrow("C6H12O6");
    private static final Ionization PROTONATED = new Charge(1);

    /** 10 ppm standard deviation, the usual order of magnitude for high-resolution MS2. */
    private static final Deviation DEV = new Deviation(10, 10 * 1e-6 * 100);

    private static double theoreticalMass() {
        return PROTONATED.addToMass(GLUCOSE.getMass());
    }

    @Test
    public void aPeakOnTheTheoreticalMassScoresNearZero() {
        final MassDeviationVertexScorer scorer = new MassDeviationVertexScorer();
        final double score = scorer.score(GLUCOSE, PROTONATED, theoreticalMass(), DEV);
        assertTrue(Double.isFinite(score) && score <= 0d && score > -1d,
                "a perfectly matching mass should score close to 0, was " + score);
    }

    /**
     * A deviation far outside the tolerance underflows erfc to 0, so log(erfc) is -Infinity. That is
     * the clamped case and it already worked.
     */
    @Test
    public void aHugeMassDeviationIsClampedInsteadOfMinusInfinity() {
        final MassDeviationVertexScorer scorer = new MassDeviationVertexScorer();
        final double score = scorer.score(GLUCOSE, PROTONATED, theoreticalMass() + 10d, DEV);
        assertEquals(-100d, score, 0d, "a hopeless mass deviation has to be clamped");
    }

    /**
     * A disabled scorer (weight 0) times the -Infinity of an underflowing erfc is NaN, and NaN is not
     * {@code < -100}. This is the value that used to reach the graph.
     */
    @Test
    public void aZeroWeightedHugeDeviationDoesNotProduceNaN() {
        final MassDeviationVertexScorer scorer = new MassDeviationVertexScorer();
        scorer.setWeight(0d);
        final double score = scorer.score(GLUCOSE, PROTONATED, theoreticalMass() + 10d, DEV);
        assertTrue(Double.isFinite(score), "score must never be NaN, was " + score);
    }

    /** A zero mass tolerance makes the erfc argument 0/0 = NaN for an exactly matching peak. */
    @Test
    public void aZeroToleranceDoesNotProduceNaN() {
        final MassDeviationVertexScorer scorer = new MassDeviationVertexScorer();
        final double score = scorer.score(GLUCOSE, PROTONATED, theoreticalMass(), new Deviation(0, 0));
        assertTrue(Double.isFinite(score), "score must never be NaN, was " + score);
    }
}
