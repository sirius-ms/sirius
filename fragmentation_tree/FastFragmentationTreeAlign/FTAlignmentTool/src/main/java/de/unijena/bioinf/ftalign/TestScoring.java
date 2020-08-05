
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
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class TestScoring implements Scoring<Fragment>, SimpleEqualityScoring<Fragment> {

    final String[] SUBSTITUENTS_LEFT = new String[]{
            "C2H2O", "C2H2O", "C2H2O", "C2H2O", "C2H4O", "CH2O2", "CH2O2", "CH2O2", "C4H4",
            "C4H4", "C4H4", "CH3", "CH3", "CH3", "C4H6", "C4H6",
            "CHN", "C4H8", "C4H8", "C4H8", "C4H8", "C4H8", "CH2", "C2H4",
            "C2H4", "CH4", "CH4", "CO", "CO", "CO", "CH2O", "H3N", "H3N", "C3H6", "C3H6", "C3H6"
    };

    final String[] SUBSTITUENTS_RIGHT = new String[]{
            "H2O", "C2H4", "CO", "C2H2", "H2O", "C2H4", "CO", "C2H2", "H2O", "C2H4", "C2H2", "H2O", "C2H4", "C2H2",
            "H2O", "C2H4", "C2H2", "H2O", "C2H4", "C5H10", "C3H6", "C2H2", "CO", "H2O", "C2H2", "H2O", "C2H4",
            "H2O", "C2H4", "C2H2", "CO", "H2O", "C2H2", "H2O", "C2H4", "CO"
    };

    final float[] SCORES = new float[]{
            4, 2, 2, 3, 2, 3, 1, 2, 1, 2, 2, 1, 3, 2, 2, 2, 2, 1, 2, 4, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 1
    };

    final String[] FRAGMENT_DIFFS = new String[]{
            "O", "C2O", "C2O2", "O2", "O3", "C2H4O2", "CO", "C3H2O", "C3O", "C3H10", "C6H10", "CH6O",
            "CH4O", "C2H8", "H2", "CH4O2", "C4H6O", "C5H8", "C6H12", "C4H10", "C3H12", "C4H4O", "C4H12",
            "C2H10O", "C5H4O", "C", "CH2", "C4H2O", "C4H10O", "CH2O", "C5H10", "C2H6O", "C2H2", "C2H8O",
            "C2H4O", "C2H2O", "C2H2O2", "C2", "H4O2", "H4O", "H6O2", "CH6O2", "C4H8O", "H2O2", "C3H10O",
            "C5H8O", "C5H12", "C3H2O2", "H2O", "H2O3", "C3H4O", "C5H6O"
    };

    final String[][] LOSS_FREQUENCIES = new String[][]{
            {"H2O", "C2H2", "C2H4", "CHN", "CO"},
            {"CH3", "H3N", "CH3N", "C2H2O", "C4H4", "C3H2O", "C4H6", "CH4", "C3H4O", "HF", "CH2", "C3H6", "C5H10",
                    "CH2O2", "CH2O", "C2O2", "C4H2", "C4H8", "C2H4O", "Cl", "HCl", "CO2"},
            {"C3H9N", "HO3P", "H3O4P", "CS", "C2H6", "HS", "H2S", "C7H8", "C3H4", "C6H7N", "OS", "O2S", "C2H5NO",
                    "CH2S", "C3H6O", "C2H7N", "C5H4", "C4H4O", "C8H16", "C5H8", "C2H2O2", "H2", "C6H2", "C4H7N",
                    "C2H5N", "C5O", "C5H4O", "C4H9N", "C3O", "CH3O", "Br", "C2H2N2", "HBr", "NO2", "C3H7N", "C4H11N",
                    "C7H10", "C7H6", "C6H10", "C9H18", "CHO2", "C6H6", "CHNO", "C2H3NO", "CH4O2", "C2H4O2", "H4O2",
                    "C2H6O", "C4H4O2", "C6H12", "C3O2", "C2H3O", "C6H4", "CH2N2", "C5H6", "H3NO", "C6H10O", "C7H4",
                    "C3H2O2", "C2HN", "S", "CH3NO", "C4H2O", "CH4O", "C2H3N", "C6H6O", "C4H", "C3H5", "C3H3N",
                    "C3H3NO", "C3H8", "I", "HI", "C4H10", "C5H8O", "C2H4N", "C4H6O", "CH5N", "C3HN", "C5H", "CH4S",
                    "C7H6O", "C3H", "C6H10O5", "C7H14", "H", "C8H8", "C2HNO", "O2"},
    };

    private HashMap<MolecularFormula, TObjectFloatHashMap<MolecularFormula>> substMap;
    private TObjectFloatHashMap<MolecularFormula> fragmentSub;
    private TObjectDoubleHashMap<MolecularFormula> lossFreqs;

    public TestScoring() {
        substMap = new HashMap<MolecularFormula, TObjectFloatHashMap<MolecularFormula>>();
        for (int i = 0; i < SUBSTITUENTS_LEFT.length; ++i) {
            final MolecularFormula left = MolecularFormula.parseOrThrow(SUBSTITUENTS_LEFT[i]);
            final MolecularFormula right = MolecularFormula.parseOrThrow(SUBSTITUENTS_RIGHT[i]);
            final float score = SCORES[i];
            if (!substMap.containsKey(left)) substMap.put(left, new TObjectFloatHashMap<MolecularFormula>());
            if (!substMap.containsKey(right)) substMap.put(right, new TObjectFloatHashMap<MolecularFormula>());
            substMap.get(left).put(right, score);
            substMap.get(right).put(left, score);
        }
        fragmentSub = new TObjectFloatHashMap<MolecularFormula>();
        for (String s : FRAGMENT_DIFFS) fragmentSub.put(MolecularFormula.parseOrThrow(s), 4f);
        lossFreqs = new TObjectDoubleHashMap<MolecularFormula>();
        for (int i = 0; i < LOSS_FREQUENCIES.length; ++i) {
            final double score = 4 + 2 * i;
            for (String s : LOSS_FREQUENCIES[i]) {
                lossFreqs.put(MolecularFormula.parseOrThrow(s), score);
            }
        }
    }

    @Override
    public boolean isScoringVertices() {
        return true;
    }

    public float scoreGeneralJoin(Fragment left, Fragment right, MolecularFormula leftLosses, MolecularFormula rightLosses, int leftX, int rightX) {
        float score = generalScore(left, right, leftLosses, rightLosses);
        if (!leftLosses.equals(rightLosses)) score -= 100;
        return score - ((leftX + 1) * (rightX + 1)) * 0.25f;
    }

    public float generalScore(Fragment left, Fragment right, MolecularFormula leftLoss, MolecularFormula rightLoss) {
        // score fragments
        float score = scoreFragments(left, right);
        // score losses
        if (leftLoss.equals(rightLoss)) {
            score += lossFreqs.get(leftLoss);//leftLoss.atomCount() - leftLoss.numberOfHydrogens();
        } else {
            final TObjectFloatHashMap<MolecularFormula> mp = substMap.get(leftLoss);
            if (mp != null) {
                final float v = mp.get(rightLoss);
                if (v > 0) return v;
            }
            score -= 1f;
            score -= leftLoss.numberOfDifferenceHeteroAtoms(rightLoss) * 0.5f;
        }
        return score;
    }

    private float scoreFragments(Fragment left, Fragment right) {
        float score = 0f;
        if (left.getFormula().equals(right.getFormula())) {
            score += 5;
            score += (left.getFormula().atomCount() - left.getFormula().numberOfHydrogens());
        } else {
            final MolecularFormula f;
            if (left.getFormula().getMass() > right.getFormula().getMass())
                f = left.getFormula().subtract(right.getFormula());
            else f = right.getFormula().subtract(left.getFormula());
            final float sc = fragmentSub.get(f);
            if (sc > 0) return sc;
            score = -2f;
        }
        return score;
    }

    @Override
    public float joinLeft(Fragment left, Fragment join, Fragment right) {
        return scoreGeneralJoin(left, right, left.getIncomingEdge().getFormula().add(join.getIncomingEdge().getFormula()), right.getIncomingEdge().getFormula(), 1, 0);
    }

    @Override
    public float match(Fragment left, Fragment right) {
        return generalScore(left, right, left.getIncomingEdge().getFormula(), right.getIncomingEdge().getFormula());
    }

    @Override
    public float joinRight(Fragment right, Fragment join, Fragment left) {
        return scoreGeneralJoin(left, right, left.getIncomingEdge().getFormula(), right.getIncomingEdge().getFormula().add(join.getIncomingEdge().getFormula()), 0, 1);
    }

    @Override
    public float deleteLeft(Fragment left) {
        return -1;
    }

    @Override
    public float deleteRight(Fragment right) {
        return -1;
    }

    @Override
    public float join(Iterator<Fragment> leftNodes, Iterator<Fragment> rightNodes, int leftSize, int rightSize) {
        final Fragment left = leftNodes.next();
        final Fragment right = rightNodes.next();
        MolecularFormula l = left.getIncomingEdge().getFormula();
        MolecularFormula r = right.getIncomingEdge().getFormula();
        while (leftNodes.hasNext()) l = l.add(leftNodes.next().getIncomingEdge().getFormula());
        while (rightNodes.hasNext()) r = r.add(rightNodes.next().getIncomingEdge().getFormula());
        return scoreGeneralJoin(left, right, l, r, leftSize, rightSize);

    }

    @Override
    public float scoreVertices(Fragment left, Fragment right) {
        return scoreFragments(left, right);
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
        return left.getFormula().equals(right.getFormula());
    }
}
