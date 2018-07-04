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

public class BayesnetScoring {

    protected final TIntObjectHashMap<AbstractCorrelationTreeNode> nodes;
    protected final AbstractCorrelationTreeNode[] nodeList;
    protected final AbstractCorrelationTreeNode[] forests;
    protected final double alpha;
    protected final FingerprintVersion fpVersion;

    protected File file;

    /**
     *
     * @param covTreeEdges array of edges int[k][0] -- int[k][1] or int[l][0] -- int[l][2], int[l][1] -- int[l][2] using absolute indices
     * @param covariances covariances per edge. Use correct ordering for each kind of nodes (one or two parent node)
     * @param fpVersion corresponding {@link FingerprintVersion}
     * @param alpha alpha used for laplace smoothing
     */
    public BayesnetScoring(int[][] covTreeEdges, double[][] covariances, FingerprintVersion fpVersion, double alpha){
        this.fpVersion = fpVersion;
        this.nodes = parseTree(covTreeEdges, fpVersion);
        List<AbstractCorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new CorrelationTreeNode[nodes.size()];
        int k=0;
        for (AbstractCorrelationTreeNode n : nodes.valueCollection()) {
            if (n.numberOfParents()==0) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        for (int i = 0; i < covTreeEdges.length; i++) {
            int child = covTreeEdges[i][1];
            double[] cov = covariances[i];
            AbstractCorrelationTreeNode node =  nodes.get(fpVersion.getRelativeIndexOf(child));
            node.setCovariance(cov);
        }
        this.alpha = alpha;
    }

    public BayesnetScoring(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, File dotFile) throws IOException {
        this(performances, predicted, correct, dotFile.toPath());
    }

    public BayesnetScoring(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, Path dotFilePath) throws IOException {
        this.fpVersion = predicted[0].getFingerprintVersion();
        this.nodes = parseTreeFile(dotFilePath, predicted[0].getFingerprintVersion());
        List<AbstractCorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new AbstractCorrelationTreeNode[nodes.size()];
        int k=0;
        for (AbstractCorrelationTreeNode n : nodes.valueCollection()) {
            if (n.numberOfParents()==0) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        makeStatistics(predicted, correct);

        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
    }

    public BayesnetScoring(PredictionPerformance[] performances, ProbabilityFingerprint[] predicted, Fingerprint[] correct, int[][] covTreeEdges) {
        this.fpVersion = predicted[0].getFingerprintVersion();

        this.nodes = parseTree(covTreeEdges, fpVersion);
        List<AbstractCorrelationTreeNode> fs = new ArrayList<>(10);
        this.nodeList = new CorrelationTreeNode[nodes.size()];
        int k=0;
        for (AbstractCorrelationTreeNode n : nodes.valueCollection()) {
            if (n.numberOfParents()==0) fs.add(n);
            nodeList[k++] = n;
        }
        this.forests = fs.toArray(new CorrelationTreeNode[fs.size()]);
        makeStatistics(predicted, correct);
        this.alpha = 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts();
    }

    private static final String SEP = "\t";

    public void  writeTreeWithCovToFile(Path outputFile) throws IOException{
        try(BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
            for (AbstractCorrelationTreeNode node : nodeList) {
                if (node.numberOfParents()==0) continue;
                int child = fpVersion.getAbsoluteIndexOf(node.getFingerprintIndex());

                StringBuilder builder = new StringBuilder();
                builder.append(String.valueOf(node.numberOfParents())); builder.append(SEP);
                for (AbstractCorrelationTreeNode p : node.getParents()) {
                    builder.append(String.valueOf(p)); builder.append(SEP);
                }
                builder.append(String.valueOf(child)); builder.append(SEP);
                double[] covariances = node.getCovarianceArray();
                for (int i = 0; i < covariances.length; i++) {
                    builder.append(String.valueOf(covariances[i]));
                    if (i<covariances.length-1) builder.append(SEP);
                }
                builder.append(String.valueOf(child)); builder.append(SEP);

                builder.append("\n");
                writer.write(builder.toString());
            }
        }
    }

    public static CovarianceScoring readScoring(InputStream stream, Charset charset, FingerprintVersion fpVersion, double alpha) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));

        final List<String> lines = new ArrayList<>();
        String l;
        while ((l=reader.readLine())!=null) lines.add(l);

        final int[][] edges = new int[lines.size()][];
        final double[][] covariances = new double[lines.size()][];
        int pos = 0;
        for (String line : lines) {
            if (line.length()==0) continue;
            String[] col = line.split(SEP);
            edges[pos] = new int[]{Integer.parseInt(col[0]), Integer.parseInt(col[1])};
            covariances[pos] = new double[]{Double.parseDouble(col[2]), Double.parseDouble(col[3]), Double.parseDouble(col[4]), Double.parseDouble(col[5])};
            pos++;
        }
        return new CovarianceScoring(edges, covariances, fpVersion, alpha);
    }

    public static CovarianceScoring readScoringFromFile(Path treeFile, FingerprintVersion fpVersion, double alpha) throws IOException {
        return readScoring(Files.newInputStream(treeFile), Charset.forName("UTF-8"), fpVersion, alpha);
    }

    protected void makeStatistics(ProbabilityFingerprint[] predicted, Fingerprint[] correct) {
        for (int i = 0; i < predicted.length; i++) {
            double[] probFp = predicted[i].toProbabilityArray();
            boolean[] reality = correct[i].toBooleanArray();

            for (AbstractCorrelationTreeNode node : nodeList) {
                final boolean isRoot = node.numberOfParents()==0;
                if (isRoot) continue;

                AbstractCorrelationTreeNode[] parents = node.getParents();

                double[] parentsPredictions = getParentPredictions(parents, probFp);
                boolean[] parentsTruth = getParentTruth(parents, reality);
                final boolean truth = reality[node.getFingerprintIndex()];
                final double prediction = laplaceSmoothing(probFp[node.getFingerprintIndex()], alpha);

                node.addPlattThis(prediction, truth, parentsTruth);
                for (int j = 0; j < parentsTruth.length; j++) {
                    node.addPlattOfParent(parentsPredictions[j], j, truth, parentsTruth);
                }
            }

        }

        for (AbstractCorrelationTreeNode node : nodeList) {
            node.computeCovariance();
        }

    }

    private double[] getParentPredictions(AbstractCorrelationTreeNode[] parents, double[] predictedFP){
        double[] parentPlatt = new double[parents.length];
        for (int i = 0; i < parents.length; i++) {
            final AbstractCorrelationTreeNode parent = parents[i];
            final double platt = laplaceSmoothing(predictedFP[parent.getFingerprintIndex()], alpha);
            parentPlatt[i] = platt;
        }
        return parentPlatt;
    }

    private boolean[] getParentTruth(AbstractCorrelationTreeNode[] parents, boolean[] trueFP){
        boolean[] parentTruth = new boolean[parents.length];
        for (int i = 0; i < parents.length; i++) {
            final AbstractCorrelationTreeNode parent = parents[i];
            parentTruth[i] = trueFP[parent.getFingerprintIndex()];
        }
        return parentTruth;
    }

    public int getNumberOfRoots(){
        return forests.length;
    }

    private static Pattern EdgePattern = Pattern.compile("(\\d+)\\s*->\\s*(\\d+)\\s*");


    /*
    parse the molecular property tree from a dot-like file
     */
    private TIntObjectHashMap<AbstractCorrelationTreeNode> parseTreeFile(Path dotFile, FingerprintVersion fpVersion) throws IOException {
        return parseTree(parseTreeFromDotFile(dotFile), fpVersion);
    }

    public static int[][] parseTreeFromDotFile(Path dotFile) throws IOException {
        List<int[]> edges = new ArrayList<>();
        try (final BufferedReader br = Files.newBufferedReader(dotFile, Charset.forName("UTF-8"))) {
            String line;
            while ((line=br.readLine())!=null) {
                final Matcher m = EdgePattern.matcher(line);
                if (m.find()) {
                    final int u = Integer.parseInt(m.group(1));
                    final int v = Integer.parseInt(m.group(2));
                    edges.add(new int[]{u,v});
                }
            }
        }
        return edges.toArray(new int[0][]);
    }

    /*
    parse molecular property net from a array of edges.
    [[a,b],...,[x,y,z]] contains edges a->b, ..., x->y,x->z
    absolute indices!
    try to correct for missing and unused properties
    important: all parents or a node must be contained in one such int[], last elements always represent the child node.
    Each node (index) can be last element only once.
     */
    private TIntObjectHashMap<AbstractCorrelationTreeNode> parseTree(int[][] absIndices, FingerprintVersion fpVersion){
        TIntObjectHashMap<AbstractCorrelationTreeNode> nodes = new TIntObjectHashMap<>();
        for (int[] absIndex : absIndices) {
            final int u = absIndex[0];

            AbstractCorrelationTreeNode[] parentNodes = new AbstractCorrelationTreeNode[absIndex.length-1];
            for (int i = 0; i < absIndex.length-1; i++) {
                parentNodes[i] = nodes.get(i);
            }
            AbstractCorrelationTreeNode currentNode = createTreeNode(u, parentNodes);
            nodes.put(u, currentNode);
            for (AbstractCorrelationTreeNode parentNode : parentNodes) {
                parentNode.getChildren().add(currentNode);
            }
        }

        for (int i : nodes.keys()) {
            if (!fpVersion.hasProperty(i)) throw new RuntimeException("tree contains properties which are not used for fingerprints");
        }


        // convert to relative indices
        TIntObjectHashMap<AbstractCorrelationTreeNode> nodesRelativeIdx = new TIntObjectHashMap<>();
        for (int i : nodes.keys()) {
            int relIdx = fpVersion.getRelativeIndexOf(i);
            AbstractCorrelationTreeNode node = nodes.get(i);
            node.setFingerprintIndex(relIdx);
            nodesRelativeIdx.put(relIdx, node);
        }

        //add properties not contained in tree
        for (int i = 0; i < fpVersion.size(); i++) {
            if (!nodesRelativeIdx.contains(i)){
                throw new RuntimeException("Property " + fpVersion.getAbsoluteIndexOf(i) + " is not contained in tree");
//                nodesRelativeIdx.put(i, createTreeNode(i));
            }
        }

        return nodesRelativeIdx;
    }

    protected AbstractCorrelationTreeNode createTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode... parentNodes){
        if (parentNodes.length==1){
            return new CorrelationTreeNode(fingerprintIndex, parentNodes[0]);
        } else if (parentNodes.length==2) {
            return new TwoParentsCorrelationTreeNode(fingerprintIndex, parentNodes);
        } else {
            throw new RuntimeException("don't support nodes with no or more than 2 parents");
        }
    }

    private static int RootT=0,RootF=1,ChildT=0,ChildF=1;

    /**
     * important: order of parent and child changed!!!!!!
     */
    protected abstract class AbstractCorrelationTreeNode{

        abstract protected void initPlattByRef();

        /*
        get bin index of this to put platt value for this property
         */
        abstract int getIdxThisPlatt(boolean thisTrue, boolean... parentsTrue);

        /*
        get bin index of a parent property to put platt value. This is the index in getParents() array
         */
        abstract int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue);


        /*
        add the predicted platt of the property. Include the information of this and the parents true values.
         */
        void addPlattThis(double platt, boolean thisTrue, boolean... parentsTrue) {
            addPlatt(getIdxThisPlatt(thisTrue, parentsTrue), platt);
        }

        void addPlattOfParent(double platt, int parentIdx, boolean thisTrue, boolean... parentsTrue) {
            addPlatt(getIdxRootPlatt(true, parentIdx, parentsTrue), platt);
        }

        abstract protected void  addPlatt(int bin, double platt);


        abstract void computeCovariance();

        abstract void setCovariance(double[] covariances);

        abstract protected double getCovariance(int whichCovariance, boolean real, boolean... realParents);

        abstract protected double[] getCovarianceArray();

        abstract AbstractCorrelationTreeNode[] getParents();

        abstract int numberOfParents();

        abstract List<AbstractCorrelationTreeNode> getChildren();

        abstract int getFingerprintIndex();

        abstract void setFingerprintIndex(int newIdx);

        abstract int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue);

    }

    protected class TwoParentsCorrelationTreeNode extends AbstractCorrelationTreeNode{
        protected AbstractCorrelationTreeNode[] parents;
        protected List<AbstractCorrelationTreeNode> children;
        protected int fingerprintIndex;

        protected double[][] covariances;
        TDoubleArrayList[] plattByRef;

        private int numberOfCombinations;

        public TwoParentsCorrelationTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode... parents) {
            assert parents.length==2;
            this.fingerprintIndex = fingerprintIndex;
            this.parents = parents;
            //number of combinations of child,parent_i,... being 0/1 times number of necessary correlations (pairwise and more)
            this.numberOfCombinations = (int)Math.pow(2,parents.length+1);
            this.covariances = new double[numberOfCombinations][numberOfCombinations-1-(parents.length-1)];
            this.children = new ArrayList<>();
            initPlattByRef();
        }

        protected void initPlattByRef() {
            plattByRef = new TDoubleArrayList[(parents.length + 1) * numberOfCombinations];
            for (int j = 0; j < plattByRef.length; j++) {
                plattByRef[j] = new TDoubleArrayList();
            }

            //add 'pseudo counts'
            for (int j = 0; j < numberOfCombinations; j++) {

                //000
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(0d);
                //
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(1d);


            }

        }

        @Override
        int getIdxThisPlatt(boolean thisTrue, boolean... parentsTrue) {
            int idx = 3*getArrayIdxForGivenAssignment(thisTrue, parentsTrue);
            return idx;
        }

        @Override
        int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue) {
            final int idx = getIdxThisPlatt(thisTrue, parentsTrue);
            return idx+parentIdx+1;
        }

        @Override
        protected void addPlatt(int bin, double platt) {
            plattByRef[bin].add(platt);
        }


        @Override
        int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue){
            assert parentsTrue.length==2;
            return ((thisTrue ? 1 : 0))+(parentsTrue[0] ? 2 : 0)+(parentsTrue[1] ? 4 : 0);
        }

        /*
        get idx of the covariance you like to have: e.g. c_ij -> (thisTrue, parent0True, parent1False) or c_ijk -> (thisTrue, parent0True, parent1True)
         */
        int getCovIdx(boolean thisContained, boolean... parentsContained){
            int idx = 8-getArrayIdxForGivenAssignment(thisContained, parentsContained); //reverse as their is no set of single elements or empty set
            return idx;
        }



        @Override
        void computeCovariance() {
            //indices of the specific cov in array
            int covIndex_ij = getCovIdx(true, true, false);
            int covIndex_ik = getCovIdx(true, false, true);
            int covIndex_jk = getCovIdx(false, true, true);
            int covIndex_ijk = getCovIdx(true, true, true);


            for (int l = 0; l < 2; l++) {
                boolean thisTrue = (l==1);
                for (int m = 0; m < 2; m++) {
                    boolean parent0True = (m==1);
                    for (int n = 0; n < 2; n++) {
                        boolean parent1True = (n==1);

                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ij] =
                                Statistics.covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray()
                                        );

                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ik] =
                                Statistics.covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_jk] =
                                Statistics.covariance(
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ijk] =
                                covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                    }
                }
            }

            initPlattByRef();//remove oldPlatt
        }

        private double covariance(double[] a, double[] b, double[] c){
            throw new NoSuchMethodError("not implemented");
        }

        @Override
        void setCovariance(double[] covariances) {
            //convert to 2D
            int secondDimSize = numberOfCombinations-1-(parents.length-1);

            int i=0, j=0;
            for (double covariance : covariances) {
                this.covariances[i][j] = covariance;
                if (j==secondDimSize-1){
                    ++i;
                    j=0;
                } else {
                    ++j;
                }
            }
            initPlattByRef();
        }

        @Override
        protected double getCovariance(int whichCovariance, boolean real, boolean... realParents) {
            return covariances[getCovIdx(real, realParents)][whichCovariance];
        }

        @Override
        protected double[] getCovarianceArray() {
            //convert
            double[] covariances1D = new double[numberOfCombinations*numberOfCombinations-1-(parents.length-1)];
            int i = 0;
            for (int j = 0; j < covariances.length; j++) {
                double[] cov = covariances[j];
                for (int k = 0; k < cov.length; k++) {
                    covariances1D[i++] = cov[k];
                }
            }
            return covariances1D;
        }

        @Override
        AbstractCorrelationTreeNode[] getParents() {
            return parents;
        }

        @Override
        int numberOfParents() {
            return parents.length;
        }

        @Override
        List<AbstractCorrelationTreeNode> getChildren() {
            return children;
        }

        @Override
        int getFingerprintIndex() {
            return fingerprintIndex;
        }

        @Override
        void setFingerprintIndex(int newIdx) {
            fingerprintIndex = newIdx;
        }

    }

    protected class CorrelationTreeNode extends AbstractCorrelationTreeNode{
        protected AbstractCorrelationTreeNode parent;
        protected List<AbstractCorrelationTreeNode> children;
        protected int fingerprintIndex;

        protected double[] covariances;
        TDoubleArrayList[] plattByRef;

        public CorrelationTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode parent){
            this.fingerprintIndex = fingerprintIndex;
            this.parent = parent;
            this.covariances = new double[4];
            this.children = new ArrayList<>();
            initPlattByRef();
        }

        protected void initPlattByRef(){
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

        int getIdxThisPlatt(boolean thisTrue, boolean... rootTrue){
            int idx = 2*getArrayIdxForGivenAssignment(thisTrue, rootTrue);
            return idx;
        }

        @Override
        int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue) {
            assert parentIdx==0;
            final int idx = getIdxThisPlatt(thisTrue, parentsTrue);
            return idx+parentIdx+1;
        }

        int getIdxRootPlatt(boolean thisTrue, boolean... parentsTrue) {
            return getIdxRootPlatt(thisTrue, 0, parentsTrue);
        }

        @Override
        protected void addPlatt(int bin, double platt) {
            plattByRef[bin].add(platt);
        }

        void computeCovariance(){
            covariances[getArrayIdxForGivenAssignment(true, true)] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, true)].toArray(), plattByRef[getIdxRootPlatt(true, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, true)] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, true)].toArray(), plattByRef[getIdxRootPlatt(false, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(true, false)] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, false)].toArray(), plattByRef[getIdxRootPlatt(true, false)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, false)] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, false)].toArray(), plattByRef[getIdxRootPlatt(false, false)].toArray());
            initPlattByRef();//remove oldPlatt
        }

        void setCovariance(double[] covariances){
            this.covariances = covariances;
            this.initPlattByRef();//remove oldPlatt
        }

        @Override
        protected double getCovariance(int whichCovariance, boolean real, boolean... realParent){
            assert realParent.length==1;
            return this.covariances[getArrayIdxForGivenAssignment(real, realParent)];
        }

        @Override
        protected double[] getCovarianceArray() {
            return covariances;
        }

        @Override
        AbstractCorrelationTreeNode[] getParents() {
            return new AbstractCorrelationTreeNode[]{parent};
        }

        @Override
        int numberOfParents() {
            return (parent==null?0:1);
        }

        @Override
        List<AbstractCorrelationTreeNode> getChildren() {
            return children;
        }

        @Override
        int getFingerprintIndex() {
            return fingerprintIndex;
        }

        @Override
        void setFingerprintIndex(int newIdx) {
            fingerprintIndex = newIdx;
        }

        @Override
        int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue) {
            assert parentsTrue.length==1;
            return ((thisTrue ? 1 : 0))+(parentsTrue[0] ? 2 : 0);
        }
    }



    public FingerblastScoring getScoring() {
        return new Scorer();
    }

    public FingerblastScoring getScoring(PredictionPerformance[] performances) {
        return new Scorer();
    }


    public class Scorer  implements FingerblastScoring {
        protected double[][] abcdMatrixByNodeIdxAndCandidateProperties;


//        protected int getIndex(boolean thisTrue, boolean rootTrue){
//            return (rootTrue ? 1 : 0)+((thisTrue ? 2 : 0));
//        }

        /*
        returns the one interesting field of the computed contingency table
         */
        double getABCDMatrixEntry(AbstractCorrelationTreeNode v, boolean thisTrue, boolean... parentsTrue){
            return abcdMatrixByNodeIdxAndCandidateProperties[v.getFingerprintIndex()][v.getArrayIdxForGivenAssignment(thisTrue, parentsTrue)];
        }


        private double threshold, minSamples;

        public Scorer(){
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
            abcdMatrixByNodeIdxAndCandidateProperties = new double[nodeList.length][];

            double[] fp = fingerprint.toProbabilityArray();

            for (AbstractCorrelationTreeNode root : forests) {

                // process conditional probabilities
                for (AbstractCorrelationTreeNode child : root.getChildren()) {
                    prepare(child, fp);
                }
            }
        }

        void prepare(AbstractCorrelationTreeNode x, double[] fingerprint){
            if (x instanceof CorrelationTreeNode){
                CorrelationTreeNode v = (CorrelationTreeNode)x;
                final AbstractCorrelationTreeNode u = v.parent;
                final int i = u.getFingerprintIndex();
                final int j = v.getFingerprintIndex();
                final double p_i = laplaceSmoothing(fingerprint[i], alpha);
                final double p_j = laplaceSmoothing(fingerprint[j], alpha);


                double[] necessaryABCDs = new double[4];

                for (int k = 0; k < 2; k++) {
                    boolean parentTrue = (k==0);
                    for (int l = 0; l < 2; l++) {
                        boolean childTrue = (l==0);

                        final double covariance = v.getCovariance(0, childTrue, parentTrue);
                        double[] abcd = computeABCD(covariance, p_i, p_j);
                        necessaryABCDs[v.getArrayIdxForGivenAssignment(childTrue, parentTrue)] = abcd[(parentTrue ? 1 : 0)+((childTrue ? 2 : 0))];

                    }

                }

                abcdMatrixByNodeIdxAndCandidateProperties[j] = necessaryABCDs;

                for (AbstractCorrelationTreeNode child : v.children) {
                    prepare(child, fingerprint);
                }
            } else {
                TwoParentsCorrelationTreeNode v = (TwoParentsCorrelationTreeNode)x;
                final AbstractCorrelationTreeNode[] parents = v.getParents();
                final int i = v.getFingerprintIndex();
                final int j = parents[0].getFingerprintIndex();
                final int k = parents[1].getFingerprintIndex();
                final double p_i = laplaceSmoothing(fingerprint[i], alpha);
                final double p_j = laplaceSmoothing(fingerprint[j], alpha);
                final double p_k = laplaceSmoothing(fingerprint[k], alpha);

                double[] necessaryABCDs = new double[4];

                //indices of the specific cov in array
                int covIndex_ij = v.getCovIdx(true, true, false);
                int covIndex_ik = v.getCovIdx(true, false, true);
                int covIndex_jk = v.getCovIdx(false, true, true);
                int covIndex_ijk = v.getCovIdx(true, true, true);


                double[] necessaryContingencyEntries = new double[8];
                for (int l = 0; l < 2; l++) {
                    boolean thisTrue = (l==1);
                    for (int m = 0; m < 2; m++) {
                        boolean parent0True = (m==1);
                        for (int n = 0; n < 2; n++) {
                            boolean parent1True = (n==1);

                            double cov_ij = v.getCovariance(covIndex_ij, thisTrue, parent0True, parent1True);
                            double cov_ik = v.getCovariance(covIndex_ik, thisTrue, parent0True, parent1True);
                            double cov_jk = v.getCovariance(covIndex_jk, thisTrue, parent0True, parent1True);
                            double cov_ijk = v.getCovariance(covIndex_ijk, thisTrue, parent0True, parent1True);

                            double[][][] q = computeContingencyTable(p_i, p_j, p_k, cov_ij, cov_ik, cov_jk, cov_ijk);

                            necessaryContingencyEntries[v.getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)] = q[l][m][n];
                        }
                    }
                }

                abcdMatrixByNodeIdxAndCandidateProperties[j] = necessaryABCDs;

                for (AbstractCorrelationTreeNode child : v.children) {
                    prepare(child, fingerprint);
                }
            }

        }

        @Override
        public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
            double logProbability = 0d;

            double[] fp = fingerprint.toProbabilityArray();
            boolean[] bool = databaseEntry.toBooleanArray();
            for (AbstractCorrelationTreeNode root : forests) {
                final int i = root.getFingerprintIndex();
                final double prediction = laplaceSmoothing(fp[i],alpha);
                final double oneMinusPrediction = 1d-prediction;
                final boolean real = bool[i];

                if (real){
                    logProbability += Math.log(prediction);
                } else {
                    logProbability += Math.log(oneMinusPrediction);
                }

                // process conditional probabilities
                for (AbstractCorrelationTreeNode child : root.getChildren()) {
                    logProbability += conditional(fp, bool, child);
                }
            }

            return logProbability;
        }


        protected double conditional(double[] fingerprint, boolean[] databaseEntry, AbstractCorrelationTreeNode x) {
            double score;
            if (x instanceof CorrelationTreeNode){
                CorrelationTreeNode v = (CorrelationTreeNode)x;
                final AbstractCorrelationTreeNode u = v.parent;
                final int i = u.getFingerprintIndex();
                final int j = v.getFingerprintIndex();
                final double p_i = laplaceSmoothing(fingerprint[i], alpha);
                final boolean real = databaseEntry[j];
                final boolean realParent = databaseEntry[i];

                double correspondingEntry = getABCDMatrixEntry(v, real, realParent);


                if (realParent){
                    score = Math.log(correspondingEntry/p_i);
                } else {
                    score = Math.log(correspondingEntry/(1-p_i));
                }


                if (Double.isNaN(score) || Double.isInfinite(score)){
                    System.err.println("NaN score for the following fingerprints:");
                    System.err.println(Arrays.toString(fingerprint));
                    System.err.println(Arrays.toString(databaseEntry));
                    System.err.println("for tree node u (" + u.getFingerprintIndex() + ") -> v (" + v.getFingerprintIndex() + ")");
                    System.err.println("with covariance:");
                    System.err.println(Arrays.toString(v.covariances));
//                System.err.printf(Locale.US, "and a = %f, b = %f, c = %f, d = %f\n", a, b, c, d);
                    System.err.printf(Locale.US, "p_i = %f\n", p_i);
                    System.err.printf(Locale.US, "alpha = %f\n", alpha);
                    throw new RuntimeException("bad score: "+score);
                }

                for (AbstractCorrelationTreeNode child : v.children) {
                    score += conditional(fingerprint,databaseEntry, child);
                }
            } else {
                TwoParentsCorrelationTreeNode v = (TwoParentsCorrelationTreeNode)x;
                final AbstractCorrelationTreeNode[] parents = v.getParents();
                final int i = v.getFingerprintIndex();
                final int j = parents[0].getFingerprintIndex();
                final int k = parents[1].getFingerprintIndex();
                final double p_i = laplaceSmoothing(fingerprint[i], alpha);
                final double p_j = laplaceSmoothing(fingerprint[j], alpha);
                final double p_k = laplaceSmoothing(fingerprint[k], alpha);

                final boolean real = databaseEntry[i];
                final boolean realParent0 = databaseEntry[j];
                final boolean realParent1 = databaseEntry[k];


                double correspondingEntry = getABCDMatrixEntry(v, real, realParent0, realParent1);

                double parentScores = Math.log((realParent0 ? p_j : 1-p_j))+Math.log((realParent1 ? p_k : 1-p_k));

                score = correspondingEntry-parentScores;


                if (Double.isNaN(score) || Double.isInfinite(score)){
                    System.err.println("NaN score for the following fingerprints:");
                    System.err.println(Arrays.toString(fingerprint));
                    System.err.println(Arrays.toString(databaseEntry));
                    System.err.println("for tree node u (" + parents[0].getFingerprintIndex() + ", " + parents[1].getFingerprintIndex() + ") -> v (" + v.getFingerprintIndex() + ")");
                    System.err.println("with covariances: ");
                    for (double[] row : v.covariances) {
                        System.err.println("\t" + Arrays.toString(row));
                    }
//                System.err.printf(Locale.US, "and a = %f, b = %f, c = %f, d = %f\n", a, b, c, d);
                    System.err.printf(Locale.US, "p_i = %f\n", p_i);
                    System.err.printf(Locale.US, "alpha = %f\n", alpha);
                    throw new RuntimeException("bad score: "+score);
                }

                for (AbstractCorrelationTreeNode child : v.children) {
                    score += conditional(fingerprint,databaseEntry, child);
                }

            }


            return score;
        }

        protected double[] computeABCD(double covariance, double p_i, double p_j) {
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

        protected double[][][] computeContingencyTable(double p_i, double p_j, double p_k, double cov_ij, double cov_ik, double cov_jk, double cov_ijk) {
            final double pipj = p_i*p_j;
            final double pipk = p_i*p_k;
            final double pjpk = p_j*p_k;
            final double pipjpk = pipj*p_k;


            double q111_soft = Math.max(cov_ij+pipj+cov_ik+pipk+cov_jk+pjpk-p_i-p_j-p_k,
                    Math.min(1+cov_ij+pipj+cov_ik+pipk+cov_jk+pjpk-p_i-p_j-p_k,
                            cov_ijk+p_k*cov_ij+p_j*cov_ik+p_i*cov_jk+pipjpk
                    )
            );

            double q111 = Math.max(0, Math.min(p_i, Math.min(p_j, Math.min(p_k,q111_soft))));

            double x_soft = cov_ij + pipj + cov_ik + pipk - 2*q111;
            double y_soft = cov_ij + pipj + cov_jk + pipk - 2*q111;
            double z_soft = cov_ik + pipk + cov_jk + pjpk - 2*q111;

            //now iterate and find good multiplier to fullfill constraints
            double[] xyz = new double[]{x_soft,y_soft,z_soft};
            boolean[] leaveOut_xyz = new boolean[]{false, false, false};
            if (!satisfyConstraints(xyz, leaveOut_xyz, p_i, p_j, p_k, q111, true)){
                new RuntimeException("we got a problem. constraints not satisfiable");
            }

            double[][][] q = new double[2][2][2];
            q[1][1][1] = q111;
            
            
            q[1][1][0] = (cov_ij+pipj-q111) / x_soft * xyz[0] + (cov_ij+pipj-q111) / y_soft * xyz[1];
            q[1][0][1] = (cov_ik+pipk-q111) / x_soft * xyz[0] + (cov_ik+pipk-q111) / z_soft * xyz[2];
            q[0][1][1] = (cov_jk+pjpk-q111) / y_soft * xyz[1] + (cov_jk+pjpk-q111) / z_soft * xyz[2];

            q[1][0][0] = p_i - (q[1][1][1]+q[1][1][0]+q[1][0][1]);
            q[0][1][0] = p_j - (q[1][1][1]+q[1][1][0]+q[0][1][1]);
            q[0][0][1] = p_k - (q[1][1][1]+q[1][0][1]+q[0][1][1]);
            q[0][0][0] = 1 - (q[1][1][1]+q[1][1][0]+q[1][0][1]+q[1][0][0]+q[0][1][1]+q[0][1][0]+q[0][0][1]);


            addPseudoAndRenormalize(q);
            return q;
        }
   
   
    }


    private void addPseudoAndRenormalize(double[][][] q){
        //todo this only works if already sum 1; rather normalize completely?
        double pseudoCount = alpha;
        int numOfEntries = q.length*q[0].length*q[0][0].length;
        double norm = 1d+numOfEntries*pseudoCount;
        for (int i = 0; i < q.length; i++) {
            final double[][] level2 = q[i];
            for (int j = 0; j < level2.length; j++) {
                final double[] level3 = level2[j];
                for (int k = 0; k < level3.length; k++) {
                    level3[k] = (level3[k]+pseudoCount)/norm;
                }
            }

        }
    }
    
        
    private boolean satisfyConstraints(double[] xyz, boolean[] leaveOut_xyz, double p_i, double p_j, double p_k, double q111, boolean firstRound){
        boolean x_violated = false, y_violated = false, z_violated = false;
        //hard constraints
        double[] new_value = new double[1];
        if (!leaveOut_xyz[0]) {
            if (!satisfiesHardConstraint(xyz[0], p_i, q111, new_value)) {
                xyz[0] = new_value[0];
                x_violated = true;
            }
        }
        if (!leaveOut_xyz[1]) {
            if (!satisfiesHardConstraint(xyz[1], p_j, q111, new_value)) {
                xyz[1] = new_value[0];
                y_violated = true;
            }
        }
        if (!leaveOut_xyz[2]) {
            if (!satisfiesHardConstraint(xyz[2], p_k, q111, new_value)) {
                xyz[2] = new_value[0];
                z_violated = true;
            }
        }

       if (!firstRound){
           leaveOut_xyz[0] = leaveOut_xyz[0] || x_violated;
           leaveOut_xyz[1] = leaveOut_xyz[1] || y_violated;
           leaveOut_xyz[2] = leaveOut_xyz[2] || z_violated;
       }

        double sum_xyz = sum(xyz);
        double twice_pi_pj_pk_2q111 = 2*(p_i+p_j+p_k+2*q111);
        double multiplier = 1.0;
        if (sum_xyz > twice_pi_pj_pk_2q111){
            if (!any(leaveOut_xyz)) return false; //didn't satisfy constraints
            multiplier = findMultiplier(xyz, leaveOut_xyz, twice_pi_pj_pk_2q111, sum_xyz);
        } else if (sum_xyz < twice_pi_pj_pk_2q111-2){
            if (!any(leaveOut_xyz)) return false; //didn't satisfy constraints
            multiplier = findMultiplier(xyz, leaveOut_xyz, twice_pi_pj_pk_2q111-2, sum_xyz);
        }
        if (multiplier!=1.0) {
            //todo test inf of zero?
            for (int i = 0; i < xyz.length; i++) {
                if (!leaveOut_xyz[i]) xyz[i] = multiplier*xyz[i];
            }
            return satisfyConstraints(xyz, leaveOut_xyz, p_i, p_j, p_k, q111, false);

        } else {
            //satisfied
            return true;
        }
    }

    private double findMultiplier(double[] xyz, boolean[] leaveOut_xyz, double bound, double sum_xyz) {
        double fixedValue = 0;
        for (int i = 0; i < xyz.length; i++) {
            if (leaveOut_xyz[i]) fixedValue += xyz[i];
        }
        return  (bound-fixedValue)/(sum_xyz-fixedValue);
    }

    private  double sum(double[] arr){
        double s = 0;
        for (double v : arr) {
            s += v;
        }
        return s;
    }

    private boolean any(boolean[] arr){
        for (boolean b : arr) {
            if (b) return true;
        }
        return false;
    }

    private boolean satisfiesHardConstraint(double w, double p, double q, double[] new_w) {
        if (w > p - q){
            new_w[0] = p - q;
            return false;
        } else if (w < 0){
            new_w[0] = 0;
            return false;
        }
        return true;
    }

    protected static double laplaceSmoothing(double probability, double alpha) {
        return (probability + alpha) / (1d + 2d * alpha);
    }
}
