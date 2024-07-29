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

package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.lcms.CorrelatedPeakDetector;
import de.unijena.bioinf.lcms.InternalStatistics;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class IonNetwork {

    protected ArrayList<IonNode> nodes;
    private TFloatArrayList signalIntensities;

    public IonNetwork() {
        this.nodes = new ArrayList<>();
        signalIntensities = new TFloatArrayList();
    }

    public void collectStatistics(InternalStatistics statistics) {
        for (IonNode node : nodes) {
            for (Edge e : node.neighbours) {
                if (e.from.mz < e.to.mz) {
                    statistics.interFeatureCorrelation.add(e.calculateInterSampleCorrelation(0d)[2]);
                    statistics.intraFeatureCorrelation.add(e.calculateIntraSampleCorrelation());
                }
            }
        }
    }

    public void deleteSingletons() {
        nodes.removeIf(x->x.neighbours.isEmpty());
    }

    public void writeToFile(LCMSProccessingInstance i, File file) throws IOException {
        final GibbsSampler sampler = new GibbsSampler(i);
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
                w.write("\t{\"id\": " + k + ", \"type\": " + type + ", \"types\": " + node.likelyTypesWithProbs() + ", \"name\": \"m/z " + mass + "\", \"mass\": " + mass + ", \"rt\": \"" + ret + "\", \"scores\": " + node.typesWithScore(sampler) +  "},\n");
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
            w.write("]}");
            /*
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
             */

        }
    }

    protected double signalThreshold = Double.NaN;
    protected double getSignalThreshold() {
        if (Double.isNaN(signalThreshold)) {
            signalIntensities.sort();
            this.signalThreshold = signalIntensities.get((int)Math.floor(signalIntensities.size()*0.25));
            signalIntensities = null;
        }
        return signalThreshold;
    }

    public void gibbsSampling(LCMSProccessingInstance instance, Assignment assignment) {
        final GibbsSampler sampler = new GibbsSampler(instance);

        final float quantile25 = (float)getSignalThreshold();
        nodes.forEach(node->node.priorForUnknownIonType = scoreNode(node,quantile25));
        float average = (float)nodes.stream().mapToDouble(x->x.priorForUnknownIonType).average().orElse(0d);
        nodes.forEach(x->x.priorForUnknownIonType = Math.max(x.priorForUnknownIonType,average));

        try (final PrintStream OUT = new PrintStream("edge_scores.txt")){
            for (IonNode node : nodes) {
                for (Edge edge : node.neighbours) {
                LoggerFactory.getLogger(getClass()).debug(String.format("score = %f - %f (evidences: inter %d and intra %d)\n", edge.score, (edge.from.priorForUnknownIonType + edge.to.priorForUnknownIonType), edge.evidencesInter, edge.evidencesIntra));
                OUT.printf("score = %f - %f (evidences: inter %d and intra %d)\n", edge.score, (edge.from.priorForUnknownIonType + edge.to.priorForUnknownIonType), edge.evidencesInter, edge.evidencesIntra);
            }}
            } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        for (IonNode node : nodes) {
            if (node.assignment == null)
                sampler.sample(node);
        }
        for (IonNode node : nodes) {
            assignment.assignment(node.getFeature(), node.assignment.ionTypes, node.assignment.probabilities);
        }
    }

    private float scoreNode(IonNode node, float quantile25) {
        return (float)(1+Math.floor(Math.pow(node.getFeature().getNumberOfIntensiveFeatures(quantile25), 4.0/5.0)));
    }

    /**
     * after building and scoring the network, look for edges in the network that do not exist in the alignment table
     * add them as correlated ions
     */
    public void reinsertLikelyCorrelatedEdgesIntoFeatures() {
        for (IonNode node : nodes) {
            final Set<PrecursorIonType> ionTypes = node.likelyIonTypes();
            for (Edge e : node.neighbours) {
                if (ionTypes.contains(e.fromType) && e.to.assignment.probability(e.toType)>=0.1) {
                    // add edge and nodes
                    final IonNode neighbour = e.to;
                    ensureInsertion(node, neighbour, e);
                }
            }
        }
    }

    private void ensureInsertion(IonNode node, IonNode neighbour, Edge e) {
        final float PROB = e.assignmentProbability();
        node.getFeature().addConnection(neighbour.getFeature(), IonConnection.ConnectionType.IN_SOURCE_OR_ADDUCT, PROB);
        eachEntry:
        for (Map.Entry<ProcessedSample, FragmentedIon> entry : node.getFeature().getFeatures().entrySet()) {
            ProcessedSample sample = entry.getKey();
            FragmentedIon ion = entry.getValue();
            // get corresponding ion for neighbour
            FragmentedIon other = neighbour.getFeature().getFeatures().get(sample);
            if (other==null) {
                // does not exist, we cannot add it
                continue;
            }
            if (other.isCompound()) {
                ion.addConnection(other, IonConnection.ConnectionType.IN_SOURCE_OR_ADDUCT, PROB);
            }
            for (CorrelatedIon adduct : ion.getAdducts()) {
                if (other instanceof CorAlignedIon) {
                    if (((CorAlignedIon) other).ion.equals(adduct)) {
                        // already exist. We do not have to add anything
                        continue eachEntry;
                    }
                } else {
                    if (adduct.ion.getSegment().getApexScanNumber() == other.getSegment().getApexScanNumber() && Math.abs(adduct.ion.getMass()-other.getMass())<0.1) {
                        // already exist. We do not have to add anything
                        continue eachEntry;
                    }
                }
            }
            // otherwise, add the ion
            if (other instanceof CorAlignedIon) {
                // it is strange that this happens, but let's correct it here
                // TODO: we should at some point check why this adduct was not
                // added in the CorrelatedPeakDetector step
                ion.getAdducts().add(((CorAlignedIon) other).ion);
            } else {
                ion.getAdducts().add(new CorrelatedIon(e.cor, other));
            }

        }
    }

    public void addNode(AlignedFeatures features) {
        IonNode node = new IonNode(features);
        nodes.add(node);
        for (FragmentedIon s : features.getFeatures().values()) {
            // TODO: we should do this for each sample separately
            final List<CorrelationGroup> isotopes = s.getIsotopes();
            if (isotopes.isEmpty()) {
                signalIntensities.add((float)s.getIntensity());
            } else {
                signalIntensities.add((float)isotopes.get(isotopes.size()-1).getRightSegment().getApexIntensity());
            }
        }
    }

    public void addCorrelatedEdgesForAllNodes(LCMSProccessingInstance instance) {
        final double signalThreshold = getSignalThreshold();
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(instance.getDetectableIonTypes());
        final IonNode[] todo = nodes.toArray(IonNode[]::new);
        for (IonNode node : todo) {
            final NavigableMap<Double, Link> links = new TreeMap<>();
            final Deviation dev = new Deviation(10);
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
                    dummy.getFeatures().forEach(detector::detectCorrelatedPeaks);
                    IonNode pseudoNode = new IonNode(dummy);
                    nodes.add(pseudoNode);
                    link.associatedNode = pseudoNode;
                } else {
                    final AlignedFeatures adduct = link.associatedNode.getFeature();
                    PrecursorIonType ionTypeLeft = null, ionTypeRight = null;
                    for (ImmutablePair<ProcessedSample, CorrelatedIon> pair : link.ions) {
                        if (adduct.getFeatures().get(pair.left) == null) {
                            final CorAlignedIon value = new CorAlignedIon(pair.right, F.get(pair.left), Edge.Type.ADDUCT);
                            adduct.getFeatures().put(pair.left, value);
                            LoggerFactory.getLogger(IonNetwork.class).warn("Detect ion afterwards.");
                            detector.detectCorrelatedPeaks(pair.left, value);
                        }
                    }
                }
                final HashMap<Pair<PrecursorIonType,PrecursorIonType>,List<CorrelatedIon>> lnks = new HashMap<>();
                for (ImmutablePair<ProcessedSample, CorrelatedIon> pair : link.ions) {
                    final AdductMassDifference diff = pair.right.correlation.getAdductAssignment();
                    for (int i=0; i < diff.size(); ++i) {
                        lnks.computeIfAbsent(Pair.of(diff.getLeftAt(i), diff.getRightAt(i)),(x)->new ArrayList<>()).add(pair.right);
                    }
                }

                for (var p : lnks.entrySet()) {
                    // if we have more than 10 samples, we only add edges if we see a correlation in at least two samples
                    if (node.getFeature().getFeatures().keySet().size()<10 || p.getValue().size()>1) {
                        // now add the edge into the graph
                        final Edge edge = new Edge(node, link.associatedNode, Edge.Type.ADDUCT, p.getKey().getLeft(), p.getKey().getRight());
                        scoreEdge(p, edge, signalThreshold);


                        // for debugging purpose
                        {
                            edge.cor = p.getValue().stream().max(Comparator.comparingDouble(i -> i.correlation.getNumberOfCorrelatedPeaks())).map(x -> x.correlation).orElse(null);
                            edge.totalNumberOfCorrelatedPeaks = p.getValue().stream().mapToInt(i->i.correlation.getNumberOfCorrelatedPeaks()).sum();
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

    }

    private void scoreEdge(Map.Entry<Pair<PrecursorIonType, PrecursorIonType>, List<CorrelatedIon>> p, Edge edge, double signalThreshold) {

        final double[] intraSampleCorrelationScores = p.getValue().stream().mapToDouble(x->-Math.log(Math.max(0.01, 1.0-x.correlation.score)) + Math.log(0.5)).toArray();
        Arrays.sort(intraSampleCorrelationScores);
        double intraSampleCorrelationScore = 0d;
        for (int i=0; i < intraSampleCorrelationScores.length; ++i) {
            intraSampleCorrelationScore += intraSampleCorrelationScores[i];
        }


        // DEBUG
        edge.evidencesIntra = p.getValue().size();
        edge.debugScoreIntra = intraSampleCorrelationScore;

        //final double[] interSampleCorrelationScore = edge.calculateInterSampleCorrelation(signalThreshold);
        //final double weighted = interSampleCorrelationScore[0]*interSampleCorrelationScore[3];
        final double scoreExtra = edge.calculateEdgeScore(signalThreshold);
        edge.debugScoreExtra = scoreExtra;
        edge.correlationGroups = p.getValue().stream().map(x->x.correlation).toArray(CorrelationGroup[]::new);
        edge.score = (float)(intraSampleCorrelationScore+ scoreExtra);//(float)// (intraSampleCorrelationScore + weighted);
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

    /*
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
                    System.err.println("WTF? Adduct and Ion differ massively in retention time. " );
                }

            } else System.out.println("Duplicate edge " + edge);

        }
    }
*/
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
            super(Polarity.of(orig.getPolarity()), null, null, null, Quality.UNUSABLE, ion.ion.getPeak(), ion.ion.getSegment(),new Scan[0]);
            this.type = type;
            this.ion = ion;
        }

        public boolean isCompound() {
            return false;
        }
    }

}
