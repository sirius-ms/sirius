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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CovarianceScoringMethod implements FingerblastScoringMethod {


    protected final TIntObjectHashMap<CorrelationTreeNode> nodes;
    protected final CorrelationTreeNode[] nodeList;
    protected final CorrelationTreeNode[] forests;
    protected final double alpha;
    protected final FingerprintVersion fpVersion;

    protected File file;


    // static helper methods
    protected static double laplaceSmoothing(double probability, double alpha) {
        return (probability + alpha) / (1d + 2d * alpha);
    }

    public static double getCovarianceScoringAlpha(PredictionPerformance[] performances) {
        return 1d / performances[0].withPseudoCount(0.25).numberOfSamplesWithPseudocounts();
    }

    private static final String SEP = "\t";


    public static CovarianceScoringMethod readScoring(Reader reader, FingerprintVersion fpVersion, PredictionPerformance[] performances) throws IOException {
        return readScoring(reader, fpVersion, getCovarianceScoringAlpha(performances));
    }

    public static CovarianceScoringMethod readScoring(InputStream stream, Charset charset, FingerprintVersion fpVersion, PredictionPerformance[] performances) throws IOException {
        return readScoring(new InputStreamReader(stream, charset), fpVersion, getCovarianceScoringAlpha(performances));
    }

    public static CovarianceScoringMethod readScoring(Reader reader, FingerprintVersion fpVersion, double alpha) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            final List<String> lines = new ArrayList<>();
            String l;
            while ((l = bufferedReader.readLine()) != null) lines.add(l);

            final int[][] edges = new int[lines.size()][];
            final double[][] covariances = new double[lines.size()][];
            int pos = 0;
            for (String line : lines) {
                if (line.length() == 0) continue;
                String[] col = line.split(SEP);
                edges[pos] = new int[]{Integer.parseInt(col[0]), Integer.parseInt(col[1])};
                covariances[pos] = new double[]{Double.parseDouble(col[2]), Double.parseDouble(col[3]), Double.parseDouble(col[4]), Double.parseDouble(col[5])};
                pos++;
            }
            return new CovarianceScoringMethod(edges, covariances, fpVersion, alpha);
        }
    }

    public static CovarianceScoringMethod readScoringFromFile(Path treeFile, FingerprintVersion fpVersion, double alpha) throws IOException {
        return readScoring(new InputStreamReader(Files.newInputStream(treeFile), Charset.forName("UTF-8")), fpVersion, alpha);
    }

    private static Pattern EdgePattern = Pattern.compile("(\\d+)\\s*->\\s*(\\d+)\\s*");

    public static int[][] parseTreeFromDotFile(Path dotFile) throws IOException {
        List<int[]> edges = new ArrayList<>();
        try (final BufferedReader br = Files.newBufferedReader(dotFile, Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final Matcher m = EdgePattern.matcher(line);
                if (m.find()) {
                    final int u = Integer.parseInt(m.group(1));
                    final int v = Integer.parseInt(m.group(2));
                    edges.add(new int[]{u, v});
                }
            }
        }
        return edges.toArray(new int[0][]);
    }




    /**
     *
     * @param covTreeEdges array of edges int[k][0] -- int[k][1], using absolute indices
     * @param covariances covariances per edge in the form cov(rootTrue,childTrue), cov(rootTrue,childFalse), cov(rootFalse,childTrue), cov(rootFalse,childFalse)
     * @param fpVersion corresponding {@link FingerprintVersion}
     * @param alpha alpha used for laplace smoothing
     */
    public CovarianceScoringMethod(int[][] covTreeEdges, double[][] covariances, FingerprintVersion fpVersion, double alpha) {
        this.fpVersion = fpVersion;
        this.nodes = parseTree(covTreeEdges, fpVersion);
        List<CorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new CorrelationTreeNode[nodes.size()];
        int k=0;
        for (CorrelationTreeNode n : nodes.valueCollection()) {
            if (n.parent==null) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        for (int i = 0; i < covTreeEdges.length; i++) {
            int child = covTreeEdges[i][1];
            double[] cov = covariances[i];
            CorrelationTreeNode node =  nodes.get(fpVersion.getRelativeIndexOf(child));
            node.setCovariance(cov);
        }
        this.alpha = alpha;
    }

    public CovarianceScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, File dotFile) throws IOException {
        this(performances, predicted, correct, dotFile.toPath());
    }

    public CovarianceScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath) throws IOException {
        this.fpVersion = predicted[0].getFingerprintVersion();
        this.nodes = parseTreeFile(dotFilePath, predicted[0].getFingerprintVersion());
        List<CorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new CorrelationTreeNode[nodes.size()];
        int k=0;
        for (CorrelationTreeNode n : nodes.valueCollection()) {
            if (n.parent==null) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        makeStatistics(predicted, correct);

        this.alpha = getCovarianceScoringAlpha(performances);
    }

    public CovarianceScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges) {
        this.fpVersion = predicted[0].getFingerprintVersion();

        this.nodes = parseTree(covTreeEdges, fpVersion);
        List<CorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new CorrelationTreeNode[nodes.size()];
        int k=0;
        for (CorrelationTreeNode n : nodes.valueCollection()) {
            if (n.parent==null) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        makeStatistics(predicted, correct);
        this.alpha = getCovarianceScoringAlpha(performances);
    }



    protected void makeStatistics(ProbabilityFingerprint[] predicted, Fingerprint[] correct) {
        for (int i = 0; i < predicted.length; i++) {
            double[] probFp = predicted[i].toProbabilityArray();
            boolean[] reality = correct[i].toBooleanArray();

            for (CorrelationTreeNode node : nodeList) {
                final CorrelationTreeNode correlationTreeNode = node;
                final boolean isRoot = node.parent==null;
                if (isRoot) continue;
                final double rootPrediction = laplaceSmoothing(isRoot ? 0.0 : probFp[node.parent.fingerprintIndex], alpha);
//                final double oneMinusRootPrediction = 1d-rootPrediction;
                final boolean rootReality = (!isRoot && reality[node.parent.fingerprintIndex]);
                final double prediction = laplaceSmoothing(probFp[node.fingerprintIndex], alpha);
//                final double oneMinusPrediction = 1d-prediction;
                final boolean real = reality[node.fingerprintIndex];

                correlationTreeNode.plattByRef[correlationTreeNode.getIdxRootPlatt(rootReality, real)].add(rootPrediction);
                correlationTreeNode.plattByRef[correlationTreeNode.getIdxThisPlatt(rootReality, real)].add(prediction);
            }

        }

        for (CorrelationTreeNode node : nodeList) {
            node.computeCovariance();
        }

    }

    public int getNumberOfRoots(){
        return forests.length;
    }


    public void writeTreeWithCovToFile(Path outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
            for (CorrelationTreeNode node : nodeList) {
                if (node.parent == null) continue;
                int parent = fpVersion.getAbsoluteIndexOf(node.parent.fingerprintIndex);
                int child = fpVersion.getAbsoluteIndexOf(node.fingerprintIndex);

                StringBuilder builder = new StringBuilder();
                builder.append(String.valueOf(parent));
                builder.append(SEP);
                builder.append(String.valueOf(child));
                builder.append(SEP);
                builder.append(String.valueOf(node.getCovariance(true, true)));
                builder.append(SEP);
                builder.append(String.valueOf(node.getCovariance(true, false)));
                builder.append(SEP);
                builder.append(String.valueOf(node.getCovariance(false, true)));
                builder.append(SEP);
                builder.append(String.valueOf(node.getCovariance(false, false)));
                builder.append("\n");
                writer.write(builder.toString());
            }
        }
    }


    /*
    parse the molecular property tree from a dot-like file
     */
    private TIntObjectHashMap<CorrelationTreeNode> parseTreeFile(Path dotFile, FingerprintVersion fpVersion) throws IOException {
        return parseTree(parseTreeFromDotFile(dotFile), fpVersion);
    }



    /*
    parse molecular property ree from a array of edges.
    [[a,b],...,[x,y]] contains edges a->b, ..., x->y
    absolute indices!
    try to correct for missing and unused properties
     */
    private TIntObjectHashMap<CorrelationTreeNode> parseTree(int[][] absIndices, FingerprintVersion fpVersion){
        TIntObjectHashMap<CorrelationTreeNode> nodes = new TIntObjectHashMap<>();
        for (int[] absIndex : absIndices) {
            final int u = absIndex[0];
            final int v = absIndex[1];
            if (nodes.get(u)==null) nodes.put(u, createTreeNode(u));
            if (nodes.get(v)==null) nodes.put(v, createTreeNode(v));
            nodes.get(u).children.add(nodes.get(v));
            nodes.get(v).parent = nodes.get(u);
        }

        boolean changed = true;
        while (changed){
            changed = false;
            for (int i : nodes.keys()) {
                //remove perperties not contained in fpVerion;
                if (!fpVersion.hasProperty(i) ){
//                    System.out.println("property not contained in fpVerion: "+i);
                    CorrelationTreeNode node = nodes.get(i);
                    CorrelationTreeNode parent = node.parent;
                    List<CorrelationTreeNode> newChildren = new ArrayList<>();
                    for (CorrelationTreeNode child : node.children) {

                        child.parent = node.parent;
                        newChildren.add(child);
                    }
                    if (parent!=null){
                        parent.children.remove(node);
                        parent.children.addAll(newChildren);
                    }
                    nodes.remove(i);
                    changed = true;
                }
            }
        }

        TIntObjectHashMap<CorrelationTreeNode> nodesRelativeIdx = new TIntObjectHashMap<>();
        for (int i : nodes.keys()) {
            int relIdx = fpVersion.getRelativeIndexOf(i);
            CorrelationTreeNode node = nodes.get(i);
            node.fingerprintIndex = relIdx;
            nodesRelativeIdx.put(relIdx, node);
        }

        //add properties not contained in tree
        for (int i = 0; i < fpVersion.size(); i++) {
            if (!nodesRelativeIdx.contains(i)){
//                System.out.println("property not contained in tree: "+i+" (absolute "+fpVersion.getAbsoluteIndexOf(i)+")");
                nodesRelativeIdx.put(i, createTreeNode(i));
            }
        }

        return nodesRelativeIdx;
    }

    protected CorrelationTreeNode createTreeNode(int fingerprintIndex){
        return new CorrelationTreeNode(fingerprintIndex);
    }


    private static int RootT=0,RootF=1,ChildT=0,ChildF=1;
    protected class CorrelationTreeNode{

        protected CorrelationTreeNode parent;
        protected List<CorrelationTreeNode> children;
        protected int fingerprintIndex;

        protected double[][] covariances;
        TDoubleArrayList[] plattByRef;

        public CorrelationTreeNode(int fingerprintIndex) {
            this.fingerprintIndex = fingerprintIndex;
            this.covariances = new double[2][2];
            this.children = new ArrayList<>();
            initPlattByRef();
        }

        private void initPlattByRef(){
            plattByRef = new TDoubleArrayList[8];
            for (int j = 0; j < plattByRef.length; j++) {
                plattByRef[j] = new TDoubleArrayList();
            }

            //add 'pseudo counts'
            for (int j = 0; j < 4; j++) {
                //00
                plattByRef[2*j].add(0d);
                plattByRef[2*j+1].add(0d);
                //01
                plattByRef[2*j].add(0d);
                plattByRef[2*j+1].add(1d);
                //10
                plattByRef[2*j].add(1d);
                plattByRef[2*j+1].add(0d);
                //11
                plattByRef[2*j].add(1d);
                plattByRef[2*j+1].add(1d);
            }
        }

        int getIdxThisPlatt(boolean rootTrue, boolean thisTrue){
            int idx = (rootTrue ? 4 : 0)+((thisTrue ? 2 : 0));
            return idx+1;
        }

        int getIdxRootPlatt(boolean rootTrue, boolean thisTrue){
            int idx = (rootTrue ? 4 : 0)+((thisTrue ? 2 : 0));
            return idx;
        }

        void computeCovariance(){
            covariances[RootT][ChildT] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, true)].toArray(), plattByRef[getIdxRootPlatt(true, true)].toArray());
            covariances[RootT][ChildF] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, false)].toArray(), plattByRef[getIdxRootPlatt(true, false)].toArray());
            covariances[RootF][ChildT] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, true)].toArray(), plattByRef[getIdxRootPlatt(false, true)].toArray());
            covariances[RootF][ChildF] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, false)].toArray(), plattByRef[getIdxRootPlatt(false, false)].toArray());
            initPlattByRef();//remove oldPlatt
        }

        void setCovariance(double[] covariances){
            this.covariances[RootT][ChildT] = covariances[0];
            this.covariances[RootT][ChildF] = covariances[1];
            this.covariances[RootF][ChildT] = covariances[2];
            this.covariances[RootF][ChildF] = covariances[3];
            this.initPlattByRef();//remove oldPlatt
        }

        protected double getCovariance(boolean realParent, boolean real){
            return this.covariances[realParent?RootT:RootF][real?ChildT:ChildF];
        }
    }


    public Scoring getScoring() {
        return new Scoring();
    }

    public Scoring getScoring(PredictionPerformance[] performances) {
        return new Scoring();
    }


    public class Scoring implements FingerblastScoring {
        protected double[][][] abcdMatrixByNodeIdxAndCandidateProperties;


        protected int getIndex(boolean rootTrue, boolean thisTrue){
            return (rootTrue ? 2 : 0)+((thisTrue ? 1 : 0));
        }

        double[] getABCDMatrix(int nodeIdx, boolean rootTrue, boolean thisTrue){
            return abcdMatrixByNodeIdxAndCandidateProperties[nodeIdx][getIndex(rootTrue, thisTrue)];
        }


        private double threshold, minSamples;

        public Scoring() {
            this.threshold = 0;
            minSamples = 0;
        }

        @Override
        public double getThreshold() {
            return threshold;
        }

        @Override
        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public double getMinSamples() {
            return minSamples;
        }

        @Override
        public void setMinSamples(double minSamples) {
            this.minSamples = minSamples;
        }


        @Override
        public void prepare(ProbabilityFingerprint fingerprint) {
            abcdMatrixByNodeIdxAndCandidateProperties = new double[nodeList.length][][];

            double[] fp = fingerprint.toProbabilityArray();

            for (CorrelationTreeNode root : forests) {

                // process conditional probabilities
                for (CorrelationTreeNode child : root.children) {
                    prepare(child, fp);
                }
            }
        }

        void prepare(CorrelationTreeNode v, double[] fingerprint){
            final CorrelationTreeNode u = v.parent;
            final int i = u.fingerprintIndex;
            final int j = v.fingerprintIndex;
            final double p_i = laplaceSmoothing(fingerprint[i], alpha);
            final double p_j = laplaceSmoothing(fingerprint[j], alpha);


            double[][] matrices = new double[4][];

            for (int k = 0; k < 2; k++) {
                boolean parentTrue = (k==0);
                for (int l = 0; l < 2; l++) {
                    boolean childTrue = (l==0);

                    final double covariance = v.getCovariance(parentTrue, childTrue);
                    double[] abcd = computeABCD(covariance, p_i, p_j);
                    matrices[getIndex(parentTrue, childTrue)] = abcd;

                }

            }

            abcdMatrixByNodeIdxAndCandidateProperties[j] = matrices;

            for (CorrelationTreeNode child : v.children) {
                prepare(child, fingerprint);
            }
        }

        @Override
        public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
            double logProbability = 0d;

            double[] fp = fingerprint.toProbabilityArray();
            boolean[] bool = databaseEntry.toBooleanArray();
            for (CorrelationTreeNode root : forests) {
                final int i = root.fingerprintIndex;
                final double prediction = laplaceSmoothing(fp[i],alpha);
                final double oneMinusPrediction = 1d-prediction;
                final boolean real = bool[i];

                if (real){
                    logProbability += Math.log(prediction);
                } else {
                    logProbability += Math.log(oneMinusPrediction);
                }

                // process conditional probabilities
                for (CorrelationTreeNode child : root.children) {
                    logProbability += conditional(fp, bool, child);
                }
            }

            return logProbability;
        }


        protected double conditional(double[] fingerprint, boolean[] databaseEntry, CorrelationTreeNode v) {
            double score;
            final CorrelationTreeNode u = v.parent;
            final int i = u.fingerprintIndex;
            final int j = v.fingerprintIndex;
            final double p_i = laplaceSmoothing(fingerprint[i], alpha);
            final boolean real = databaseEntry[j];
            final boolean realParent = databaseEntry[i];



            //matrix
            /*
                    Di
                ------------
             |  a       b   |   pj
         Dj  |              |
             |  c       d   |   1-pj
                ------------
                pi      1-pi


            (1) a+b = pj
            (2) a+c = pi
            (3) a-pi*pj = learnedij
            (4) a+b+c+d = 1

            learnedij is the covariance between i,j

             */



            double[] abcd = getABCDMatrix(j, realParent, real);


            final double a = abcd[0];
            final double b = abcd[1];
            final double c = abcd[2];
            final double d = abcd[3];


            if (real){
                if (realParent){
                    score = Math.log(a/p_i);
                } else {
                    score = Math.log(b/(1-p_i));
                }
            } else {
                if (realParent){
                    score = Math.log(c/p_i);
                } else {
                    score = Math.log(d/(1-p_i));
                }
            }

            if (Double.isNaN(score) || Double.isInfinite(score)){
                System.err.println("NaN score for the following fingerprints:");
                System.err.println(Arrays.toString(fingerprint));
                System.err.println(Arrays.toString(databaseEntry));
                System.err.println("for tree node u (" + u.fingerprintIndex + ") -> v (" + v.fingerprintIndex + ")");
                System.err.println("with covariances: ");
                for (double[] row : v.covariances) {
                    System.err.println("\t" + Arrays.toString(row));
                }
                System.err.printf(Locale.US, "and a = %f, b = %f, c = %f, d = %f\n", a, b, c, d);
                System.err.printf(Locale.US, "p_i = %f\n", p_i);
                System.err.printf(Locale.US, "alpha = %f\n", alpha);
                throw new RuntimeException("bad score: "+score);
            }

            for (CorrelationTreeNode child : v.children) {
                score += conditional(fingerprint,databaseEntry, child);
            }
            return score;
        }

        protected double[] computeABCD(double covariance, double p_i, double p_j) {
            final double pipj = p_i*p_j;

            //a = learned_ij+pi*pj and \in [0, min{pi,pj}]
            double a = covariance+pipj;
            if (a<0){
                a = 0;
            }
            else if (a > Math.min(p_i, p_j)){
                a = Math.min(p_i, p_j);
            }
            if (a<(p_i+p_j-1)){
                a = p_i+p_j-1;
            }


//            else {
//                System.out.println("a is "+a+" and pi,pj "+p_i+" , "+p_j);
//            }

            double b = p_j-a;
            double c = p_i-a;
            double d = 1d-a-b-c;
            if (d<0) d = 0;

            //normalize
            double pseudoCount = alpha;
            a+=pseudoCount; b+=pseudoCount; c+=pseudoCount; d+=pseudoCount;
            double norm = 1d+4*pseudoCount;
            a/=norm; b/=norm; c/=norm; d/=norm;
            return new double[]{a,b,c,d};
        }
    }
}
