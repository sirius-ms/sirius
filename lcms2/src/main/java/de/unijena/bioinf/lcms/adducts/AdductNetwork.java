package de.unijena.bioinf.lcms.adducts;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.util.*;
import java.util.stream.Collectors;

public class AdductNetwork {

    protected AdductNode[] rtOrderedNodes;
    protected AdductManager adductManager;

    protected Deviation deviation;

    ProjectSpaceTraceProvider provider;

    public AdductNetwork(ProjectSpaceTraceProvider provider, AlignedFeatures[] features, AdductManager manager) {
        this.rtOrderedNodes = new AdductNode[features.length];
        for (int k=0; k < features.length; ++k) {
            rtOrderedNodes[k] = new AdductNode(features[k], k);
        }
        Arrays.sort(rtOrderedNodes, Comparator.comparingDouble(AdductNode::getRetentionTime));
        adductManager = manager;
        this.provider = provider;
        deviation = new Deviation(10);
        buildNetworkFromMassDeltas();
    }

    protected void buildNetworkFromMassDeltas() {
        Scorer scorer = new Scorer();
        for (int l=0; l < rtOrderedNodes.length; ++l) {
            final AdductNode leftNode = rtOrderedNodes[l];
            final double thresholdLeft = rtOrderedNodes[l].getFeature().getRetentionTime().getStartTime();
            final double thresholdRight = rtOrderedNodes[l].getFeature().getRetentionTime().getEndTime();
            final Range<Double> threshold = Range.closed(thresholdLeft, thresholdRight);
            /*
            int rLeft=l-1;
            for (; rLeft >= 0; --rLeft) {
                if (!threshold.contains(rtOrderedNodes[rLeft].getRetentionTime() )  ) {
                    break;
                }
            }
            ++rLeft;
            */
            int rLeft=l+1;
            int rRight=l+1;
            for (; rRight < rtOrderedNodes.length; ++rRight) {
                if (!threshold.contains(rtOrderedNodes[rRight].getRetentionTime() )  ) {
                    break;
                }
            }
            --rRight;

            for (int i=rLeft; i <= rRight; ++i) {
                if (i != l) {
                    final AdductNode rightNode = rtOrderedNodes[i];
                    if (rightNode.features.getRetentionTime().asRange().contains(leftNode.getRetentionTime())) {
                        final double massDelta = rightNode.getMass() - leftNode.getMass();
                        List<KnownMassDelta> knownMassDeltas = adductManager.retrieveMassDeltas(massDelta, deviation);
                        if (!knownMassDeltas.isEmpty()) {
                            final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, knownMassDeltas.toArray(KnownMassDelta[]::new));
                            scorer.computeScore(provider, adductEdge);
                            if (Double.isFinite(adductEdge.ratioScore)) {
                                addEdge(adductEdge);
                                System.out.printf("%.3f (%d sec) ---> %.3f (%d sec) || ratio=%.3f, cor=%.3f %s\n", leftNode.getMass(), (int) leftNode.getRetentionTime(),
                                        rightNode.getMass(), (int) rightNode.getRetentionTime(), adductEdge.ratioScore, adductEdge.correlationScore, knownMassDeltas.stream().map(Object::toString).collect(Collectors.joining(", ")));
                            }
                        }
                    }
                }
            }
        }

        BitSet visited = new BitSet(rtOrderedNodes.length);
        List<List<AdductNode>> subgraphs = new ArrayList<>();
        List<AdductNode> singletons = new ArrayList<>();
        for (int k=0; k < rtOrderedNodes.length; ++k) {
            if (!visited.get(rtOrderedNodes[k].index)) {
                List<AdductNode> nodes = spread(rtOrderedNodes[k], visited);
                if (nodes.size()>1) subgraphs.add(nodes);
                else singletons.add(rtOrderedNodes[k]);
            }
        }
        FloatArrayList sizes = new FloatArrayList();
        for (List<AdductNode> subg : subgraphs) {
            Set<AdductEdge> edges = new HashSet<>();
            for (AdductNode n : subg) edges.addAll(n.edges);
            int adductEdges=0;
            for (AdductEdge e : edges) {
                if (Double.isFinite(e.ratioScore)) {
                    if (Arrays.stream(e.explanations).anyMatch(x->x instanceof AdductRelationship)) {
                        ++adductEdges;
                    }
                }
            }
            if (adductEdges>0) {
                System.out.println("--------------------------");
                sizes.add(edges.size());
                for (AdductEdge e : edges) {
                    if (Double.isFinite(e.ratioScore)) {
                        System.out.printf("%.3f (%d sec) ---> %.3f (%d sec) || ratio=%.3f, corr=%.3f %s\n", e.left.getMass(), (int) e.left.getRetentionTime(),
                                e.right.getMass(), (int) e.right.getRetentionTime(), e.ratioScore, e.correlationScore, Arrays.stream(e.explanations).map(Object::toString).collect(Collectors.joining(", ")));
                    }
                }
            }
        }

        System.out.println("average network size: " + sizes.doubleStream().average().getAsDouble());
        System.out.println("--------------------------");
        System.out.printf("%d subnetworks and %d singletons out of %d total features.\n", sizes.size(),
                singletons.size(), rtOrderedNodes.length);

    }

    private void addEdge(AdductEdge edge) {
        edge.left.edges.add(edge);
        edge.right.edges.add(edge);
    }

    private List<AdductNode> spread(AdductNode seed, BitSet colors) {
        ArrayList<AdductNode> stack = new ArrayList<>();
        stack.add(seed);
        colors.set(seed.index);
        int i = 0;
        while (i < stack.size()) {
            AdductNode n = stack.get(i);
            for (AdductNode m : n.getNeighbours()) {
                if (!colors.get(m.index)) {
                    stack.add(m);
                    colors.set(m.index);
                }
            }
            ++i;
        }
        return stack;
    }

    protected AdductNode[] findNodesByRt(double rtFrom, double rtTo) {
        if (rtTo < rtFrom) {
            throw new IllegalArgumentException("Illegal mass range: rtFrom = " + rtFrom +  " and is larger than rtTo = " + rtTo);
        }
        int i = BinarySearch.searchForDoubleByIndex(j-> rtOrderedNodes[j].getRetentionTime(),
                0, rtOrderedNodes.length, rtFrom);
        if (i < 0)  {
            i = -(i + 1);
        }
        if (i >= rtOrderedNodes.length || rtOrderedNodes[i].getMass() > rtTo) return new AdductNode[0];
        final int start = i;
        for (i=start+1; i < rtOrderedNodes.length; ++i) {
            if (rtOrderedNodes[i].getRetentionTime() > rtTo) {
                break;
            }
        }
        final int end = i;
        final AdductNode[] sublist = new AdductNode[start-end];
        System.arraycopy(rtOrderedNodes, start, sublist, 0, end-start);
        return sublist;
    }
}
