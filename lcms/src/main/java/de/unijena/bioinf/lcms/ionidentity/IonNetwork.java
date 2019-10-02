package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IonNetwork {

    protected ArrayList<IonNode> nodes;

    public IonNetwork() {
        this.nodes = new ArrayList<>();
    }

    public void writeToFile(LCMSProccessingInstance i, File file) throws IOException {
        final TObjectIntHashMap node2Id = new TObjectIntHashMap();
        try (final BufferedWriter w = FileUtils.getWriter(file)) {
            w.write("document.data = {\"nodes\": [\n");
            int k = 0;
            for (IonNode node : nodes) {
                node2Id.put(node, ++k);
                final long rrt = Math.round(node.getFeature().getRetentionTime() / 1000d);
                String mass = String.format(Locale.US, "%.3f", node.getFeature().getMass());
                String ret = String.format(Locale.US, "%d min, %d s", (int) (rrt / 60d), (int) (rrt % 60));
                int type = node.getFeature().getFeatures().values().stream().anyMatch(x -> x.getMsMs() != null) ? 1 : 0;
                w.write("\t{\"id\": " + k + ", \"type\": " + type + ", \"types\": " + node.likelyTypesWithProbs() + ", \"name\": \"m/z " + mass + "\", \"mass\": " + mass + ", \"rt\": \"" + ret + "\"},\n");
            }
            w.write("],\n");
            w.write("\"links\": [");
            final ArrayList<Edge> allEdges = new ArrayList<>();
            for (IonNode node : nodes) {
                for (Edge e : node.neighbours) {
                    if (e.deltaMz() >= 0) {
                        String name = e.description();
                        w.write("\t{\"id\":" + allEdges.size() + ",\"source\": " + node2Id.get(e.from) + ", \"target\":" + node2Id.get(e.to) + ", \"type\": \"" + e.type.toString() + "\", \"name\": \"" + name + "\"},\n");
                        allEdges.add(e);
                    }
                }
            }
            w.write("],\n");
            // add ion information
            w.write("\"peaks\": [\n");
            for (int q = 0; q < allEdges.size(); ++q) {
                final Edge e = allEdges.get(q);
                final IonGroup[] ions = e.getMatchingIons2();
                w.write("\t[");
                for (IonGroup g : ions) {
                    w.write("[");
                    for (int p = g.getSegment().getStartIndex(); p < g.getSegment().getEndIndex(); ++p) {
                        w.write("[");
                        w.write(String.valueOf(g.getPeak().getRetentionTimeAt(p)));
                        w.write(",");
                        w.write(String.valueOf(g.getPeak().getIntensityAt(p)));
                        w.write("],");
                    }
                    w.write("],");
                }
                w.write("],");
            }
            w.write("]}");

        }
    }

    public void gibbsSampling(Assignment assignment) {
        final GibbsSampler sampler = new GibbsSampler();
        sampler.learnP(this);
        for (IonNode node : nodes) {
            if (node.assignment == null)
                sampler.sample(node);
        }
        for (IonNode node : nodes) {
            assignment.assignment(node.getFeature(), node.assignment.ionTypes, node.assignment.probabilities);
        }
    }

    public void addNode(AlignedFeatures features) {
        IonNode node = new IonNode(features);
        nodes.add(node);
    }

    public void addCorrelatedEdgesForAllNodes() {
        final IonNode[] todo = nodes.toArray(IonNode[]::new);
        for (IonNode node : todo) {
            final NavigableMap<Double, Link> links = new TreeMap<>();
            final Deviation dev = new Deviation(40);
            Map<ProcessedSample, FragmentedIon> F = node.getFeature().getFeatures();
            for (Map.Entry<ProcessedSample, FragmentedIon> entry : F.entrySet()) {
                for (CorrelatedIon correlatedIon : entry.getValue().getAdducts()) {
                    final double mz = correlatedIon.ion.getMass();
                    // first let us check if we already have seen this mass. If not, add a new link
                    final double delta = dev.absoluteFor(mz);
                    Optional<Link> lookup = links.subMap(mz - delta, mz + delta).values().stream().min(Comparator.comparingDouble(x -> Math.abs(mz - x.mz)));
                    final Link link = lookup.orElseGet(() -> {
                        Link l = new Link(mz);
                        links.put(mz, l);
                        return l;
                    });

                    // associate this correlation with the link
                    link.ions.add(new ImmutablePair<>(entry.getKey(), correlatedIon));

                    // 1. check if we already have a node for this ion
                    if (link.associatedNode == null) {
                        for (IonNode candidate : nodes) {
                            FragmentedIon ion = candidate.getFeature().getFeatures().get(entry.getKey());
                            if (ion!=null && ion.getSegment().samePeak(correlatedIon.ion.getSegment())) {
                                // YES, we can directly associate this ion with the feature
                                link.associatedNode = candidate;
                                break;
                            } else {
                                // NO, we probably have to add this node
                            }
                        }
                    }
                }
            }
            // we now have a link for each adduct type. We now iterate again over all links. If we have an associated
            // node, we can just merge the missing peaks into the feature
            // otherwise, we create a pseudo alignment
            for (Link link : links.values()) {
                if (link.associatedNode == null) {
                    // create a dummy alignment
                    Iterator<ImmutablePair<ProcessedSample, CorrelatedIon>> iter = link.ions.iterator();
                    ImmutablePair<ProcessedSample, CorrelatedIon> init = iter.next();
                    AlignedFeatures dummy = new AlignedFeatures(init.left, new CorAlignedIon(init.right, F.get(init.left), Edge.Type.ADDUCT), init.left.getRecalibratedRT(init.right.ion.getSegment().getApexRt()));
                    while (iter.hasNext()) {
                        init = iter.next();
                        dummy = dummy.merge(new AlignedFeatures(init.left, new CorAlignedIon(init.right, F.get(init.left), Edge.Type.ADDUCT), init.left.getRecalibratedRT(init.right.ion.getSegment().getApexRt())));
                    }
                    IonNode pseudoNode = new IonNode(dummy);
                    nodes.add(pseudoNode);
                    link.associatedNode = pseudoNode;
                } else {
                    final AlignedFeatures adduct = link.associatedNode.getFeature();
                    PrecursorIonType ionTypeLeft = null, ionTypeRight = null;
                    for (ImmutablePair<ProcessedSample, CorrelatedIon> pair : link.ions) {
                        if (adduct.getFeatures().get(pair.left) == null) {
                            adduct.getFeatures().put(pair.left, new CorAlignedIon(pair.right, F.get(pair.left), Edge.Type.ADDUCT));
                        }
                    }
                }
                PrecursorIonType ionTypeLeft = null, ionTypeRight = null;
                for (ImmutablePair<ProcessedSample, CorrelatedIon> pair : link.ions) {
                    if (ionTypeLeft == null) {
                        ionTypeLeft = pair.right.correlation.getLeftType();
                        ionTypeRight = pair.right.correlation.getRightType();
                    } else {
                        if (!ionTypeLeft.equals(pair.right.correlation.getLeftType()) || !ionTypeRight.equals(pair.right.correlation.getRightType())) {
                            LoggerFactory.getLogger(IonNetwork.class).warn("Strange inconsistency in adduct detection: Two different explanations for the same edge: " + ionTypeLeft.toString() + " -> " + ionTypeRight.toString() + " and " + pair.right.correlation.getLeftType() + " -> " + pair.right.correlation.getRightType());
                        }
                    }
                }

                // if we have more than 10 samples, we only add edges if we see a correlation in at least two samples
                if (node.getFeature().getFeatures().keySet().size()<10 || link.associatedNode.getFeature().getFeatures().keySet().size()>1 ) {
                    // now add the edge into the graph
                    final Edge edge = new Edge(node, link.associatedNode, Edge.Type.ADDUCT, ionTypeLeft, ionTypeRight);
                    // for debugging purpose
                    {
                        edge.cor = link.ions.stream().max(Comparator.comparingDouble(i -> i.right.correlation.getNumberOfCorrelatedPeaks())).map(x -> x.right.correlation).orElse(null);
                    }
                    if (node.hasEdge(edge)) {
                        continue;
                    }
                    node.neighbours.add(edge);
                    link.associatedNode.neighbours.add(edge.reverse());
                }
            }
        }

    }

    private static class Link {
        private final double mz;
        private final ArrayList<ImmutablePair<ProcessedSample, CorrelatedIon>> ions;
        private IonNode associatedNode;

        public Link(double mz) {
            this.mz = mz;
            this.ions = new ArrayList<>();
        }
    }

    private AlignedFeatures merge(AlignedFeatures a, AlignedFeatures b) {
        return b.without(a.getFeatures().keySet()).map(x -> a.merge(x)).orElse(a);
    }

    private void addCorrelated(IonNode parentFeature, AlignedFeatures features) {
        final TIntObjectHashMap<IonNode> map = new TIntObjectHashMap<>();
        final TIntIntHashMap counter = new TIntIntHashMap(100, 0.75f, -1, 0);
        final ProcessedSample[] samples = features.getFeatures().keySet().toArray(ProcessedSample[]::new);
        Arrays.sort(samples, Comparator.comparingDouble(s -> features.getFeatures().get(s).getIntensity()).reversed());
        final FragmentedIon[] ions = Arrays.stream(samples).map(x -> features.getFeatures().get(x)).toArray(FragmentedIon[]::new);
        for (int i = 0; i < ions.length; ++i) {
            int w = (i == 0 ? 10 : (i <= 5 ? 5 : 1));
            pickCorrelated(features, map, counter, samples[i], ions[i], w, ions[i].getAdducts(), Edge.Type.ADDUCT);
            //pickCorrelated(features, map, counter, samples[i], ions[i], w, ions[i].getInSourceFragments(), Edge.Type.INSOURCE);
        }

        for (int key : counter.keys()) {
            if (counter.get(key) < 10) {
                map.remove(key);
            }
        }

        // add all nodes into the network
        for (IonNode newNode : map.valueCollection()) {
            final CorAlignedIon cor = (CorAlignedIon) newNode.getFeature().getRepresentativeIon();
            int dup = findDuplicate(newNode);
            if (dup < 0) {
                insert(newNode);
            } else {
                newNode = nodes.get(dup);
            }

            // add edge
            final Edge edge = new Edge(parentFeature, newNode, cor.type, cor.ion.correlation.getLeftType(), cor.ion.correlation.getRightType());
            edge.cor = cor.ion.correlation;
            if (!parentFeature.hasEdge(edge)) {
                parentFeature.neighbours.add(edge);
                newNode.neighbours.add(edge.reverse());

                if (Math.abs(edge.from.getFeature().getRetentionTime() - edge.to.getFeature().getRetentionTime()) > 5000) {
                    System.err.println("WTF?");
                }

            } else System.out.println("Duplicate edge " + edge);

        }
    }

    private void pickCorrelated(AlignedFeatures features, TIntObjectHashMap<IonNode> map, TIntIntHashMap counter, ProcessedSample sample, FragmentedIon fion, int w, List<CorrelatedIon> allIons, Edge.Type type) {
        for (CorrelatedIon ion : allIons) {
            IonNode cor = map.get((int) ion.ion.getMass());
            if (cor == null) {
                cor = new IonNode(new AlignedFeatures(sample, new CorAlignedIon(ion, fion, type), features.getRetentionTime()));
                map.put((int) cor.mz, cor);
                counter.adjustOrPutValue((int) cor.mz, w, w);
            } else {
                counter.adjustOrPutValue((int) cor.mz, w, w);
                cor.setFeature(cor.getFeature().merge(sample, new CorAlignedIon(ion, fion, type)));
            }
        }
    }

    private void insert(IonNode node) {
        // simple solution first
        nodes.add(node);
    }

    private int findDuplicate(IonNode node) {
        final HashSet<ProcessedSample> samples = new HashSet<>();
        for (int i = 0; i < nodes.size(); ++i) {
            final IonNode n = nodes.get(i);
            if (Math.abs(n.mz - node.mz) < 0.1 && node.getFeature().chargeStateIsNotDifferent(n.getFeature())) {
                samples.clear();
                samples.addAll(node.getFeature().getFeatures().keySet());
                samples.retainAll(n.getFeature().getFeatures().keySet());
                for (ProcessedSample s : samples) {
                    FragmentedIon fragmentedIon = n.getFeature().getFeatures().get(s);
                    if (fragmentedIon != null && fragmentedIon.getPeak().equals(node.getFeature().getFeatures().get(s).getPeak())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    protected static class CorAlignedIon extends FragmentedIon {
        private CorrelatedIon ion;
        private Edge.Type type;

        public CorAlignedIon(CorrelatedIon ion, FragmentedIon orig, Edge.Type type) {
            super(Polarity.of(orig.getPolarity()), null, null, Quality.UNUSABLE, ion.ion.getPeak(), ion.ion.getSegment());
            this.type = type;
            this.ion = ion;
        }
    }

}
