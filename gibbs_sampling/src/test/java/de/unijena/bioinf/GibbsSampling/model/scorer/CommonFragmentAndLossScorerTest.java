package de.unijena.bioinf.GibbsSampling.model.scorer;/*
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

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.IonizedMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionFix;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CommonFragmentAndLossScorerTest {

    @Test
    public void testScoreCommonPeaks() throws IOException {
        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorer(0);

        double[][] expectedFragmentScores = new double[][]{
                new double[]{63.0, 9.0, 1.0},
                new double[]{9.0, 22.0, 0.0},
                new double[]{1.0, 0.0, 61.0}
        };

        double[][] expectedLossScores = new double[][]{
                new double[]{20.0, 1.0, 3.0},
                new double[]{1.0, 21.0, 0.0},
                new double[]{3.0, 0.0, 20.0}

        };

        testScoreCommonPeaks(c, expectedFragmentScores, expectedLossScores, true);
        testScoreCommonPeaks(c, expectedFragmentScores, expectedLossScores, false);

    }

    @Test
    public void testScoreFragmentCandidates() throws IOException {
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaksExpected = parseFragmentPeaksWithExplanationFromString();
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaksExpected = parseLossPeaksWithExplanationFromString();

        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
        FragmentsCandidate[][] candidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);

        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorer(0.0);
        EdgeFilter edgeFilter = new EdgeThresholdMinConnectionsFilter(0.0, 1, 1);

        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks = extractFragmentPeaks(candidates, c, true);
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaks = extractFragmentPeaks(candidates, c, false);

        prepareEdgeScorer(candidates, c, edgeFilter);

        double score = c.score(candidates[0][0], candidates[1][0]);

        assertEqualWithoutIndex(allFragmentPeaksExpected, allFragmentPeaks);
        assertEqualWithoutIndex(allLossPeaksExpected, allLossPeaks);

    }

    @Test
    public void testCreatePeaksWithExplanations() throws IOException {
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaksExpected = parseFragmentPeaksWithExplanationFromString();
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaksExpected = parseLossPeaksWithExplanationFromString();

        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
        FragmentsCandidate[][] candidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);

        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorer(0.0);
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks = extractFragmentPeaks(candidates, c, true);
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaks = extractFragmentPeaks(candidates, c, false);


        assertEqualWithoutIndex(allFragmentPeaksExpected, allFragmentPeaks);
        assertEqualWithoutIndex(allLossPeaksExpected, allLossPeaks);

    }

    protected void assertEqualWithoutIndex(CommonFragmentAndLossScorer.PeakWithExplanation[][] allPeaksExpected, CommonFragmentAndLossScorer.PeakWithExplanation[][] allPeaksObserved ) {
        for (int i = 0; i < allPeaksExpected.length; i++) {
            assertEqualWithoutIndex(allPeaksExpected[i], allPeaksObserved[i]);
        }
    }

    protected void assertEqualWithoutIndex(CommonFragmentAndLossScorer.PeakWithExplanation[] peaksExpected, CommonFragmentAndLossScorer.PeakWithExplanation[] peaksObserved) {
        assertEquals(peaksExpected.length, peaksObserved.length);
        for (int i = 0; i < peaksExpected.length; i++) {
            CommonFragmentAndLossScorer.PeakWithExplanation peakWithExplanation = peaksExpected[i];
            //todo only check MFs, because ionziation missing for validation data
            MolecularFormula[] formulasExpected = peaksExpected[i].formulas;
            MolecularFormula[] formulas = peaksObserved[i].formulas;
            assertArrayEquals(formulasExpected, formulas);
            assertEquals(peaksExpected[i].mass, peaksObserved[i].mass, 1e-13);
            assertEquals(peaksExpected[i].bestScore, peaksObserved[i].bestScore, 1e-13);

        }
    }

    protected void testScoreCommonPeaks(CommonFragmentAndLossScorer c, double[][] expectedFragmentScores, double[][] expectedLossScores, boolean startFromPeaks) throws IOException {
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks, allLossPeaks;
        if (startFromPeaks) {
            allFragmentPeaks = parseFragmentPeaksWithExplanationFromString();
            allLossPeaks = parseLossPeaksWithExplanationFromString();
        } else {
            //do all parsing from MS input
            Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
            FragmentsCandidate[][] candidates = ExamplePreparationUtils.extractCandidates(data);

            allFragmentPeaks = extractFragmentPeaks(candidates, c, true);
            allLossPeaks = extractFragmentPeaks(candidates, c, false);
        }

        testScoreCommonPeaks(c, expectedFragmentScores, expectedLossScores, allFragmentPeaks, allLossPeaks);
    }

    protected void testScoreCommonPeaks(CommonFragmentAndLossScorer c, double[][] expectedFragmentScores, double[][] expectedLossScores, CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks, CommonFragmentAndLossScorer.PeakWithExplanation[][] allLossPeaks) throws IOException {
//        writePeaksWithExplanation(allLossPeaks);

        double[][] fragmentScores = calculateScores(allFragmentPeaks, c);
        double[][] lossScores = calculateScores(allLossPeaks, c);

        for (int i = 0; i < fragmentScores.length; i++) {
            assertArrayEquals(expectedFragmentScores[i], fragmentScores[i], 1e-14);
        }

        for (int i = 0; i < lossScores.length; i++) {
            assertArrayEquals(expectedLossScores[i], lossScores[i], 1e-14);
        }
    }

    protected void writePeaksWithExplanation(CommonFragmentAndLossScorer.PeakWithExplanation[][] peaks){
        System.out.println("allPeaks = new String[][]{");
        for (int i = 0; i < peaks.length; i++) {
            CommonFragmentAndLossScorer.PeakWithExplanation[] compound = peaks[i];
            System.out.println("new String[]{");
            for (int j = 0; j < compound.length; j++) {
                CommonFragmentAndLossScorer.PeakWithExplanation peak = compound[j];
                String mfs = Arrays.stream(peak.formulas).map(im-> im.toString()).collect(Collectors.joining("|"));
                double mass = peak.mass;
                double score = peak.bestScore;
                System.out.println("\""+mfs+", "+mass+", "+score+"\",");
            }
            System.out.println("},");
        }
        System.out.println("};");
    }

    @Test
    public void assertNormalization() throws IOException {
        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorer(0);
        double[] norm = new double[]{40.0, 42.0, 40.0};

        assertNormalization(c, norm, "/tiny-example");
    }

    protected void assertNormalization(CommonFragmentAndLossScorer c, double[] expectedNorm, String resource) throws IOException {
        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData(resource, 15, true);
        FragmentsCandidate[][] candidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);

        assertArrayEquals(expectedNorm, c.normalization(candidates, 1.0), 1e-15);
    }

    @Test
    public void testPreparation() throws Exception {
        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorer(0);

        double minNumberMatchedPeaksLossesExpected = 2.0;
        double thresholdExpected = 0.0;
        double[] normalizationExpected = new double[]{39.0, 41.0, 39.0};
//        BitSet[] maybeSimilarExpected = new BitSet[]{BitSet.valueOf(new long[]{6}), new BitSet(), new BitSet()};//BitSet {1,2}
        BitSet[] maybeSimilarExpected = new BitSet[]{new BitSet(), BitSet.valueOf(new long[]{1}), BitSet.valueOf(new long[]{1})};//BitSet {0}
        assertAfterCalculatingWeights(c, minNumberMatchedPeaksLossesExpected, thresholdExpected, normalizationExpected, maybeSimilarExpected);
    }

    protected void assertAfterCalculatingWeights(CommonFragmentAndLossScorer c, double minNumberMatchedPeaksLossesExpected, double thresholdExpected, double[] normalizationExpected, BitSet[] maybeSimilarExpected) throws IOException {
        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.getData("/tiny-example", 15, true);
        System.out.println();

        EdgeFilter edgeFilter = new EdgeThresholdMinConnectionsFilter(0.5, 1, 1);
        ScoreProbabilityDistribution probabilityDistribution = new LogNormalDistribution(true);
        ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionFix(c, probabilityDistribution, 0.9);

        FragmentsCandidate[][] expectedCandidates = ExamplePreparationUtils.parseExpectedCandidatesFromString(data);


        prepareEdgeScorer(expectedCandidates, commonFragmentAndLossScorer, edgeFilter);


        List<Ms2Experiment> experimentsOrdered = new ArrayList<>(data.keySet());


        assertEquals(minNumberMatchedPeaksLossesExpected, c.MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES, 1e-15);
        assertEquals(thresholdExpected, c.threshold, 1e-15);

        for (int i = 0; i < experimentsOrdered.size(); i++) {
            final Ms2Experiment e = experimentsOrdered.get(i);
            assertEquals(c.idxMap.get(e), i);
        }

        assertEquals(normalizationExpected[0], c.normalizationMap.get(experimentsOrdered.get(0)), 1e-15);
        assertEquals(normalizationExpected[1], c.normalizationMap.get(experimentsOrdered.get(1)), 1e-15);
        assertEquals(normalizationExpected[2], c.normalizationMap.get(experimentsOrdered.get(2)), 1e-15);

        assertEquals(maybeSimilarExpected[0], c.maybeSimilar[0]);
        assertEquals(maybeSimilarExpected[1], c.maybeSimilar[1]);
        assertEquals(maybeSimilarExpected[2], c.maybeSimilar[2]);
    }










    protected double[][] calculateScores(CommonFragmentAndLossScorer.PeakWithExplanation[][] peaks, CommonFragmentAndLossScorer c){
        double[][] scores = new double[peaks.length][];
        for(int i = 0; i < peaks.length; ++i) {
            double[] s = new double[peaks.length];
            for(int j = 0; j < peaks.length; ++j) {
                final double common = c.scoreCommons(peaks[i], peaks[j]); //todo do not need indices for this step?
                s[j] = common;
            }
            scores[i] = s;
        }
        return scores;
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] extractFragmentPeaks(FragmentsCandidate[][] candidates, CommonFragmentAndLossScorer c, boolean useFragments) {
        CommonFragmentAndLossScorer.PeakWithExplanation[][] allFragmentPeaks = new CommonFragmentAndLossScorer.PeakWithExplanation[candidates.length][];

        for(int i = 0; i < candidates.length; ++i) {
            Ms2Experiment experiment = candidates[i][0].getExperiment();
            FragmentsCandidate[] currentCandidates = candidates[i];

            CommonFragmentAndLossScorer.PeakWithExplanation[] fragmentPeaks = c.getPeaksWithExplanations(currentCandidates, useFragments);
            allFragmentPeaks[i] = fragmentPeaks;
        }

        return allFragmentPeaks;
    }

    protected void prepareEdgeScorer(FragmentsCandidate[][] allCandidates, EdgeScorer<FragmentsCandidate> edgeScorer, EdgeFilter edgeFilter){
        edgeScorer.prepare(allCandidates);

        if (edgeScorer instanceof ScoreProbabilityDistributionFix){
            if (edgeFilter instanceof EdgeThresholdFilter){
                ((ScoreProbabilityDistributionFix)edgeScorer).setThresholdAndPrepare(allCandidates);
            } else {
                ((ScoreProbabilityDistributionFix)edgeScorer).prepare(allCandidates);
            }

        } else if (edgeScorer instanceof ScoreProbabilityDistributionEstimator){
            if (edgeFilter instanceof EdgeThresholdFilter) {
                ((ScoreProbabilityDistributionEstimator) edgeScorer).setThresholdAndPrepare(allCandidates);
            } else {
                ((ScoreProbabilityDistributionEstimator) edgeScorer).prepare(allCandidates);
            }
        } else {
            edgeScorer.prepare(allCandidates);
        }
    }

    protected PairwiseEdgeScorerCompoundSimilarities createSimpleCompoundSimilarityRepresentation(FragmentsCandidate[][] fragmentsCandidates, BitSet[] similarCompounds) {
        PairwiseEdgeScorerCompoundSimilarities compoundSimilarities = new PairwiseEdgeScorerCompoundSimilarities();
//        int idx = 0;
        for (int i = 0; i < fragmentsCandidates.length; i++) {
            FragmentsCandidate[] candidates = fragmentsCandidates[i];
            compoundSimilarities.compoundIds.add(candidates[0].getExperiment().getName());
            compoundSimilarities.maybeSimilarList.add(similarCompounds[i]);

        }
        return compoundSimilarities;
    }


    protected void writeEdgeScorerCompoundsSimilarities(PairwiseEdgeScorerCompoundSimilarities compoundSimilarities, String resource) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(resource))) {
            for (int i = 0; i < compoundSimilarities.compoundIds.size(); i++) {
                String expName = compoundSimilarities.compoundIds.get(i);
                String indexString = compoundSimilarities.maybeSimilarList.get(i).stream().mapToObj(Integer::toString).collect(Collectors.joining("\t"));

                writer.write(expName + "\t" + indexString);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected int[] extractIntegers(String[] cols, int start, int end) {
        String[] part = Arrays.copyOfRange(cols, start, end);
        return Arrays.stream(part).mapToInt(Integer::parseInt).toArray();
    }

    protected void checkEdgeScorerCompoundSimilarities(PairwiseEdgeScorerCompoundSimilarities expectedCompoundSimilarities, PairwiseEdgeScorerCompoundSimilarities actualCompoundSimilarities) {
        assertEquals(expectedCompoundSimilarities.compoundIds.size(), actualCompoundSimilarities.compoundIds.size());

        TObjectIntHashMap<String> expectedIndexMap = expectedCompoundSimilarities.createIndexMap();
        TObjectIntHashMap<String> actualIndexMap = actualCompoundSimilarities.createIndexMap();
        for (int i = 0; i < actualCompoundSimilarities.compoundIds.size(); i++) {
            assertAllEdgesContained(actualCompoundSimilarities.compoundIds.get(i), actualCompoundSimilarities.maybeSimilarList.get(i), actualCompoundSimilarities, expectedCompoundSimilarities, expectedIndexMap);
            assertAllEdgesContained(expectedCompoundSimilarities.compoundIds.get(i), expectedCompoundSimilarities.maybeSimilarList.get(i), expectedCompoundSimilarities, actualCompoundSimilarities, actualIndexMap);
        }
    }

    protected void assertAllEdgesContained(String experimentName, BitSet edges, PairwiseEdgeScorerCompoundSimilarities compoundSimilarities, PairwiseEdgeScorerCompoundSimilarities compoundSimilaritiesToCompare, TObjectIntHashMap<String> indexMapToCompare) {
        int toCompareIndex = indexMapToCompare.get(experimentName);
        int[] directionChanged = new int[]{0};
        edges.stream().forEach(idx -> {
            String experimentName2 = compoundSimilarities.compoundIds.get(idx);
            int toCompareIndex2 = indexMapToCompare.get(experimentName2);
            if (compoundSimilaritiesToCompare.maybeSimilarList.get(toCompareIndex).get(toCompareIndex2)) {
                //all good
            } else if (compoundSimilaritiesToCompare.maybeSimilarList.get(toCompareIndex2).get(toCompareIndex)) {
                ++directionChanged[0];
            } else {
                fail("compound similarity edge missing.");
            }
        });

        if (directionChanged[0] > 0) {
            System.err.println("direction of edges of this compound changed for " + directionChanged[0] + " of " + edges.cardinality() + " total");
        }
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] parseFragmentPeaksWithExplanationFromString() {
        String[][] allPeaks = new String[][]{
                new String[]{
                        "C14H26O, 210.198365452, 1.0",
                        "C17H22, 226.172150704, 1.0",
                        "C14H28O2, 228.208930136, 1.0",
                        "C15H26O2, 238.193280072, 1.0",
                        "C17H24O, 244.182715388, 1.0",
                        "C14H30O3, 246.21949481999997, 1.0",
                        "C19H20, 248.15650064, 1.0",
                        "C18H22O, 254.167065324, 1.0",
                        "C15H28O3, 256.20384475599997, 1.0",
                        "C17H26O2, 262.193280072, 1.0",
                        "C19H22O, 266.16706532399996, 1.0",
                        "C18H24O2, 272.177630008, 1.0",
                        "C15H30O4, 274.21440944, 1.0",
                        "C20H20O, 276.15141525999996, 1.0",
                        "C19H24O2, 284.177630008, 1.0",
                        "C16H32O4, 288.230059504, 1.0",
                        "C18H26O3, 290.18819469199997, 1.0",
                        "C20H22O2, 294.161979944, 1.0",
                        "C17H30O4, 298.21440944, 1.0",
                        "C19H28O3, 304.20384475599997, 1.0",
                        "C20H24O3, 312.17254462799997, 1.0",
                        "C20H26O3, 314.18819469199997, 1.0",
                        "C17H32O5, 316.22497412399997, 1.0",
                        "C21H26O3, 326.18819469199997, 1.0",
                        "C20H28O4, 332.198759376, 1.0",
                        "C17H34O6, 334.235538808, 1.0",
                        "C22H24O3, 336.17254462799997, 1.0",
                        "C18H36O6, 348.25118887199994, 1.0",
                        "C20H30O5, 350.20932406, 1.0",
                        "C22H26O4, 354.183109312, 1.0",
                        "C19H34O6, 358.235538808, 1.0",
                        "C21H32O5, 364.22497412399997, 1.0",
                        "C22H28O5, 372.193673996, 1.0",
                        "C22H30O5, 374.20932406, 1.0",
                        "C19H36O7, 376.246103492, 1.0",
                        "C22H34O5, 378.24062418799997, 1.0",
                        "C23H30O5, 386.20932406, 1.0",
                        "C22H32O6, 392.21988874399995, 1.0",
                        "C26H26N4, 394.215746832, 1.0",
                        "C24H28O5, 396.193673996, 1.0",
                        "C20H40O8, 408.27231824, 1.0",
                        "C24H30O6, 414.20423868, 1.0",
                        "C23H24N6O2|C27H28O4, 416.197416692, 1.0",
                        "C24H36O6, 420.25118887199994, 1.0",
                        "C29H32N2O, 424.25146364399995, 1.0",
                        "C28H28N4O, 436.22631151599995, 1.0",
                        "C21H40O9, 436.26723286, 1.0",
                        "C24H38O7, 438.261753556, 1.0",
                        "C25H34O7, 446.230453428, 1.0",
                        "C30H32N2O2, 452.246378264, 1.0",
                        "C28H30N4O2, 454.2368762, 1.0",
                        "C25H26N6O3|C29H30O5, 458.20798137599996, 1.0",
                        "C26H34O8, 474.22536804799995, 1.0",
                        "C25H28N6O4|C29H32O6, 476.21854606, 1.0",
                        "C26H40O8, 480.27231824, 1.0",
                        "C30H32N4O3, 496.24744088399996, 1.0",
                        "C26H42O9, 498.282882924, 1.0",
                        "C30H34N4O4, 514.2580055679999, 1.0",
                        "C27H30N6O5|C31H34O7, 518.229110744, 1.0",
                        "C27H32N6O6|C31H36O8, 536.239675428, 1.0",
                        "C28H46O11, 558.3040122919999, 1.0",
                        "C32H38N4O6, 574.279134936, 1.0",
                        "C29H36N6O8|C33H40O10, 596.260804796, 1.0",
                },
                new String[]{
                        "C13H16O, 188.120115132, 1.0",
                        "C14H18O, 202.135765196, 1.0",
                        "C14H16O2, 216.115029752, 1.0",
                        "C17H22, 226.172150704, 1.0",
                        "C15H18O2, 230.130679816, 1.0",
                        "C17H24O, 244.182715388, 1.0",
                        "C18H22O, 254.167065324, 1.0",
                        "C18H24O2, 272.177630008, 1.0",
                        "C20H20O, 276.15141525999996, 1.0",
                        "C18H26O3, 290.18819469199997, 1.0",
                        "C20H22O2, 294.161979944, 1.0",
                        "C22H28O2, 324.208930136, 1.0",
                        "C22H24O3, 336.17254462799997, 1.0",
                        "C22H30O3, 342.21949481999997, 1.0",
                        "C22H26O4, 354.183109312, 1.0",
                        "C22H32O4, 360.230059504, 1.0",
                        "C24H30O4, 382.21440944, 1.0",
                        "C28H34O4, 434.245709568, 1.0",
                        "C26H34O6|C27H30N4O2, 442.236207504, 1.0",
                        "C30H38O6|C31H34N4O2, 494.267507632, 1.0",
                        "C32H42O6|C33H38N4O2, 522.29880776, 1.0",
                        "C34H46O8|C35H42N4O4, 582.319937128, 1.0",
                },
                new String[]{
                        "C14H26O2, 226.193280072, 1.0",
                        "C15H24O2, 236.177630008, 1.0",
                        "C17H22O, 242.167065324, 1.0",
                        "C14H28O3, 244.20384475599997, 1.0",
                        "C18H20O, 252.15141526, 1.0",
                        "C15H26O3, 254.18819469199997, 1.0",
                        "C17H24O2, 260.177630008, 1.0",
                        "C19H20O, 264.15141525999996, 1.0",
                        "C18H22O2, 270.161979944, 1.0",
                        "C15H28O4, 272.198759376, 1.0",
                        "C20H18O, 274.13576519599997, 1.0",
                        "C19H22O2, 282.161979944, 1.0",
                        "C16H30O4, 286.21440944, 1.0",
                        "C18H24O3, 288.17254462799997, 1.0",
                        "C15H30O5, 290.20932406, 1.0",
                        "C20H20O2, 292.14632988, 1.0",
                        "C17H28O4, 296.198759376, 1.0",
                        "C19H26O3, 302.18819469199997, 1.0",
                        "C18H26O4, 306.183109312, 1.0",
                        "C20H22O3, 310.15689456399997, 1.0",
                        "C20H24O3, 312.17254462799997, 1.0",
                        "C17H30O5, 314.20932406, 1.0",
                        "C21H24O3, 324.17254462799997, 1.0",
                        "C20H24O4, 328.167459248, 1.0",
                        "C20H26O4, 330.183109312, 1.0",
                        "C17H32O6, 332.21988874399995, 1.0",
                        "C22H22O3, 334.15689456399997, 1.0",
                        "C18H34O6, 346.235538808, 1.0",
                        "C20H28O5, 348.193673996, 1.0",
                        "C22H24O4, 352.167459248, 1.0",
                        "C21H30O5, 362.20932406, 1.0",
                        "C22H26O5, 370.178023932, 1.0",
                        "C19H34O7, 374.230453428, 1.0",
                        "C23H28O5, 384.193673996, 1.0",
                        "C22H28O6, 388.18858861599995, 1.0",
                        "C22H30O6, 390.20423868, 1.0",
                        "C19H36O8, 392.241018112, 1.0",
                        "C22H32O7, 408.214803364, 1.0",
                        "C24H28O6, 412.18858861599995, 1.0",
                        "C22H38O7, 414.261753556, 1.0",
                        "C24H30O7, 430.1991533, 1.0",
                        "C25H34O6, 430.235538808, 1.0",
                        "C21H38O9, 434.251582796, 1.0",
                        "C24H34O8, 450.22536804799995, 1.0",
                        "C27H32O6, 452.21988874399995, 1.0",
                        "C21H40O10, 452.26214747999995, 1.0",
                        "C24H40O8, 456.27231824, 1.0",
                        "C24H36O9, 468.235932732, 1.0",
                        "C26H32O8, 472.209717984, 1.0",
                        "C27H36O7, 472.246103492, 1.0",
                        "C24H42O9, 474.282882924, 1.0",
                        "C26H34O9, 490.220282668, 1.0",
                        "C27H38O8, 490.25666817599995, 1.0",
                        "C29H34O7, 494.230453428, 1.0",
                        "C23H42O11, 494.272712164, 1.0",
                        "C26H38O10, 510.246497416, 1.0",
                        "C29H36O8, 512.241018112, 1.0",
                        "C28H36O10, 532.230847352, 1.0",
                        "C28H50O13, 594.32514166, 1.0",
                        "C31H46O12, 610.298926912, 1.0",
                        "C33H44O12, 632.283276848, 1.0",
                },
        };
        return parseAllCandidatesFromString(allPeaks);
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] parseLossPeaksWithExplanationFromString() {
        String[][] allPeaks = new String[][]{
                new String[]{
                        "C2H4O2, 60.021129368, 1.0",
                        "C2H6O3, 78.03169405199999, 1.0",
                        "C4H8O4, 120.042258736, 1.0",
                        "C2H6N2O4|C3H2N6|C7H6O2, 122.03454338933334, 1.0",
                        "C4H10O5, 138.05282341999998, 1.0",
                        "C3H6N2O5|C4H2N6O|C8H6O3, 150.0294580093333, 1.0",
                        "C6H12O6, 180.06338810399998, 1.0",
                        "C10H6N4|C5H6N6O2|C9H10O4, 182.05745947466667, 1.0",
                        "C10H8N4O|C5H8N6O3|C9H12O5, 200.06802415866665, 1.0",
                        "C10H10O5|C11H6N4O|C6H6N6O3, 210.05237409466665, 1.0",
                        "C11H12O5|C12H8N4O|C7H8N6O3, 224.06802415866665, 1.0",
                        "C11H14O6|C12H10N4O2|C7H10N6O4, 242.07858884266668, 1.0",
                        "C11H16O7|C12H12N4O3|C7H12N6O5, 260.0891535266666, 1.0",
                        "C12H14O7|C13H10N4O3|C8H10N6O5, 270.0735034626666, 1.0",
                        "C13H16O7|C14H12N4O3|C9H12N6O5, 284.0891535266666, 1.0",
                        "C13H18O8|C14H14N4O4|C9H14N6O6, 302.09971821066665, 1.0",
                        "C10H12N6O6|C14H16O8|C15H12N4O4, 312.08406814666665, 1.0",
                        "C13H20O9|C14H16N4O5|C9H16N6O7, 320.1102828946666, 1.0",
                        "C10H14N6O7|C14H18O9|C15H14N4O5, 330.0946328306666, 1.0",
                        "C10H16N6O8|C14H20O10|C15H16N4O6, 348.1051975146666, 1.0",
                },
                new String[]{
                        "C2H4O2, 60.021129368, 1.0",
                        "C4H8O2, 88.052429496, 1.0",
                        "C8H12O2, 140.083729624, 1.0",
                        "C6H12O4|C7H8N4, 148.07422756, 1.0",
                        "C10H16O4|C11H12N4, 200.105527688, 1.0",
                        "C12H14O4|C13H10N4, 222.089877624, 1.0",
                        "C12H20O4|C13H16N4, 228.136827816, 1.0",
                        "C12H16O5|C13H12N4O, 240.10044230799997, 1.0",
                        "C12H22O5|C13H18N4O, 246.14739249999997, 1.0",
                        "C12H18O6|C13H14N4O2, 258.111006992, 1.0",
                        "C14H24O6|C15H20N4O2, 288.157957184, 1.0",
                        "C16H20O5|C17H16N4O, 292.13174243599997, 1.0",
                        "C14H26O7|C15H22N4O3, 306.16852186799997, 1.0",
                        "C16H22O6|C17H18N4O2, 310.14230712, 1.0",
                        "C16H24O7|C17H20N4O3, 328.152871804, 1.0",
                        "C18H18N4O3, 338.13789043599996, 1.0",
                        "C19H28O6|C20H24N4O2, 352.189257312, 1.0",
                        "C18H20N4O4, 356.14845512, 1.0",
                        "C21H26N4O2, 366.205576072, 1.0",
                        "C20H28O7|C21H24N4O3, 380.18417193199997, 1.0",
                        "C22H26N4O3, 394.20049069199996, 1.0",
                },
                new String[]{
                        "C5H8O2, 100.052429496, 1.0",
                        "C4H8O4, 120.042258736, 1.0",
                        "C4H10O5, 138.05282341999998, 1.0",
                        "C7H10O3, 142.06299417999998, 1.0",
                        "C7H12O4, 160.073558864, 1.0",
                        "C6H12O6, 180.06338810399998, 1.0",
                        "C9H14O5, 202.08412354799998, 1.0",
                        "C9H16O6, 220.09468823199998, 1.0",
                        "C11H16O6, 244.09468823199998, 1.0",
                        "C10H16O7, 248.08960285199998, 1.0",
                        "C11H18O7, 262.105252916, 1.0",
                        "C11H20O8, 280.1158176, 1.0",
                        "C11H22O9, 298.126382284, 1.0",
                        "C13H20O8, 304.1158176, 1.0",
                        "C12H20O9, 308.11073222, 1.0",
                        "C13H22O9, 322.126382284, 1.0",
                        "C13H24O10, 340.13694696799996, 1.0",
                        "C14H22O10, 350.121296904, 1.0",
                        "C13H26O11, 358.147511652, 1.0",
                        "C14H24O11, 368.131861588, 1.0",
                },
        };
        return parseAllCandidatesFromString(allPeaks);
    }

    protected CommonFragmentAndLossScorer.PeakWithExplanation[][] parseAllCandidatesFromString(String[][] allPeaks) {
        CommonFragmentAndLossScorer.PeakWithExplanation[][] peakWithExplanations = new CommonFragmentAndLossScorer.PeakWithExplanation[allPeaks.length][];
        for (int i = 0; i < allPeaks.length; i++) {
            String[] compound = allPeaks[i];
            CommonFragmentAndLossScorer.PeakWithExplanation[] peaks = new CommonFragmentAndLossScorer.PeakWithExplanation[compound.length];
            for (int j = 0; j < compound.length; j++) {
                String[] peak = compound[j].split(", ");
                String[] mfs = peak[0].split("\\|");
                double mass = Double.valueOf(peak[1]);
                double score = Double.valueOf(peak[2]);
                //todo initial data was without ionzation, hence using dummy
                Ionization ionization = PrecursorIonType.unknownPositive().getIonization();
//                CommonFragmentAndLossScorer.PeakWithExplanation p = new CommonFragmentAndLossScorer.PeakWithExplanation(mfs, mass, score);
                short idx = (short) j; //todo use sorted index
                CommonFragmentAndLossScorer.PeakWithExplanation p = new CommonFragmentAndLossScorer.PeakWithExplanation(Arrays.stream(mfs).map(mf->MolecularFormula.parseOrThrow(mf)).toArray(MolecularFormula[]::new), mass, score);
                peaks[j] = p;
            }
            peakWithExplanations[i] = peaks;
        }
        return peakWithExplanations;
    }


    protected class PairwiseEdgeScorerCompoundSimilarities {
        final public List<String> compoundIds;
        final List<BitSet> maybeSimilarList;

        public PairwiseEdgeScorerCompoundSimilarities() {
            super();
            this.maybeSimilarList = new ArrayList<>();
            this.compoundIds = new ArrayList<>();
        }

        public TObjectIntHashMap<String> createIndexMap() {
            TObjectIntHashMap<String> map = new TObjectIntHashMap<>();
            for (int i = 0; i < compoundIds.size(); i++) {
                String name = compoundIds.get(i);
                map.put(name, i);
            }
            return map;
        }

    }
}
