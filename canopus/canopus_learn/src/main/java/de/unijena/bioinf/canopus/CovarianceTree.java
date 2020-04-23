package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class CovarianceTree {

    TShortObjectHashMap<Node> nodes;
    Node root;
    Sampler sampler;

    CovarianceTree(Sampler sampler, File backbone) throws IOException {
        this.nodes = new TShortObjectHashMap<Node>(sampler.version.size(), 0.75f, (short)-1);
        int j=0;
        for (int index : sampler.version.allowedIndizes()) {
            final Node node = new Node((short)index, (short)j, sampler.tps[j], sampler.fps[j], sampler.tns[j], sampler.fns[j], sampler.positives[j], sampler.negatives[j]);
            nodes.put(node.fpIndex, node);
            ++j;
        }
        buildtree(sampler, backbone);
        this.sampler = sampler;
    }

    public double[] draw(final ArrayFingerprint truth) {
        //System.out.println("Sample covariance");
        final Random random = new Random();
        final double[] sampled = new double[nodes.size()];
        return draw(sampled, truth, Double.NaN, root, random);
    }

    private double[] draw(double[] sampled, ArrayFingerprint truth, double previousProb, Node u, Random random) {
        final boolean t = truth.isSet(u.fpIndex);
        final double prob;
        if (u.incomingEdge==null) {
            prob = sampler.draw(t ? u.positives : u.negatives, random);
        } else {
            prob = u.incomingEdge.draw(random,previousProb, t);
        }
        sampled[u.relIndex] = prob;
        for (Node child : u.children) {
            draw(sampled,truth,prob,child,random);
        }
        return sampled;
    }

    private void buildtree(Sampler sampler, File backbone) throws IOException {
        final int n = sampler.trainFps.length;
        final double[][] transposedMatrix = new double[nodes.size()][n];
        final boolean[][] transposedBool = new boolean[nodes.size()][n];
        for (int k=0; k < n; ++k) {
            double[] ary = sampler.trainFps[k].toProbabilityArray();
            for (int i=0; i  <ary.length; ++i) {
                transposedMatrix[i][k] = ary[i];
            }
            for (FPIter f : sampler.perfectFps[k].presentFingerprints()) {
                transposedBool[sampler.version.getRelativeIndexOf(f.getIndex())][k] = true;
            }
        }

        for (String line : FileUtils.readLines(backbone)) {
            String[] pts = line.split("\t", 3);
            final short from = Short.parseShort(pts[0]);
            final short to = Short.parseShort(pts[1]);
            Node u = nodes.get(from);
            Node v = nodes.get(to);
            Edge edge = new Edge(u, v, transposedMatrix[u.relIndex], transposedMatrix[v.relIndex], transposedBool[v.relIndex]);
            u.children.add(v);
            v.incomingEdge = edge;
        }

        // find root
        this.root = null;
        for (Node v : nodes.valueCollection()) {
            if (v.incomingEdge==null) {
                if (root!=null) {
                    System.err.println("Warning. tree has several roots:  " + this.root.fpIndex + " and " + v.fpIndex);
                } else {
                    this.root = v;
                }
            }
        }
        //System.out.println("Root is " + root.fpIndex + " (at position " + root.relIndex + ")");
    }

    protected static class CovTreeAdapter implements TreeAdapter<Node> {

        @Override
        public int getDegreeOf(Node node) {
            return node.children.size();
        }

        @Override
        public List<Node> getChildrenOf(Node node) {
            return node.children;
        }
    }

    protected static class Node {
        private short fpIndex, relIndex;
        private TDoubleArrayList tps, fps, tns, fns, positives, negatives;
        private List<Node> children;
        private Edge incomingEdge;

        public Node(short fpIndex, short relIndex, TDoubleArrayList tps, TDoubleArrayList fps, TDoubleArrayList tns, TDoubleArrayList fns, TDoubleArrayList positives, TDoubleArrayList negatives) {
            this.fpIndex = fpIndex;
            this.relIndex = relIndex;
            this.tps = tps;
            this.fps = fps;
            this.tns = tns;
            this.fns = fns;
            this.positives = positives;
            this.negatives = negatives;
            this.children = new ArrayList<>();
            this.incomingEdge = null;
        }
    }

    //
    // P(x=1
    //
    protected class Edge {
        Node source, target;
        PredictionPerformance positive, negative;

        protected Edge(Node source, Node target, double[] predictionsA, double[] predictionsB, boolean[] truthB) {
            PredictionPerformance.Modify negative = new PredictionPerformance(0,0,0,0,0d,false).modify();
            PredictionPerformance.Modify positive = new PredictionPerformance(0,0,0,0,0d, false).modify();
            for (int k=0; k < predictionsA.length; ++k) {
                positive.update(truthB[k], true, predictionsA[k]*predictionsB[k]);
                positive.update(truthB[k], false, predictionsA[k]*(1d-predictionsB[k]));
                negative.update(truthB[k], true, (1d-predictionsA[k])*predictionsB[k]);
                negative.update(truthB[k], false, (1d-predictionsA[k])*(1d-predictionsB[k]));
            }
            this.positive = positive.done();
            this.negative = negative.done();
            this.source = source;
            this.target = target;
        }

        protected double draw(Random r, double predictionBefore, boolean truth) {

            if (predictionBefore < 0.05)
                predictionBefore = 0d;
            if (predictionBefore > 0.95)
                predictionBefore = 1d;

            if (truth) {
                double tp = predictionBefore*positive.getTpRate() + (1-predictionBefore)*negative.getTpRate();
                double fn = predictionBefore*positive.getFnRate() + (1-predictionBefore)*negative.getFnRate();

                //System.out.println(source.fpIndex + " -> " + target.fpIndex + ",\t" + String.valueOf(truth)+ ", before = " + String.valueOf(predictionBefore) + ",\ttprate = " + positive.getTpRate() + " or " + negative.getTpRate() + ",\ttp = " + tp + ",\tfn = " + fn + ",\t independent tprate = " + sampler.recall[target.relIndex]);

                if (r.nextDouble() <= tp) {
                    return Sampler.draw(target.tps, r);
                } else {
                    return Sampler.draw(target.fps, r);
                }

            } else {
                double tn = predictionBefore*positive.getTnRate() + (1-predictionBefore)*negative.getTnRate();
                double fp = predictionBefore*positive.getFpRate() + (1-predictionBefore)*negative.getFpRate();
                final int J = target.relIndex;
                //System.out.println(source.fpIndex + " -> " + target.fpIndex + ",\t" +String.valueOf(truth)+ ", before = " + String.valueOf(predictionBefore) + ",\ttnrate = " + positive.getTnRate() + " or " + negative.getTnRate() + "\n, tn = " + tn + ",\tfp = " + fp + ",\t independent tnrate = " + (sampler.tns[J].size()/((double)sampler.tns[J].size()+sampler.fps[J].size())));

                if (r.nextDouble() <= tn) {
                    return Sampler.draw(target.tns, r);
                } else {
                    return Sampler.draw(target.fns, r);
                }

            }
        }
    }

}
