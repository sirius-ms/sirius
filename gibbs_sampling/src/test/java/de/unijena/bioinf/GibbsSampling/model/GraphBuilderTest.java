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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.model.distributions.LogNormalDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionFix;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorerNoiseIntensityWeighted;
import de.unijena.bioinf.jjobs.JJob;
import gnu.trove.map.hash.TIntIntHashMap;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class GraphBuilderTest {

    @Test
    public void testGraphConstructionExample() throws Exception {
        final Path exampleDir = Paths.get(getClass().getResource("/tiny-example").getFile());

        LoggerFactory.getLogger(GraphBuilderTest.class).warn("test");
        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.readData(exampleDir);
        System.out.println();

        EdgeFilter edgeFilter = new EdgeThresholdMinConnectionsFilter(0.5, 1, 1);

        ScoreProbabilityDistribution probabilityDistribution = new LogNormalDistribution(true);
        CommonFragmentAndLossScorer c = new CommonFragmentAndLossScorerNoiseIntensityWeighted(0);
        ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionFix(c, probabilityDistribution, 0.9);

        EdgeScorer[] edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};
        NodeScorer[] nodeScorers = new NodeScorer[]{new StandardNodeScorer(true, 1d)};

        String[] ids = data.keySet().stream().map(Ms2Experiment::getName).toArray(String[]::new);

        List<FragmentsCandidate[]> candidateList = new ArrayList<>();
        for (Map.Entry<Ms2Experiment, List<FTree>> entry : data.entrySet()) {
            Ms2Experiment experiment = entry.getKey();
            List<FTree> trees = entry.getValue();
            List<FragmentsCandidate> fc = FragmentsCandidate.createAllCandidateInstances(trees, experiment);
            candidateList.add(fc.toArray(new FragmentsCandidate[0]));
        }

        FragmentsCandidate[][] candidates = candidateList.toArray(new FragmentsCandidate[0][]);

        GraphBuilder<FragmentsCandidate> graphBuilder =GraphBuilder.createGraphBuilder(ids, candidates, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
        graphBuilder.registerJobManager(SiriusJobs.getGlobalJobManager());
        graphBuilder.setState(JJob.JobState.RUNNING);
        graphBuilder.calculateWeight();

        System.out.println("weights");
        for (int i = 0; i < graphBuilder.graph.weights.length; i++) {
            System.out.println(Arrays.toString(graphBuilder.graph.weights[i].toArray()));

        }

        assertAfterCalculatingWeights(graphBuilder.graph);

        graphBuilder.setConnections();

        Graph<FragmentsCandidate> graph = graphBuilder.graph;

        assertResults(graph);

    }

    private void assertAfterCalculatingWeights(Graph<FragmentsCandidate> graph) {
        double[][] weights = new double[][]{
                new double[]{0.6836898487475431, 0.6836898487475431},
                new double[]{0.6836898487475431, 0.6836898487475431},
                new double[]{0.5266091798483991, 0.2640473591334766},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.8671766748914292, 0.8671766748914292, 0.5410392532759746},
                new double[]{0.8671766748914292, 0.8671766748914292, 0.2784774325610521},
                new double[]{0.18348682614388614, 0.18348682614388614, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515, 0.014430073427575515}
        };

        for (int i = 0; i < graph.weights.length; i++) {
            assertArrayEquals(graph.weights[i].toArray(), weights[i], 1e-12);
        }

        assertArrayEquals(graph.edgeThresholds, new double[] {
                -0.18348682614388614, -0.18348682614388614, -0.014430073427575515, 0.0, 0.0, 0.0, 0.0, 0.0,0.0
        }, 1e-12);
    }

    private void assertResults(Graph<FragmentsCandidate> graph) {

        assertArrayEquals(graph.ids, new String[]{"_3981384892034070220-1706-unknown1705", "_9944738237342398858-3008-unknown3007", "_18202298417616308149-930-unknown929"});
        assertEquals(graph.size, 9);
        assertArrayEquals(graph.boundaries, new int[]{3,5,8});
        assertArrayEquals(graph.edgeThresholds, new double[] {
                -0.18348682614388614, -0.18348682614388614, -0.18348682614388614, -0.18348682614388614, 0.0, 0.0, 0.0, 0.0,0.0
        }, 1e-12);

        int[][] connections = new int[][]{
                new int[]{5, 4, 6, 7, 8},
                new int[]{5, 4, 6, 7, 8},
                new int[]{5, 4, 6, 7, 8},
                new int[]{6, 8, 7},
                new int[]{0, 1, 2},
                new int[]{0, 1, 2},
                new int[]{3, 2, 1, 0},
                new int[]{3, 2, 1, 0},
                new int[]{3, 2, 1, 0}
        };
        for (int i = 0; i < graph.connections.length; i++) {
            assertArrayEquals(graph.connections[i], connections[i]);
        }

        double[][] weights = new double[][]{
                new double[]{0.8671766748914292, 0.8671766748914292, 0.18348682614388614, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.8671766748914292, 0.8671766748914292, 0.18348682614388614, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.5410392532759746, 0.2784774325610521, 0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.8671766748914292, 0.8671766748914292, 0.5410392532759746},
                new double[]{0.8671766748914292, 0.8671766748914292, 0.2784774325610521},
                new double[]{0.18348682614388614, 0.18348682614388614, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
                new double[]{0.014430073427575515, 0.014430073427575515, 0.014430073427575515, 0.014430073427575515},
        };
        for (int i = 0; i < graph.weights.length; i++) {
            assertArrayEquals(graph.weights[i].toArray(), weights[i], 1e-12);
        }

        String[] indexMapStrings = new String[]{
                "6=2, 5=1, 4=0, 8=4, 7=3",
                "6=2, 5=1, 4=0, 8=4, 7=3",
                "6=2, 5=1, 4=0, 8=4, 7=3",
                "6=0, 8=2, 7=1",
                "2=2, 1=1, 0=0",
                "2=2, 1=1, 0=0",
                "3=3, 2=2, 1=1, 0=0",
                "3=3, 2=2, 1=1, 0=0",
                "3=3, 2=2, 1=1, 0=0"
        };
        TIntIntHashMap[] indexMaps = Arrays.stream(indexMapStrings).map(s->ExamplePreparationUtils.intIntHashMapFromString(s)).toArray(TIntIntHashMap[]::new);
        for (int i = 0; i < graph.indexMap.length; i++) {
            assertEquals(graph.indexMap[i], indexMaps[i]);
        }


    }




}
