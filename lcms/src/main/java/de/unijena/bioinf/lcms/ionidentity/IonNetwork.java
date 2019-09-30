package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
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
            int k=0;
            for (IonNode node : nodes) {
                node2Id.put(node,++k);
                final long rrt = Math.round(node.getFeature().getRetentionTime() / 1000d);
                String mass = String.format(Locale.US, "%.3f", node.getFeature().getMass());
                String ret = String.format(Locale.US, "%d min, %d s", (int)(rrt/60d), (int)(rrt%60));
                int type = node.getFeature().getFeatures().values().stream().anyMatch(x->x.getMsMs()!=null) ? 1 : 0;
                w.write("\t{\"id\": " + k + ", \"type\": " + type + ", \"types\": " + node.likelyTypesWithProbs() +  ", \"name\": \"m/z " + mass + "\", \"mass\": " + mass + ", \"rt\": \"" + ret +"\"},\n" );
            }
            w.write("],\n");
            w.write("\"links\": [");
            final ArrayList<Edge> allEdges = new ArrayList<>();
            for (IonNode node : nodes) {
                for (Edge e : node.neighbours) {
                    if (e.deltaMz()>=0) {
                        String name = e.description();
                        w.write("\t{\"id\":" + allEdges.size() + ",\"source\": " + node2Id.get(e.from) + ", \"target\":" + node2Id.get(e.to) + ", \"type\": \"" + e.type.toString() + "\", \"name\": \"" + name  + "\"},\n" );
                        allEdges.add(e);
                    }
                }
            }
            w.write("],\n");
            // add ion information
            w.write("\"peaks\": [\n");
            for (int q=0; q < allEdges.size(); ++q) {
                final Edge e = allEdges.get(q);
                final IonGroup[] ions = e.getMatchingIons2();
                w.write("\t[");
                for (IonGroup g : ions) {
                    w.write("[");
                    for (int p=g.getSegment().getStartIndex(); p  < g.getSegment().getEndIndex(); ++p) {
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
            if (node.assignment==null)
                sampler.sample(node);
        }
        for (IonNode node : nodes) {
            assignment.assignment(node.getFeature(), node.assignment.ionTypes, node.assignment.probabilities);
        }
    }

    public void addNode(AlignedFeatures features) {
        IonNode node = new IonNode(features);

        // 1. check if we already have this node
        {
            final int dup = findDuplicate(node);
            if (dup>=0) {
                nodes.get(dup).setFeature(merge(features, nodes.get(dup).getFeature()));
                node = nodes.get(dup);
            } else {
                insert(node);
            }
        }

        // 2. add all correlated peaks IF they seem to be reproducible
        addCorrelated(node,node.getFeature());
    }

    private AlignedFeatures merge(AlignedFeatures a, AlignedFeatures b) {
        return b.without(a.getFeatures().keySet()).map(x->a.merge(x)).orElse(a);
    }

    private void addCorrelated(IonNode parentFeature, AlignedFeatures features) {
        final TIntObjectHashMap<IonNode> map = new TIntObjectHashMap<>();
        final TIntIntHashMap counter = new TIntIntHashMap(100,0.75f,-1,0);
        final ProcessedSample[] samples = features.getFeatures().keySet().toArray(ProcessedSample[]::new);
        Arrays.sort(samples, Comparator.comparingDouble(s->features.getFeatures().get(s).getIntensity()).reversed());
        final FragmentedIon[] ions = Arrays.stream(samples).map(x->features.getFeatures().get(x)).toArray(FragmentedIon[]::new);
        for (int i=0; i < ions.length; ++i) {
            int w = (i == 0 ? 10 : (i <= 5 ? 5 : 1));
            pickCorrelated(features, map, counter, samples[i], ions[i], w, ions[i].getAdducts(), Edge.Type.ADDUCT);
            //pickCorrelated(features, map, counter, samples[i], ions[i], w, ions[i].getInSourceFragments(), Edge.Type.INSOURCE);
        }

        for (int key : counter.keys()) {
            if (counter.get(key)<10) {
                map.remove(key);
            }
        }

        // add all nodes into the network
        for (IonNode newNode : map.valueCollection()) {
            final CorAlignedIon cor = (CorAlignedIon)newNode.getFeature().getRepresentativeIon();
            int dup = findDuplicate(newNode);
            if (dup<0) {
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
            } else System.out.println("Duplicate edge " + edge);

        }
    }

    private void pickCorrelated(AlignedFeatures features, TIntObjectHashMap<IonNode> map, TIntIntHashMap counter, ProcessedSample sample, FragmentedIon fion, int w, List<CorrelatedIon> allIons, Edge.Type type) {
        for (CorrelatedIon ion : allIons) {
            IonNode cor = map.get((int)ion.ion.getMass());
            if (cor == null) {
                cor = new IonNode(new AlignedFeatures(sample,new CorAlignedIon(ion, fion, type),features.getRetentionTime()));
                map.put((int)cor.mz, cor);
                counter.adjustOrPutValue((int)cor.mz, w,w);
            } else {
                counter.adjustOrPutValue((int)cor.mz, w,w);
                cor.setFeature(cor.getFeature().merge(sample,new CorAlignedIon(ion, fion, type)));
            }
        }
    }

    private void insert(IonNode node) {
        // simple solution first
        nodes.add(node);
    }

    private int findDuplicate(IonNode node) {
        final HashSet<ProcessedSample> samples = new HashSet<>();
        for (int i=0; i < nodes.size(); ++i) {
            final IonNode n = nodes.get(i);
            if (Math.abs(n.mz-node.mz)<0.1 && node.getFeature().chargeStateIsNotDifferent(n.getFeature())) {
                samples.clear();
                samples.addAll(node.getFeature().getFeatures().keySet());
                samples.retainAll(n.getFeature().getFeatures().keySet());
                for (ProcessedSample s : samples) {
                    FragmentedIon fragmentedIon = n.getFeature().getFeatures().get(s);
                    if (fragmentedIon!=null && fragmentedIon.getPeak().equals(node.getFeature().getFeatures().get(s).getPeak())) {
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
            super(Polarity.of(orig.getPolarity()), null,null, Quality.UNUSABLE, ion.ion.getPeak(), ion.ion.getSegment());
            this.type = type;
            this.ion = ion;
        }
    }

}
