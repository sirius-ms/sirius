package de.unijena.bioinf.lcms.adducts;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.*;
import java.util.stream.Collectors;

public class AdductNetwork {

    protected AdductNode[] rtOrderedNodes;
    protected AdductManager adductManager;

    protected Deviation deviation;

    ProjectSpaceTraceProvider provider;

    public AdductNetwork(ProjectSpaceTraceProvider provider, AlignedFeatures[] features, AdductManager manager, double retentionTimeTolerance) {
        this.rtOrderedNodes = new AdductNode[features.length];
        for (int k=0; k < features.length; ++k) {
            rtOrderedNodes[k] = new AdductNode(features[k], k);
        }
        Arrays.sort(rtOrderedNodes, Comparator.comparingDouble(AdductNode::getRetentionTime));
        adductManager = manager;
        this.provider = provider;
        deviation = new Deviation(10);
        buildNetworkFromMassDeltas(retentionTimeTolerance);
    }

    protected void buildNetworkFromMassDeltas(double retentionTimeTolerance) {
        Scorer scorer = new Scorer();
        final PValueStats pValueStats = new PValueStats();
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
                    if (leftNode.getMass() < rightNode.getMass() && Math.abs(leftNode.getRetentionTime() - rightNode.getRetentionTime()) < retentionTimeTolerance &&  rightNode.features.getRetentionTime().asRange().contains(leftNode.getRetentionTime())) {
                        final double massDelta = rightNode.getMass() - leftNode.getMass();
                        List<KnownMassDelta> knownMassDeltas = adductManager.retrieveMassDeltas(massDelta, deviation);
                        if (!knownMassDeltas.isEmpty()) {
                            final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, knownMassDeltas.toArray(KnownMassDelta[]::new));
                            scorer.computeScore(provider, adductEdge);
                            if (Double.isFinite(adductEdge.ratioScore)) {
                                addEdge(adductEdge);
                                System.out.printf("%.3f (%d sec) ---> %.3f (%d sec) || ratio=%.3f, cor=%.3f cor2=%.3f %s\n", leftNode.getMass(), (int) leftNode.getRetentionTime(),
                                        rightNode.getMass(), (int) rightNode.getRetentionTime(), adductEdge.ratioScore, adductEdge.correlationScore, adductEdge.representativeCorrelationScore, knownMassDeltas.stream().map(Object::toString).collect(Collectors.joining(", ")));
                            }
                        } else if (adductManager.hasDecoy(massDelta)) {
                            final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, new KnownMassDelta[0]);
                            scorer.computeScore(provider, adductEdge);
                            pValueStats.add(adductEdge);
                        }
                    }
                }
            }
        }

        pValueStats.done();

        deleteEdgesWithLowPvalue(pValueStats, 5);

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
            int adductEdges=0;
            for (AdductNode n : subg) {
                edges.addAll(n.edges);
                Set<KnownAdductType> ionTypes = new HashSet<>(Arrays.asList(n.ionTypes));
                for (AdductEdge e : n.edges) {
                    for (KnownMassDelta t : e.explanations) {
                        if (t instanceof AdductRelationship) {
                            if (e.left==n) ionTypes.add(((AdductRelationship)t).left);
                            if (e.right==n) ionTypes.add(((AdductRelationship)t).right);
                            ++adductEdges;
                        }
                    }
                }
                n.ionTypes = ionTypes.toArray(KnownAdductType[]::new);
            }
            // resolve other edges
            for (AdductNode n : subg) {
                Set<KnownAdductType> ionTypes = new HashSet<>(Arrays.asList(n.ionTypes));
                for (AdductEdge e : n.edges) {
                    for (KnownMassDelta t : e.explanations) {
                        if (t instanceof LossRelationship) {
                            if (e.left==n) ionTypes.addAll(Arrays.asList(e.right.ionTypes));
                            if (e.right==n) ionTypes.addAll(Arrays.asList(e.left.ionTypes));
                        }
                    }
                }
                n.ionTypes = ionTypes.toArray(KnownAdductType[]::new);
            }
            if (adductEdges>0) {
                System.out.println("--------------------------");
                sizes.add(edges.size());
                for (AdductEdge e : edges) {
                    System.out.printf("%.3f (%d sec) ---> %.3f (%d sec) || pvalue = %.3f ratio=%.3f, corr=%.3f corr2=%.3f %s\n", e.left.getMass(), (int) e.left.getRetentionTime(),
                                e.right.getMass(), (int) e.right.getRetentionTime(), pValueStats.logPvalue(e), e.ratioScore, e.correlationScore, e.representativeCorrelationScore, Arrays.stream(e.explanations).map(Object::toString).collect(Collectors.joining(", ")));

                    if (e.correlationScore < -0.8 || e.representativeCorrelationScore < -0.8) {
                        System.err.println("Strange...");
                        scorer.computeScore(provider, e);
                    }
                }
                // check for compatibility
                int incompatibleNodes = 0;
                for (AdductNode n : subg) {
                    if (n.ionTypes.length>2) ++incompatibleNodes;
                    else if (n.ionTypes.length==2 && n.ionTypes[0].toPrecursorIonType().isPresent() && n.ionTypes[0].toPrecursorIonType().get().isIonizationUnknown()){
                        n.selectedIon = 1;
                    } else n.selectedIon=0;
                }
                if (incompatibleNodes==0) {
                    int incompatibleEdges = 0;
                    for (AdductEdge e : edges) {
                        for (KnownMassDelta m : e.explanations) {
                            if (!m.isCompatible(e.left.ionTypes[e.left.selectedIon], e.right.ionTypes[e.right.selectedIon])) {
                                ++incompatibleEdges;
                            }
                        }
                    }
                    if (incompatibleEdges<=0) {
                        System.out.println("Subnetwork can be resolved.");
                    } else {
                        System.out.println(incompatibleEdges + " incompatible edges.");
                    }
                } else {
                    System.out.println(incompatibleNodes + " incompatible nodes.");
                }

            }
        }

        System.out.println("average network size: " + sizes.doubleStream().average().getAsDouble());
        System.out.println("--------------------------");
        System.out.printf("%d subnetworks and %d singletons out of %d total features.\n", sizes.size(),
                singletons.size(), rtOrderedNodes.length);
        System.out.printf("There are %d decoy edges:\n", pValueStats.count);

    }

    private void deleteEdgesWithLowPvalue(PValueStats pValueStats, int threshold) {
        for (AdductNode node : rtOrderedNodes) {
            Iterator<AdductEdge> iterator = node.edges.iterator();
            while (iterator.hasNext()) {
                AdductEdge e = iterator.next();
                if (-pValueStats.logPvalue(e) < threshold) {
                    iterator.remove();
                }
            }
        }
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

    class PValueStats {
        private IntArrayList ratioScore5, cor1, cor2;
        private double[] pvalueRatio, pvalueCor1, pvalueCor2;
        private double pbase;
        private int count;

        public PValueStats() {
            ratioScore5 = new IntArrayList();
            cor1 = new IntArrayList();
            cor2 = new IntArrayList();
            count = 0;
        }

        private void done() {
            pvalueRatio = new double[ratioScore5.size()+1];
            pvalueCor1 = new double[cor1.size()+1];
            pvalueCor2 = new double[cor2.size()+1];

            int cumsum=1;
            double base = Math.log(count+1);
            pbase=base;
            pvalueRatio[pvalueRatio.length-1] = Math.log(cumsum)-base;
            for (int i=ratioScore5.size()-1; i >= 0; --i) {
                cumsum += ratioScore5.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueRatio[i] = logprob;
            }
            cumsum=1;
            pvalueCor1[pvalueCor1.length-1] = Math.log(cumsum)-base;
            for (int i=cor1.size()-1; i >= 0; --i) {
                cumsum += cor1.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueCor1[i] = logprob;
            }
            cumsum=1;
            pvalueCor2[pvalueCor2.length-1] = Math.log(cumsum)-base;
            for (int i=cor2.size()-1; i >= 0; --i) {
                cumsum += cor2.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueCor2[i] = logprob;
            }
        }

        public double logPvalue(AdductEdge edge) {
            double pvalue = 0d;
            if (Double.isFinite(edge.ratioScore)) {
                int b = ratio2bin(edge.ratioScore);
                if (b >= pvalueRatio.length) pvalue += pvalueRatio[pvalueRatio.length-1];
                else pvalue += pvalueRatio[b];
            }
            if (Double.isFinite(edge.correlationScore)) {
                int b = cor2bin(edge.correlationScore);
                if (b >= pvalueCor1.length) pvalue += pvalueCor1[pvalueCor1.length-1];
                else pvalue += pvalueCor1[b];
            }
            if (Double.isFinite(edge.representativeCorrelationScore)) {
                int b = cor2bin(edge.representativeCorrelationScore);
                if (b >= pvalueCor2.length) pvalue += pvalueCor2[pvalueCor2.length-1];
                else pvalue += pvalueCor2[b];
            }
            return pvalue;
        }



        public void add(AdductEdge edge) {
            ++count;
            if (Double.isFinite(edge.ratioScore)) {
                int b = ratio2bin(edge.ratioScore);
                while (b >= ratioScore5.size()) ratioScore5.add(0);
                ratioScore5.set(b, ratioScore5.getInt(b)+1);
            }
            if (Double.isFinite(edge.correlationScore)) {
                int b = cor2bin(edge.correlationScore);
                while (b >= cor1.size()) cor1.add(0);
                cor1.set(b, cor1.getInt(b)+1);
            }
            if (Double.isFinite(edge.representativeCorrelationScore)) {
                int b = cor2bin(edge.representativeCorrelationScore);
                while (b >= cor2.size()) cor2.add(0);
                cor2.set(b, cor2.getInt(b)+1);
            }
        }

        private int ratio2bin(double score) {
            return (int)Math.max(0,Math.round((score+0)/5));
        }
        private int cor2bin(double score) {
            return (int)Math.max(0,Math.round(score*10));
        }
    }

}
