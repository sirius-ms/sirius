/*
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

package de.unijena.bioinf.fingerid.blast;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.unijena.bioinf.fingerid.blast.BayesnetScoring.SEP;

public class BayesnetScoringBuilder {
    private static final Logger Log = LoggerFactory.getLogger(BayesnetScoringBuilder.class);

    protected PredictionPerformance[] performances;
    protected ProbabilityFingerprint[] predicted;
    protected Fingerprint[] correct;
    protected int[][] covTreeEdges;
    protected boolean allowOnlyNegativeScores;

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringBuilder(performances, predicted, correct, covTreeEdges, false).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges) {
        return new BayesnetScoringBuilder(performances, predicted, correct, covTreeEdges, false).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath, boolean allowOnlyNegativeScores) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringBuilder(performances, predicted, correct, covTreeEdges, allowOnlyNegativeScores).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges, boolean allowOnlyNegativeScores) {
        return new BayesnetScoringBuilder(performances, predicted, correct, covTreeEdges, allowOnlyNegativeScores).buildScoring();
    }

    protected static BayesnetScoringBuilder getDummyInstance() {
        //todo implement different to override node classes
        return new BayesnetScoringBuilder(null, null, null, null, false);
    }


    public static BayesnetScoring readScoring(InputStream stream, Charset charset, FingerprintVersion fpVersion, double alpha, boolean allowOnlyNegativeScores) throws IOException {
        return readScoring(new BufferedReader(new InputStreamReader(stream, charset)), fpVersion, alpha, allowOnlyNegativeScores);
    }

    @Nullable
    public static BayesnetScoring readScoring(@Nullable BufferedReader treeReader, FingerprintVersion fpVersion, double alpha, boolean allowOnlyNegativeScores) throws IOException {
        return readScoring(treeReader, null, fpVersion, alpha, allowOnlyNegativeScores);
    }

    public static BayesnetScoring readScoring(@Nullable BufferedReader treeReader, @Nullable BufferedReader statisticsReader, FingerprintVersion fpVersion, double alpha, boolean allowOnlyNegativeScores) throws IOException {
        if (treeReader == null)
            return null;

        final List<String> lines = new ArrayList<>();
        String l;
        while ((l = treeReader.readLine()) != null) lines.add(l);

        if (lines.isEmpty())
            return null;

        List<int[]> edges = new ArrayList<>();
        final double[][] covariances = new double[lines.size()][];
        int pos = 0;
        for (String line : lines) {
            if (line.length() == 0) continue;
            String[] row = line.split(SEP);
            if (row.length == 6) {
                throw new RuntimeException("seems like the input file is still using old input format.");
//                //old format for tree as input
//                edges.add(new int[]{Integer.parseInt(row[0]), Integer.parseInt(row[1])});
//                covariances[pos] = new double[]{Double.parseDouble(row[2]), Double.parseDouble(row[3]), Double.parseDouble(row[4]), Double.parseDouble(row[5])};
            } else {
                int numberOfParents = Integer.parseInt(row[0]);

                int[] current_edges = new int[numberOfParents+1];
                for (int i = 1; i <= numberOfParents+1; i++) {
                    current_edges[i-1] = Integer.parseInt(row[i]);
                }
                edges.add(current_edges);
                double[] covs = new double[row.length-(numberOfParents+2)];
                for (int i = numberOfParents+2; i < row.length; i++) {
                    covs[i-(numberOfParents+2)] = Double.parseDouble(row[i]);
                }
                covariances[pos] = covs;
            }
            pos++;
        }

        int[][] covTreeEdges = edges.toArray(new int[0][]);

        if (covTreeEdges.length!=covariances.length) throw new RuntimeException("size of edge and covariances array differ");

        final BayesnetScoringBuilder dummyBuilder = getDummyInstance();
        final TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodes = dummyBuilder.parseTree(covTreeEdges, fpVersion);
        final List<BayesnetScoring.AbstractCorrelationTreeNode> fs = new ArrayList<>(10);
        final BayesnetScoring.AbstractCorrelationTreeNode[] nodeList = new BayesnetScoring.AbstractCorrelationTreeNode[nodes.size()];
        int k=0;

        for (BayesnetScoring.AbstractCorrelationTreeNode n : nodes.valueCollection()) {
            if (n.numberOfParents()==0) fs.add(n);
            nodeList[k++] = n;

        }

        final BayesnetScoring.AbstractCorrelationTreeNode[] forests = fs.toArray(new BayesnetScoring.AbstractCorrelationTreeNode[fs.size()]);
        for (int i = 0; i < covTreeEdges.length; i++) {
            int child = covTreeEdges[i][covTreeEdges[i].length-1];
            double[] cov = covariances[i];
            BayesnetScoring.AbstractCorrelationTreeNode node =  nodes.get(fpVersion.getRelativeIndexOf(child));
            node.setCovariance(cov);
        }

        if (dummyBuilder.hasCycles(forests, fpVersion)){
            throw new RuntimeException("bayes net contains cycles");
        }

        FingerprintStatistics statistics = null;
        if (statisticsReader != null)
            statistics = new ObjectMapper().readValue(statisticsReader, FingerprintStatistics.class);

        return new BayesnetScoring(nodes, nodeList, forests, alpha, fpVersion, null, allowOnlyNegativeScores, statistics);
    }


    public static BayesnetScoring readScoringFromFile(Path treeFile, FingerprintVersion fpVersion, double alpha) throws IOException {
        return readScoringFromFile(treeFile, fpVersion, alpha, false);
    }

    public static BayesnetScoring readScoringFromFile(Path treeFile, FingerprintVersion fpVersion, double alpha, boolean allowOnlyNegativeScores) throws IOException {
        InputStream inputStream = Files.newInputStream(treeFile);
        BayesnetScoring scoring =  readScoring(inputStream, Charset.forName("UTF-8"), fpVersion, alpha, allowOnlyNegativeScores);
        inputStream.close();
        return scoring;
    }

    protected boolean hasCycles(BayesnetScoring.AbstractCorrelationTreeNode[] roots, FingerprintVersion fpVersion){
        Queue<BayesnetScoring.AbstractCorrelationTreeNode> queue = new LinkedList<>();
        TIntHashSet visited = new TIntHashSet();
        for (BayesnetScoring.AbstractCorrelationTreeNode root : roots) {
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            BayesnetScoring.AbstractCorrelationTreeNode node = queue.poll();
            int idx = node.getFingerprintIndex();
            if (visited.contains(idx)){
                System.out.printf("idx %d is part of a cycle%n",fpVersion.getAbsoluteIndexOf(idx));
                return true;
            }
            visited.add(idx);
            for (BayesnetScoring.AbstractCorrelationTreeNode child : node.getChildren()) {
                if (visited.contains(child.getFingerprintIndex())|| queue.contains(child)) continue;
                queue.add(child);
            }
        }
        return false;
    }


    private static Pattern EdgePattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s*");
    private static Pattern EdgePatternDot = Pattern.compile("(\\d+)\\s*->\\s*(\\d+)\\s*");

    private static Pattern SinlgeNodePattern = Pattern.compile("(\\d+)\\s*");


    /*
    parse the molecular property tree from a dot-like file or tab separated vertices
     */
    private TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> parseTreeFile(Path dotFile, FingerprintVersion fpVersion) throws IOException {
        int[][] edges;
        if (containsArrow(dotFile)){
            edges = parseTreeFromFile(dotFile, EdgePatternDot);
        } else {
            edges = parseTreeFromFile(dotFile, EdgePattern);

        }
        return parseTree(edges, fpVersion);
    }

    private boolean containsArrow(Path file) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            String line;
            while ((line=br.readLine())!=null) {
                Matcher m = EdgePatternDot.matcher(line);
                if (m.find()) {
                    return true;
                }
                m = EdgePattern.matcher(line);
                if (m.find()) {
                    return false;
                }
            }
        }
        return false;
    }

    public static int[][] parseTreeFromDotFile(Path dotFile) throws IOException {
        return parseTreeFromFile(dotFile, EdgePatternDot);
    }

    public static int[][] parseTreeFromFile(Path dotFile) throws IOException {
        return parseTreeFromFile(dotFile, EdgePattern);
    }


    public static int[][] parseTreeFromFile(Path file, Pattern edgePattern) throws IOException {
        List<int[]> edges = new ArrayList<>();
        try (final BufferedReader br = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            String line;
            while ((line=br.readLine())!=null) {
                Matcher m = edgePattern.matcher(line);
                if (m.find()) {
                    final int u = Integer.parseInt(m.group(1));
                    final int v = Integer.parseInt(m.group(2));
                    edges.add(new int[]{u,v});
                } else {
                    m = SinlgeNodePattern.matcher(line);
                    if (m.find()) {
                        final int u = Integer.parseInt(m.group(1));
                        edges.add(new int[]{u});
                    }
                }
            }
        }
        return edges.toArray(new int[0][]);
    }

    protected double getProbability(ProbabilityFingerprint fp, int idx, boolean isCorrectInCandidate) {
        return fp.getProbability(idx);
    }

    /*
    parse molecular property net from a array of edges.
    [[a,b],...,[x,y,z]] contains edges a->b, ..., x->y,x->z //todo isn't it x->z and y->z ?
    absolute indices!
    try to correct for missing and unused properties
    important: all parents or a node must be contained in one such int[], last elements always represent the child node.
    Each node (index) can be last element only once.
     */
    protected TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> parseTree(int[][] absIndices, FingerprintVersion fpVersion){
        TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodes = new TIntObjectHashMap<>();

        for (int[] absIndex : absIndices) {
            final int v = absIndex[absIndex.length-1];

            BayesnetScoring.AbstractCorrelationTreeNode[] parentNodes = new BayesnetScoring.AbstractCorrelationTreeNode[absIndex.length-1];
            for (int i = 0; i < absIndex.length-1; i++) {
                parentNodes[i] = nodes.get(absIndex[i]);
                if (parentNodes[i]==null){
                    //root or just unknown
                    parentNodes[i] = createTreeNode(absIndex[i]);
                    nodes.put(absIndex[i], parentNodes[i]);
                }
            }


            BayesnetScoring.AbstractCorrelationTreeNode currentNode = nodes.get(v);
            if (currentNode!=null){
                //already known
                List<BayesnetScoring.AbstractCorrelationTreeNode> children = new ArrayList<>(currentNode.getChildren());
                BayesnetScoring.AbstractCorrelationTreeNode[] old_parents = currentNode.getParents();
                List<BayesnetScoring.AbstractCorrelationTreeNode> combinedParents = new ArrayList<>();
                for (BayesnetScoring.AbstractCorrelationTreeNode parentNode : old_parents) {
                    if (parentNode==null) continue;
                    parentNode.removeChild(currentNode);
                    combinedParents.add(parentNode);
                }
                for (BayesnetScoring.AbstractCorrelationTreeNode parentNode : parentNodes) {
                    if (parentNode==null) continue;
                    parentNode.removeChild(currentNode);
                    combinedParents.add(parentNode);
                }
                parentNodes = combinedParents.toArray(new BayesnetScoring.AbstractCorrelationTreeNode[0]);
                BayesnetScoring.AbstractCorrelationTreeNode newNode = createTreeNode(v, parentNodes);
                for (BayesnetScoring.AbstractCorrelationTreeNode child : children) {
                    child.replaceParent(currentNode, newNode);
                    newNode.getChildren().add(child);
                }
                currentNode = newNode;
            } else {
                currentNode = createTreeNode(v, parentNodes);
            }

            nodes.put(v, currentNode);
            for (BayesnetScoring.AbstractCorrelationTreeNode parentNode : parentNodes) {
                parentNode.getChildren().add(currentNode);
            }
        }

        for (int i : nodes.keys()) {
            if (!fpVersion.hasProperty(i))
                throw new RuntimeException("tree contains property " + i +" which is not part of the fingerprint " + fpVersion);
        }


        // convert to relative indices
        TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodesRelativeIdx = new TIntObjectHashMap<>();
        for (int i : nodes.keys()) {
            int relIdx = fpVersion.getRelativeIndexOf(i);
            BayesnetScoring.AbstractCorrelationTreeNode node = nodes.get(i);
            node.setFingerprintIndex(relIdx);
            nodesRelativeIdx.put(relIdx, node);
        }

        //add properties not contained in tree
        TIntArrayList notContained = new TIntArrayList();
        for (int i = 0; i < fpVersion.size(); i++) {
            if (!nodesRelativeIdx.contains(i)){
                notContained.add(fpVersion.getAbsoluteIndexOf(i));
                nodesRelativeIdx.put(i, createTreeNode(i));
            }
        }
        if (notContained.size()>0){
            Log.debug("the following properties are not contained in the scoring tree: "+ Arrays.toString(notContained.toArray()));
        }

        return nodesRelativeIdx;
    }

    protected BayesnetScoring.AbstractCorrelationTreeNode createTreeNode(int fingerprintIndex, BayesnetScoring.AbstractCorrelationTreeNode... parentNodes){
        if (parentNodes.length==0){
            return new BayesnetScoring.CorrelationTreeNode(fingerprintIndex);
        } else if (parentNodes.length==1){
            return new BayesnetScoring.CorrelationTreeNode(fingerprintIndex, parentNodes[0]);
        } else if (parentNodes.length==2) {
            //currently not used. would only be part of BayesnetScoringWithTwoParents
            return new BayesnetScoringWithTwoParents.TwoParentsCorrelationTreeNode(fingerprintIndex, parentNodes);
        } else {
            throw new RuntimeException("don't support nodes with no or more than 2 parents");
        }
    }

    protected double laplaceSmoothing(double probability, double alpha) {
        //todo necessary here and in BayesnetScoring?
        return (probability + alpha) / (1d + 2d * alpha);
    }


    public BayesnetScoringBuilder(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges, boolean allowOnlyNegativeScores){
        this.performances =performances;
        this.predicted = predicted;
        this.correct = correct;
        this.covTreeEdges = covTreeEdges;
        this.allowOnlyNegativeScores = allowOnlyNegativeScores;
    }

    public BayesnetScoring buildScoring(){
        long time = System.currentTimeMillis();
        FingerprintVersion fpVersion = predicted[0].getFingerprintVersion();

        TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodes = parseTree(covTreeEdges, fpVersion);
        List<BayesnetScoring.AbstractCorrelationTreeNode> fs = new ArrayList<>(10);
        BayesnetScoring.AbstractCorrelationTreeNode[] nodeList = new BayesnetScoring.AbstractCorrelationTreeNode[nodes.size()];
        int k=0;
        for (BayesnetScoring.AbstractCorrelationTreeNode n : nodes.valueCollection()) {
            if (n.numberOfParents()==0) fs.add(n);
            nodeList[k++] = n;
        }
        BayesnetScoring.AbstractCorrelationTreeNode[] forests = fs.toArray(new BayesnetScoring.CorrelationTreeNode[fs.size()]);
        double alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
        makeStatistics(nodeList, alpha);

        if (hasCycles(forests, fpVersion)){
            throw new RuntimeException("bayes net contains cycles");
        }
        long time2 = System.currentTimeMillis();
        LoggerFactory.getLogger(BayesnetScoringBuilder.class).warn("Build scoring took " + (time2-time)  + " ms." );

        return getNewInstance(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);

    }

    protected BayesnetScoring getNewInstance(TIntObjectHashMap<BayesnetScoring.AbstractCorrelationTreeNode> nodes, BayesnetScoring.AbstractCorrelationTreeNode[] nodeList, BayesnetScoring.AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores){
        return new BayesnetScoring(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);
    }



    protected void makeStatistics(BayesnetScoring.AbstractCorrelationTreeNode[] nodeList, double alpha) {
        for (int i = 0; i < predicted.length; i++) {
            double[] probFp = predicted[i].toProbabilityArray();
            boolean[] reality = correct[i].toBooleanArray();

            for (BayesnetScoring.AbstractCorrelationTreeNode node : nodeList) {
                final boolean isRoot = node.numberOfParents()==0;
                if (isRoot) continue;

                BayesnetScoring.AbstractCorrelationTreeNode[] parents = node.getParents();

                double[] parentsPredictions = getParentPredictions(parents, probFp, reality, performances, alpha);
                boolean[] parentsTruth = getParentTruth(parents, reality);
                final boolean truth = reality[node.getFingerprintIndex()];
                final double prediction = laplaceSmoothing(probFp[node.getFingerprintIndex()], alpha);

                node.addPlattThis(prediction, truth, parentsTruth);
                for (int j = 0; j < parentsTruth.length; j++) {
                    node.addPlattOfParent(parentsPredictions[j], j, truth, parentsTruth);
                }
            }

        }

        for (BayesnetScoring.AbstractCorrelationTreeNode node : nodeList) {
            node.computeCovariance();
        }

    }

    protected double transformProbability(double predicted, boolean trueValue, PredictionPerformance performance, double alpha){
        return laplaceSmoothing(predicted, alpha);
    }

    protected double[] getParentPredictions(BayesnetScoring.AbstractCorrelationTreeNode[] parents, double[] predictedFP, boolean[] trueFP, PredictionPerformance[] performances, double alpha){
        double[] parentPlatt = new double[parents.length];
        for (int i = 0; i < parents.length; i++) {
            final BayesnetScoring.AbstractCorrelationTreeNode parent = parents[i];
//            final double platt = laplaceSmoothing(predictedFP[parent.getFingerprintIndex()], alpha);
            //changed
            int idx = parent.getFingerprintIndex();

            final double platt = transformProbability(predictedFP[idx], trueFP[idx], performances[idx], alpha);
            parentPlatt[i] = platt;
        }
        return parentPlatt;
    }

    protected boolean[] getParentTruth(BayesnetScoring.AbstractCorrelationTreeNode[] parents, boolean[] trueFP){
        boolean[] parentTruth = new boolean[parents.length];
        for (int i = 0; i < parents.length; i++) {
            final BayesnetScoring.AbstractCorrelationTreeNode parent = parents[i];
            parentTruth[i] = trueFP[parent.getFingerprintIndex()];
        }
        return parentTruth;
    }

    protected void setCovariance(BayesnetScoring.AbstractCorrelationTreeNode node, double[] covariances) {
        node.setCovariance(covariances);
    }
}
