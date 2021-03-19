package de.unijena.bioinf.GibbsSampling.model;/*
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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class FragmentCandidateTest {

    @Test
    public void testParsing() throws IOException {
        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
        FragmentsCandidate[][] candidates = ExamplePreparationUtils.extractCandidates(data);

        FragmentsCandidate[][] expectedCandidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);

        assertEquals(expectedCandidates, candidates);

    }

    void assertEquals(FragmentsCandidate[][] expectedCandidates, FragmentsCandidate[][] candidates, double allowedIntensityDifference) {
        Assert.assertEquals(expectedCandidates.length, candidates.length);
        for (int i = 0; i < expectedCandidates.length; i++) {
            assertEquals(expectedCandidates[i], candidates[i], allowedIntensityDifference);
        }
    }

    private void assertEquals(FragmentsCandidate[] expectedCandidates, FragmentsCandidate[] candidates, double allowedIntensityDifference) {
        Assert.assertEquals(expectedCandidates.length, candidates.length);
        for (int i = 0; i < expectedCandidates.length; i++) {
            assertEquals(expectedCandidates[i], candidates[i], allowedIntensityDifference);
        }
    }

    private void assertEquals(FragmentsCandidate expectedCandidate, FragmentsCandidate candidate, double allowedIntensityDifference) {
        Assert.assertEquals(expectedCandidate.getExperiment().getName(), candidate.getExperiment().getName());
        Assert.assertEquals(expectedCandidate.formula, candidate.formula);
        Assert.assertEquals(expectedCandidate.ionType, candidate.ionType);
        Assert.assertEquals(expectedCandidate.getScore(), candidate.getScore(), 1e-14);

        assertEquals(expectedCandidate.getCandidate(), candidate.getCandidate(), allowedIntensityDifference);
    }

    private void assertEquals(FragmentsAndLosses expectedFragmentsAndLosses, FragmentsAndLosses fragmentsAndLosses, double allowedIntensityDifference) {
        assertEquals(expectedFragmentsAndLosses.getFragments(), fragmentsAndLosses.getFragments(), allowedIntensityDifference);
        assertEquals(expectedFragmentsAndLosses.getLosses(), fragmentsAndLosses.getLosses(), allowedIntensityDifference);
    }

    private void assertEquals(FragmentWithIndex[] expectedFragments, FragmentWithIndex[] fragments, double allowedIntensityDifference) {
        Assert.assertEquals(expectedFragments.length, fragments.length);

        //todo changed! sorting!!!
        Arrays.sort(expectedFragments, new Comparator<FragmentWithIndex>() {
            @Override
            public int compare(FragmentWithIndex o1, FragmentWithIndex o2) {
                return o1.getFormula().compareTo(o2.getFormula());
            }

        });

        Arrays.sort(fragments, new Comparator<FragmentWithIndex>() {
            @Override
            public int compare(FragmentWithIndex o1, FragmentWithIndex o2) {
                return o1.getFormula().compareTo(o2.getFormula());
            }
        });

        for (int i = 0; i < expectedFragments.length; i++) {
//            Assert.assertEquals(expectedFragments[i].idx, fragments[i].idx); //not that important?
            Assert.assertEquals(expectedFragments[i].getFormula(), fragments[i].getFormula());
            Assert.assertEquals(expectedFragments[i].getIonization(), fragments[i].getIonization());
            Assert.assertEquals(expectedFragments[i].score, fragments[i].score, allowedIntensityDifference);

        }
    }

    private void assertEquals(FragmentsCandidate[][] expectedCandidates, FragmentsCandidate[][] candidates) {
        Assert.assertEquals(expectedCandidates.length, candidates.length);
        for (int i = 0; i < expectedCandidates.length; i++) {
            assertEquals(expectedCandidates[i], candidates[i]);
        }
    }

    private void assertEquals(FragmentsCandidate[] expectedCandidates, FragmentsCandidate[] candidates) {
        Assert.assertEquals(expectedCandidates.length, candidates.length);
        for (int i = 0; i < expectedCandidates.length; i++) {
            assertEquals(expectedCandidates[i], candidates[i]);
        }
    }

    private void assertEquals(FragmentsCandidate expectedCandidate, FragmentsCandidate candidate) {
        Assert.assertEquals(expectedCandidate.getExperiment().getName(), candidate.getExperiment().getName());
        Assert.assertEquals(expectedCandidate.formula, candidate.formula);
        Assert.assertEquals(expectedCandidate.ionType, candidate.ionType);
        Assert.assertEquals(expectedCandidate.score, candidate.score, 1e-14);

        assertEquals(expectedCandidate.getCandidate(), candidate.getCandidate());
    }

    private void assertEquals(FragmentsAndLosses expectedFragmentsAndLosses, FragmentsAndLosses fragmentsAndLosses) {
        assertEquals(expectedFragmentsAndLosses.getFragments(), fragmentsAndLosses.getFragments());
        assertEquals(expectedFragmentsAndLosses.getLosses(), fragmentsAndLosses.getLosses());
    }

    private void assertEquals(FragmentWithIndex[] expectedFragments, FragmentWithIndex[] fragments) {
        Assert.assertEquals(expectedFragments.length, fragments.length);

        //todo changed! sorting!!!
        Arrays.sort(expectedFragments, new Comparator<FragmentWithIndex>() {
            @Override
            public int compare(FragmentWithIndex o1, FragmentWithIndex o2) {
                return o1.getFormula().compareTo(o2.getFormula());
            }

        });

        Arrays.sort(fragments, new Comparator<FragmentWithIndex>() {
            @Override
            public int compare(FragmentWithIndex o1, FragmentWithIndex o2) {
                return o1.getFormula().compareTo(o2.getFormula());
            }
        });

        for (int i = 0; i < expectedFragments.length; i++) {
//            Assert.assertEquals(expectedFragments[i].idx, fragments[i].idx); //not that important?
            Assert.assertEquals(expectedFragments[i].getFormula(), fragments[i].getFormula());
            Assert.assertEquals(expectedFragments[i].getIonization(), fragments[i].getIonization());
            Assert.assertEquals(expectedFragments[i].score, fragments[i].score, 1e-12);

        }
    }





}
