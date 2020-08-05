
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

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;

import java.util.Iterator;
import java.util.List;

public class StandardScoring implements Scoring<Fragment>, SimpleEqualityScoring<Fragment> {

    public final static float DEFAULT_MATCHSCORE = 5;
    public final static float DEFAULT_SCOREFOREACHNONHYDROGEN = 1;
    public final static float DEFAULT_MISSMATCHPENALTY = -3;
    public final static float DEFAULT_PENALTYFOREACHNONHYDROGEN = 0;

    public final static float DEFAULT_LOSSMATCHSCORE = 5;
    public final static float DEFAULT_LOSSSCOREFOREACHNONHYDROGEN = 1;
    public final static float DEFAULT_LOSSMISSMATCHPENALTY = -2;
    public final static float DEFAULT_LOSSPENALTYFOREACHNONHYDROGEN = -0.5f;

    public final static float DEFAULT_JOINMATCHSCORE = 5;
    public final static float DEFAULT_JOINSCOREFOREACHNONHYDROGEN = 1;
    public final static float DEFAULT_JOINMISSMATCHPENALTY = -2;
    public final static float DEFAULT_JOINPENALTYFOREACHNONHYDROGEN = -0.5f;


    public final static float DEFAULT_PENALTYFOREACHJOIN = 0;//-0.5f;
    public final static float DEFAULT_SCOREFORJOIN = 0;//1f;
    public final static float DEFAULT_GAP_SCORE = 0;
    //=> Join(N)-Score = scoreForJoin + N * penaltyForEachJoin
    // Problem: Scores werden mehrfach bestraft =/ Naja, sie werden ja auch mehrfach gescort. Auch nicht so dolle

    public float matchScore, scoreForEachNonHydrogen, missmatchPenalty, penaltyForEachNonHydrogen,
            lossMatchScore, lossScoreForEachNonHydrogen, lossMissmatchPenalty, lossPenaltyForEachNonHydrogen,
            penaltyForEachJoin, gapScore,
            joinMatchScore, joinScoreForEachNonHydrogen, joinMissmatchPenalty, joinPenaltyForEachNonHydrogen;

    private boolean useFragment;

    public StandardScoring(boolean useFragment, boolean usePacking) {
        matchScore = DEFAULT_MATCHSCORE;
        scoreForEachNonHydrogen = DEFAULT_SCOREFOREACHNONHYDROGEN;
        missmatchPenalty = DEFAULT_MISSMATCHPENALTY;
        penaltyForEachNonHydrogen = DEFAULT_PENALTYFOREACHNONHYDROGEN;
        lossMatchScore = DEFAULT_LOSSMATCHSCORE;
        lossScoreForEachNonHydrogen = DEFAULT_LOSSSCOREFOREACHNONHYDROGEN;
        lossMissmatchPenalty = DEFAULT_LOSSMISSMATCHPENALTY;
        lossPenaltyForEachNonHydrogen = DEFAULT_LOSSPENALTYFOREACHNONHYDROGEN;
        penaltyForEachJoin = DEFAULT_PENALTYFOREACHJOIN;
        joinMatchScore = DEFAULT_JOINMATCHSCORE;
        joinScoreForEachNonHydrogen = DEFAULT_JOINSCOREFOREACHNONHYDROGEN;
        joinMissmatchPenalty = DEFAULT_JOINMISSMATCHPENALTY;
        joinPenaltyForEachNonHydrogen = DEFAULT_JOINPENALTYFOREACHNONHYDROGEN;

        gapScore = DEFAULT_GAP_SCORE;
        penaltyForEachJoin = DEFAULT_SCOREFORJOIN;
        this.useFragment = useFragment;
    }

    public StandardScoring(boolean useFragment) {
        this(useFragment, false);
    }

    private static MolecularFormula selfJoin(MolecularFormula l, int n) {
        MolecularFormula f = l;
        int i = 1;
        for (; i < n; i <<= 1) {
            f = f.add(f);
        }
        i >>= 1;
        for (; i < n; ++i) {
            f = f.add(l);
        }
        return f;
    }

    @Override
    public float match(Fragment left, Fragment right) {
        float score = scoreFormulas(left.getIncomingEdge().getFormula(), right.getIncomingEdge().getFormula(), true);
        if (useFragment) {
            score += scoreFormulas(left.getFormula(), right.getFormula(), false);
        }
        return score;
    }

    @Override
    public float deleteLeft(Fragment left) {
        return gapScore;
    }

    @Override
    public float deleteRight(Fragment right) {
        return gapScore;
    }

    @Override
    public boolean isScoringVertices() {
        return useFragment;
    }

    @Override
    public float joinLeft(Fragment left, Fragment join, Fragment right) {
        float score = scoreJoinFormulas(left.getIncomingEdge().getFormula().add(join.getIncomingEdge().getFormula()), right.getIncomingEdge().getFormula());
        if (useFragment) {
            score += scoreFormulas(join.getFormula(), right.getFormula(), false);
        }
        score += (penaltyForEachJoin);
        return score;
    }

    @Override
    public float joinRight(Fragment right, Fragment join, Fragment left) {
        return joinLeft(right, join, left);
    }

    /*
    public float scoreMultiPackedForms(Iterator<Fragment> left, Iterator<Fragment> right) {
        FTree ln = left.next();
        FTree rn = right.next();
        final PackedFormula formLeft = ln.getPackedNeutralLoss();
        final PackedFormula formRight = rn.getPackedNeutralLoss();
        long leftBits = formLeft.data;
        int leftSize = formLeft.numberOfNonHydrogens;
        long rightBits = formRight.data;
        int rightSize = formRight.numberOfNonHydrogens;
        while (left.hasNext()) {
            ln = left.next();
            final PackedFormula p = ln.getPackedNeutralLoss();
            leftBits += p.data;
            leftSize += p.numberOfNonHydrogens;
        }
        while (right.hasNext()) {
            rn = right.next();
            final PackedFormula p = rn.getPackedNeutralLoss();
            rightBits += p.data;
            rightSize += p.numberOfNonHydrogens;
        }
        float score = scorePackedFormulas(new PackedFormula(leftBits, leftSize), new PackedFormula(rightBits, rightSize));
        if (useFragment) {
            final MolecularFormula fragmentLeft = ln.getFragment();
            final MolecularFormula fragmentRight = rn.getFragment();
            score += scoreFormulas(fragmentLeft, fragmentRight, false);
        }
        return score;
    }
    */

    public float scoreMultiForms(Iterator<Fragment> left, Iterator<Fragment> right) {
        Fragment ln = left.next();
        Fragment rn = right.next();
        MolecularFormula formLeft = ln.getIncomingEdge().getFormula();
        MolecularFormula formRight = rn.getIncomingEdge().getFormula();
        final MolecularFormula fragmentLeft = ln.getFormula();
        final MolecularFormula fragmentRight = rn.getFormula();
        while (left.hasNext()) {
            ln = left.next();
            formLeft = formLeft.add(ln.getIncomingEdge().getFormula());
        }
        while (right.hasNext()) {
            rn = right.next();
            formRight = formRight.add(rn.getIncomingEdge().getFormula());
        }
        float score = scoreJoinFormulas(formLeft, formRight);
        if (useFragment) {
            score += scoreFormulas(fragmentLeft, fragmentRight, false);
        }
        return score;
    }

    @Override
    public float join(Iterator<Fragment> leftNodes, Iterator<Fragment> rightNodes, int left, int right) {
        final float score = scoreMultiForms(leftNodes, rightNodes);
        return score + penaltyForEachJoin * Math.max(left, right);
    }

    public float scoreFormulas(MolecularFormula left, MolecularFormula right, boolean isLoss) {
        final int length = Math.max(left.atomCount(), right.atomCount());
        final int diffs = left.numberOfDifferenceHeteroAtoms(right);
        if (diffs > 0 || left.numberOfHydrogens() != right.numberOfHydrogens()) {
            return (isLoss ? lossMissmatchPenalty + lossPenaltyForEachNonHydrogen * diffs
                    : missmatchPenalty + penaltyForEachNonHydrogen * diffs);
        } else {
            final int nonHydrogens = left.atomCount() - left.numberOfHydrogens();
            return (isLoss ? lossMatchScore + nonHydrogens * lossScoreForEachNonHydrogen
                    : matchScore + nonHydrogens * scoreForEachNonHydrogen);
        }
    }

    public float scoreJoinFormulas(MolecularFormula left, MolecularFormula right) {
        final int length = Math.max(left.atomCount(), right.atomCount());
        final int diffs = left.numberOfDifferenceHeteroAtoms(right);
        if (diffs > 0 || left.numberOfHydrogens() != right.numberOfHydrogens()) {
            return joinMissmatchPenalty + joinPenaltyForEachNonHydrogen * diffs;
        } else {
            final int nonHydrogens = left.atomCount() - left.numberOfHydrogens();
            return joinMatchScore + nonHydrogens * joinScoreForEachNonHydrogen;
        }
    }

    @Override
    public float scoreVertices(Fragment left, Fragment right) {
        // don't penalize root missmatch!
        return Math.max(0, scoreFormulas(left.getFormula(), right.getFormula(), false));
    }

    @Override
    public float selfAlignScore(Fragment root) {
        return new PostOrderTraversal<Fragment>(root, FTree.treeAdapterStatic()).<Float>call(new PostOrderTraversal.Call<Fragment, Float>() {
            @Override
            public Float call(Fragment vertex, List<Float> values, boolean isRoot) {
                float sum = (isRoot ? scoreVertices(vertex, vertex) : match(vertex, vertex));
                for (float f : values) sum += f;
                return sum;
            }
        });
    }

    @Override
    public boolean isMatching(Fragment left, Fragment right) {
        return left.getIncomingEdge().getFormula().equals(right.getIncomingEdge().getFormula());
    }
}
