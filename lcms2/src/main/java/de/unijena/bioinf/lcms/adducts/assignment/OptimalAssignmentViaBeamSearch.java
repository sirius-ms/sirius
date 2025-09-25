package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.adducts.*;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.math3.analysis.function.Add;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class OptimalAssignmentViaBeamSearch implements SubnetworkResolver {

    @Override
    public AdductAssignment[] resolve(AdductManager manager, AdductNode[] subnetwork, int charge) {

        CompatibilityNode[] compatibilityNetwork = transformGraphIntoCompatibilityGraph(subnetwork);
        if (compatibilityNetwork == null) return null;
        // for debugging
        //compareBeamSearch(subnetwork, compatibilityNetwork, charge);
        //
        return beamSearch(manager, subnetwork, compatibilityNetwork, charge);
    }

    private AdductAssignment[] resolveCompatibilityNetwork(AdductManager manager, AdductNode[] subnetwork, CompatibilityNode[] compatibilityNetwork, int[] bestPermutation, int charge, double score) {
        // first we set each node to correct ion type

        Int2ObjectOpenHashMap<IonType> assignments = new Int2ObjectOpenHashMap<>();
        for (int c = 0; c < compatibilityNetwork.length; ++c) {
            int choice = bestPermutation[c];
            IonType basicIonType;
            if (choice > 0) {
                basicIonType = new IonType(compatibilityNetwork[c].ionTypes[choice - 1]);
            } else {
                // we do not know anything about this adduct
                basicIonType = new IonType(PrecursorIonType.unknown(charge));
            }
            // set inner nodes
            for (AdductNode u : compatibilityNetwork[c].subnodes) {
                assignments.put(u.getIndex(), basicIonType);
            }
        }
        AdductAssignment[] array = Arrays.stream(subnetwork).map(x -> new AdductAssignment(new IonType[]{assignments.get(x.getIndex())}, new double[]{1d})).toArray(AdductAssignment[]::new);
        addMissingIonTypesByTransitiveEdges(manager, array, subnetwork, assignments);
        addFallbackIonsForUnlikelyAdducts(manager, array, subnetwork, assignments);
        treatIsotopesAndInsource(manager, array, subnetwork, assignments);
//        debugPrint(subnetwork, array, score);
        return array;

    }

    private void treatIsotopesAndInsource(AdductManager manager, AdductAssignment[] array, AdductNode[] subnetwork, Int2ObjectOpenHashMap<IonType> assignments) {
        for (int i=0; i < subnetwork.length; ++i) {
            final AdductNode I = subnetwork[i];
            for (AdductEdge e : I.getEdges()) {
                boolean compatible=false;
                int iso=-1;
                for (KnownMassDelta d : e.getExplanations()) {
                    if (d instanceof IsotopeRelationship && e.getRight()==I) {
                        iso=((IsotopeRelationship)d).getIsotopicShift();
                    } else if (d.isCompatible(assignments.get(e.getLeft().getIndex()), assignments.get(e.getRight().getIndex()))) {
                        compatible=true;
                    }
                }
                if (!compatible && iso>=0) {
                    // the only explanation for this edge is an isotope peak
                    assignments.put(I.getIndex(), assignments.get(I.getIndex()).withIsotope(iso));
                    array[i] = new AdductAssignment(new IonType[]{assignments.get(I.getIndex())}, new double[]{1});
                }
            }
        }
    }

    private void addFallbackIonsForUnlikelyAdducts(AdductManager manager, AdductAssignment[] array, AdductNode[] subnetwork, Int2ObjectOpenHashMap<IonType> assignments) {
        for (int i=0; i < subnetwork.length; ++i) {
            AdductNode x = subnetwork[i];
            IonType ion = assignments.get(x.getIndex());
            IonType.Frequency freq = ion.getAdductFrequency();
            if (freq== IonType.Frequency.UNLIKELY) {
                int edgecount = 0;
                outer:
                for (AdductEdge e : x.getEdges()) {
                    for (KnownMassDelta d : e.getExplanations()) {
                        if (d instanceof AdductEdge && d.isCompatible(assignments.get(((AdductEdge) d).getLeft().getIndex()), assignments.get(((AdductEdge) d).getRight().getIndex()) )) {
                            if (++edgecount>=2) break outer;
                        }
                    }
                }
                if (edgecount<2) {
                    // add fallback if there are less than two edges
                    array[i] = array[i].withAdded(new IonType(PrecursorIonType.getPrecursorIonType(ion.getIonType().getIonization())));
                }

            }
        }
    }

    private void debugPrint(AdductNode[] nodes, AdductAssignment[] array, double score) {
        System.out.println("---------   " + score + "  -----  rt = " + nodes[0].getRetentionTime() + "   -------------");
        for (int i=0; i < nodes.length; ++i) {
            System.out.println(nodes[i] + "\t" + array[i]);
        }
        System.out.println("------------------");
    }

    /**
     * We now handle loss relationships differently:
     * if a loss relationships equals an adduct relation we omit the adduct relation AND the adduct relation has same ion type on both sides, then
     * we omit the adduct relation and insert the loss relation instead. In this function we re-insert the adduct relations again.
     * In this way, we avoid a lot of ambiguity.
     */
    private void addMissingIonTypesByTransitiveEdges(AdductManager manager, AdductAssignment[] array, AdductNode[] subnetwork, Int2ObjectOpenHashMap<IonType> assignments) {
        final Deviation dev = new Deviation(10);
        for (int i=0; i < subnetwork.length; ++i) {
            for (int j=0; j < subnetwork.length; ++j) {
                final AdductNode I = subnetwork[i];
                final AdductNode J = subnetwork[j];
                List<KnownMassDelta> knownMassDeltas = manager.retrieveMassDeltas(J.getMass() - I.getMass(), dev).stream().filter(x->
                        assignments.get(I.getIndex()).getIonType().getIonization().equals(assignments.get(J.getIndex()).getIonType().getIonization())).toList();
                for (KnownMassDelta delta : knownMassDeltas) {
                    if (delta instanceof AdductRelationship) {
                        IonType leftType = assignments.get(I.getIndex());
                        IonType rightType = assignments.get(J.getIndex());
                        if (leftType.getIonType().equals(rightType.getIonType())) {
                            PrecursorIonType ionTypeLeft = ((AdductRelationship) delta).getLeft();
                            PrecursorIonType ionTypeRight = ((AdductRelationship) delta).getRight();
                            if (!assignments.get(I.getIndex()).getIonType().equals(ionTypeLeft)        && assignments.get(J.getIndex()).getIonType().equals(ionTypeRight)) {
                                array[i] = array[i].withAdded(new IonType(ionTypeLeft));
                                // add fallback ionization
                                PrecursorIonType ionization = PrecursorIonType.getPrecursorIonType(ionTypeLeft.getIonization());
                                if (!assignments.get(I.getIndex()).getIonType().equals(ionization)) {
                                    array[i] = array[i].withAdded(new IonType(ionization));
                                }
                            }
                            if (!assignments.get(J.getIndex()).getIonType().equals(ionTypeRight)                  && assignments.get(I.getIndex()).getIonType().equals(ionTypeLeft)) {
                                array[j] = array[j].withAdded(new IonType(ionTypeRight));
                                // add fallback ionization
                                PrecursorIonType ionization = PrecursorIonType.getPrecursorIonType(ionTypeRight.getIonization());
                                if (!assignments.get(J.getIndex()).getIonType().equals(ionization)) {
                                    array[j] = array[j].withAdded(new IonType(ionization));
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int i=0; i < array.length; ++i) {
            array[i] = array[i].uniform();
        }
    }

    private double evaluate(CompatibilityNode[] compatibilityNetwork, int[] assignments) {
        double score = 0d;
        for (int i = 0; i < compatibilityNetwork.length; ++i) {
            int assignment = assignments[i];
            if (assignment == 0) continue; // ignore this ion
            int ionTypeFrom = assignment - 1;
            CompatibilityNode u = compatibilityNetwork[i];
            for (int edge = 0; edge < u.edgesPerIonType[ionTypeFrom].size(); ++edge) {
                CompatibilityEdge uv = u.edgesPerIonType[ionTypeFrom].get(edge);
                int ionTypeTo = assignments[uv.to.index] - 1;
                if (uv.toType == ionTypeTo) {
                    score += uv.score;
                } else if (ionTypeTo < 0) {
                    // no score but also no incompatibility
                } else {
                    return Double.NEGATIVE_INFINITY; // incompatible network
                }
            }
        }
        return score;
    }

    private void compareBeamSearch(AdductNode[] subnetwork, CompatibilityNode[] nodes, int charge) {
        // 1.) sort all edges by score
        ArrayList<CompatibilityEdge> edges = new ArrayList<>();
        for (CompatibilityNode u : nodes) {
            for (List<CompatibilityEdge> uvs : u.edgesPerIonType) {
                for (CompatibilityEdge uv : uvs) {
                    if (uv.from.index < uv.to.index) {
                        edges.add(uv);
                    }
                }
            }
        }
        edges.sort(Comparator.comparingDouble(x -> -x.score));
        // 2.) do beamsearch on edges
        AdductBeamSearch adductBeamSearch = new AdductBeamSearch(nodes.length, 10);
        for (CompatibilityEdge uv : edges) {
            adductBeamSearch.add(uv.from.index, uv.fromType + 1, uv.to.index, uv.toType + 1, uv.score);
        }
        // 3.) return best result
        AdductBeamSearch.MatchNode[] topSolutions = adductBeamSearch.getTopSolutions();
        for (int x = 0; x < Math.min(3, topSolutions.length); ++x) {
            System.out.println(Arrays.toString(topSolutions[x].assignment()) + "\t" + topSolutions[x].score());
        }
        double threshold = topSolutions[0].score() - 3d;
        if (topSolutions.length > 1 && topSolutions[1].score() >= threshold) {
            System.out.println("######");
            for (int k = 0; k < topSolutions.length; ++k) {
                if (topSolutions[k].score() >= threshold) prettyprint(nodes, topSolutions[k].assignment());
            }
        }
        System.out.println("-----------------------");
    }

    private void prettyprint(CompatibilityNode[] nodes, int[] assignment) {
        for (CompatibilityNode node : nodes) {
            if (assignment[node.index] == 0) continue;
            for (CompatibilityEdge edge : node.edgesPerIonType[assignment[node.index] - 1]) {
                CompatibilityNode u = edge.from;
                CompatibilityNode v = edge.to;
                if (assignment[v.index] == 0 || assignment[u.index] == 0) continue;
                if ((assignment[u.index] - 1) == edge.fromType && (assignment[v.index] - 1) == edge.toType) {
                    System.out.print(u.ionTypes[assignment[u.index] - 1] + " -> " + v.ionTypes[assignment[v.index] - 1] + " (" + edge.score + "),\t");
                }
            }
        }
        System.out.println();
    }

    private AdductAssignment[] beamSearch(AdductManager manager, AdductNode[] subnetwork, CompatibilityNode[] nodes, int charge) {
        // 1.) sort all edges by score
        ArrayList<CompatibilityEdge> edges = new ArrayList<>();
        for (CompatibilityNode u : nodes) {
            for (List<CompatibilityEdge> uvs : u.edgesPerIonType) {
                for (CompatibilityEdge uv : uvs) {
                    if (uv.from.index < uv.to.index) {
                        edges.add(uv);
                    }
                }
            }
        }
        edges.sort(Comparator.comparingDouble(x -> -x.score));
        // 2.) do beamsearch on edges
        AdductBeamSearch adductBeamSearch = new AdductBeamSearch(nodes.length, 30);
        for (CompatibilityEdge uv : edges) {
            adductBeamSearch.add(uv.from.index, uv.fromType + 1, uv.to.index, uv.toType + 1, uv.score);
        }
        // 3.) return best results
        return mergeTopResults(manager, adductBeamSearch.getTopSolutions(), subnetwork, nodes, charge);
    }

    private AdductAssignment[] mergeTopResults(AdductManager manager, AdductBeamSearch.MatchNode[] topSolutions, AdductNode[] subnetwork, CompatibilityNode[] nodes, int charge) {
        if (topSolutions.length == 0) return null;
        double topScore = topSolutions[0].score();
        if (topScore < 2) return null; // reject
        final double threshold = topScore - 3;
        topSolutions = Arrays.stream(topSolutions).takeWhile(x -> x.score() >= threshold).toArray(AdductBeamSearch.MatchNode[]::new);
        if (topSolutions.length == 1) {
            return resolveCompatibilityNetwork(manager, subnetwork, nodes, topSolutions[0].assignment(), charge, topScore);
        }
        AdductAssignment[][] assignments = Arrays.stream(topSolutions).map(x -> resolveCompatibilityNetwork(manager, subnetwork, nodes, x.assignment(), charge, x.score())).toArray(AdductAssignment[][]::new);
        double[] scores = Arrays.stream(topSolutions).mapToDouble(AdductBeamSearch.MatchNode::score).map(x -> Math.exp(x - topScore)).toArray();
        AdductAssignment[] merged = new AdductAssignment[subnetwork.length];
        for (int i = 0; i < subnetwork.length; ++i) {
            final int I = i;
            merged[i] = AdductAssignment.merge(charge, Arrays.stream(assignments).map(x -> x[I]).toArray(AdductAssignment[]::new), scores);
        }
        return merged;
    }

/*
    private CompatibilityNode[] transformGraphIntoCompatibilityGraph(AdductNode[] network) {
        final Int2IntOpenHashMap nodecoloring = new Int2IntOpenHashMap();
        nodecoloring.defaultReturnValue(-1);
        final ArrayList<HashSet<AdductEdge>> edgesPerColor = new ArrayList<>();
        final ArrayList<ArrayList<AdductNode>> nodesPerColor = new ArrayList<>();
        ArrayList<AdductNode> stack = new ArrayList<>();
        int currentColor = -1;
        for (AdductNode u : network) {
            if (nodecoloring.get(u.getIndex()) < 0) {
                ++currentColor;
                // node has no color, give it a new color
                nodecoloring.put(u.getIndex(), currentColor);
                nodesPerColor.add(new ArrayList<>());
                edgesPerColor.add(new HashSet<>());
                nodesPerColor.get(currentColor).add(u);
                stack.add(u);
            }
            while (!stack.isEmpty()) {
                u = stack.remove(stack.size() - 1);
                for (AdductEdge uv : u.getEdges()) {
                    if (!uv.isAdductEdge()) {
                        AdductNode v = uv.getOther(u);
                        if (nodecoloring.get(v.getIndex()) < 0) {
                            stack.add(v);
                            nodecoloring.put(v.getIndex(), currentColor);
                            nodesPerColor.get(currentColor).add(v);
                        }
                    } else {
                        edgesPerColor.get(currentColor).add(uv);
                    }
                }
            }
        }
        if (currentColor <= 0) return null; // no adducts in the network
        CompatibilityNode[] nodes = new CompatibilityNode[currentColor + 1];
        for (int k = 0; k <= currentColor; ++k) {
            nodes[k] = new CompatibilityNode(k, nodesPerColor.get(k).toArray(AdductNode[]::new));
        }
        // add ion types
        Object2IntOpenHashMap[] allIontypes = new Object2IntOpenHashMap[currentColor + 1];
        for (int color = 0; color <= currentColor; ++color) {
            HashSet<AdductEdge> adductEdges = edgesPerColor.get(color);
            Object2IntOpenHashMap<PrecursorIonType> ionTypes = new Object2IntOpenHashMap<>();
            for (AdductEdge edge : adductEdges) {
                int fromColor = nodecoloring.get(edge.getLeft().getIndex());
                for (KnownMassDelta explanation : edge.getExplanations()) {
                    if (explanation instanceof AdductRelationship) {
                        PrecursorIonType type;
                        if (fromColor == color) {
                            type = (((AdductRelationship) explanation).getLeft());
                        } else {
                            type = (((AdductRelationship) explanation).getRight());
                        }
                        if (!ionTypes.containsKey(type)) {
                            ionTypes.put(type, ionTypes.size());
                        }
                    }
                }
            }
            CompatibilityNode u = nodes[color];
            u.ionTypes = new PrecursorIonType[ionTypes.size()];
            ionTypes.forEach((x, y) -> u.ionTypes[y] = x);
            u.edgesPerIonType = new ArrayList[u.ionTypes.length];
            for (int i = 0; i < u.edgesPerIonType.length; ++i) u.edgesPerIonType[i] = new ArrayList<>();
            allIontypes[color] = ionTypes;
        }

        // add edges
        BitSet noAdductEdgeColors = new BitSet(currentColor+1);
        for (int color = 0; color <= currentColor; ++color) {
            HashSet<AdductEdge> adductEdges = edgesPerColor.get(color);
            for (AdductEdge edge : adductEdges) {
                if (nodecoloring.get(edge.getLeft().getIndex()) == color) {
                    // edge goes from left to right
                    for (KnownMassDelta explanation : edge.getExplanations()) {
                        if (explanation instanceof AdductRelationship) {
                            int toIndex = nodecoloring.get(edge.getRight().getIndex());
                            int fromType = allIontypes[color].getInt(((AdductRelationship) explanation).getLeft());
                            CompatibilityEdge cedge = new CompatibilityEdge(nodes[color], nodes[toIndex],
                                    fromType,
                                    allIontypes[toIndex].getInt(((AdductRelationship) explanation).getRight()),
                                    edge);
                            nodes[color].edgesPerIonType[fromType].add(cedge);
                        } else {
                            noAdductEdgeColors.set(nodecoloring.get(edge.getRight().getIndex()));
                        }
                    }
                } else {
                    // edge goes from right to left
                    for (KnownMassDelta explanation : edge.getExplanations()) {
                        if (explanation instanceof AdductRelationship) {
                            int toIndex = nodecoloring.get(edge.getLeft().getIndex());
                            int fromType = allIontypes[color].getInt(((AdductRelationship) explanation).getRight());
                            CompatibilityEdge cedge = new CompatibilityEdge(nodes[color], nodes[toIndex],
                                    fromType,
                                    allIontypes[toIndex].getInt(((AdductRelationship) explanation).getLeft()),
                                    edge);
                            nodes[color].edgesPerIonType[fromType].add(cedge);
                        } else {
                            noAdductEdgeColors.set(nodecoloring.get(edge.getLeft().getIndex()));
                        }
                    }

                }
            }
            // check if we have to add additional edges for the special case that two node sets are connected via
            // a loss (e.g. H2O). In this case we have to add a special edge with same ion type on both ends
            for (int otherColor=noAdductEdgeColors.nextSetBit(0); otherColor >= 0; otherColor=noAdductEdgeColors.nextSetBit(otherColor+1)) {
                Set<>
                CompatibilityEdge cedge = new CompatibilityEdge(nodes[color], nodes[otherColor],
                        ,
                        allIontypes[toIndex].getInt(((AdductRelationship) explanation).getLeft()),
                        edge);
                nodes[color].edgesPerIonType[fromType].add(cedge);
            }
        }
        return nodes;
    }

 */

    private CompatibilityNode[] transformGraphIntoCompatibilityGraph(AdductNode[] subNetwork) {
        Int2ObjectOpenHashMap<AdductNode> nodes = new Int2ObjectOpenHashMap<>();
        for (AdductNode u : subNetwork) {
            nodes.put(u.getIndex(), u);
        }
        // first we get the supernodes that are connected via adduct edges
        int[][] superNodes = getConncectivityComponents(nodes, edge->!edge.isAdductEdge());
        // for each of these nodes we can generate a compability node
        CompatibilityNode[] cnodes = new CompatibilityNode[superNodes.length];
        Int2IntOpenHashMap nodeColoring = new Int2IntOpenHashMap();
        Set<PrecursorIonType>[] adductTypes = new Set[superNodes.length];
        for (int i=0; i < cnodes.length; ++i) {
            final int index = i;
            adductTypes[index] = new HashSet<>();
            cnodes[i] = new CompatibilityNode(i, Arrays.stream(superNodes[i]).mapToObj(nodes::get).toArray(AdductNode[]::new));
            Arrays.stream(superNodes[i]).forEach(c->nodeColoring.put(c,index));
        }
        // we now collect potential ion types by collecting all adduct types in edges that are between two supernodes
        for (AdductNode n : subNetwork) {
            for (AdductEdge e : n.getEdges()) {
                if (e.isAdductEdge()) {
                    AdductNode u = e.getLeft(), v = e.getRight();
                    int c1 = nodeColoring.get(u.getIndex());
                    int c2 = nodeColoring.get(v.getIndex());
                    adductTypes[c1].addAll(Arrays.stream(e.getExplanations()).filter(x->x instanceof AdductRelationship).map(x->((AdductRelationship)x).getLeft()).toList());
                    adductTypes[c2].addAll(Arrays.stream(e.getExplanations()).filter(x->x instanceof AdductRelationship).map(x->((AdductRelationship)x).getRight()).toList());
                }
            }
        }
        // there is an edge case though: two nodes X and Y have an adduct edge a->b AND an in-source edge. Currently,
        // we have adduct types {a} in X and {b} in Y, but we have to expand this to {a,b} in X and {a,b} in Y, because
        // the in-source edge is only compatible if both ends have the same adduct.
        int[][] superNodesInSource = getConncectivityComponents(nodes, AdductEdge::isModificationEdge);
        Set<PrecursorIonType>[] additionalIonTypes = new Set[cnodes.length];
        for (int[] components : superNodesInSource) {
            int[] componentColors = Arrays.stream(components).map(nodeColoring::get).distinct().toArray();
            if (componentColors.length>1) {
                HashSet<PrecursorIonType> allIonTypes = new HashSet<>();
                for (int color : componentColors) {
                    allIonTypes.addAll(adductTypes[color]);
                }
                for (int color : componentColors) {
                    if (additionalIonTypes[color]==null) additionalIonTypes[color] = allIonTypes;
                    else additionalIonTypes[color].addAll(allIonTypes);
                }
            }
        }
        for (int color=0; color < additionalIonTypes.length; ++color) {
            if (additionalIonTypes[color]!=null) {
                adductTypes[color].addAll(additionalIonTypes[color]);
            }
            cnodes[color].specifyIonTypes(adductTypes[color].toArray(PrecursorIonType[]::new));
        }

        // now we have all compatibility nodes done and can add the compatibility edges
        for (AdductNode n : subNetwork) {
            for (AdductEdge e : n.getEdges()) {
                final AdductNode u = e.getLeft(), v = e.getRight();
                if (v==n) continue; // count each edge only once!
                final int cu = nodeColoring.get(u.getIndex()), cv = nodeColoring.get(v.getIndex());
                if (cu!=cv) {
                    cnodes[cu].addCompatibilityEdge(cnodes[cv], e);
                }
            }
        }
        return cnodes;
    }

    /**
     * Given a network of adducts, output all connectivity components (by node indices) using only edges
     * that pass the given predicate
     */
    protected static int[][] getConncectivityComponents(Int2ObjectOpenHashMap<AdductNode> nodes, Predicate<AdductEdge> predicate) {

        ArrayList<int[]> components = new ArrayList<>();
        IntArrayList stack = new IntArrayList();
        Int2IntOpenHashMap coloring = new Int2IntOpenHashMap(nodes.size());
        coloring.defaultReturnValue(-1);
        IntArrayList buffer = new IntArrayList();
        nodes.keySet().intStream().forEach((index) -> {
            if (coloring.get(index)<0) {
                coloring.put(index, components.size());
                stack.add(index);
                buffer.add(index);
                while (!stack.isEmpty()) {
                    int u = stack.popInt();
                    AdductNode U = nodes.get(u);
                    for (AdductEdge e : U.getEdges()) {
                        AdductNode V = e.getOther(U);
                        if (coloring.get(V.getIndex())<0 && predicate.test(e)) {
                            coloring.put(V.getIndex(), components.size());
                            stack.add(V.getIndex());
                            buffer.add(V.getIndex());
                        }
                    }
                }
                components.add(buffer.toIntArray());
                buffer.clear();
            }
        });
        return components.toArray(int[][]::new);
    }


    protected static class CompatibilityNode {
        AdductNode[] subnodes;
        PrecursorIonType[] ionTypes;
        List<CompatibilityEdge>[] edgesPerIonType;
        int index;

        public CompatibilityNode(int index, AdductNode[] subnodes) {
            this.index = index;
            this.subnodes = subnodes;
        }

        @Override
        public String toString() {
            return Arrays.toString(subnodes) + " with modes: " + Arrays.toString(ionTypes);
        }

        public void specifyIonTypes(PrecursorIonType[] array) {
            this.ionTypes = array;
            this.edgesPerIonType = new List[array.length];
            for (int i=0; i < edgesPerIonType.length; ++i) edgesPerIonType[i] = new ArrayList<CompatibilityEdge>();
        }

        public void addCompatibilityEdge(CompatibilityNode v, AdductEdge uv) {
            int fromType=0, toType=0;
            boolean addAllsame=false;
            for (KnownMassDelta d : uv.getExplanations()) {
                PrecursorIonType l,r;
                if (d instanceof AdductRelationship) {
                    l = ((AdductRelationship) d).getLeft();
                    r = ((AdductRelationship) d).getRight();
                    for (fromType=0; fromType < ionTypes.length; ++fromType) {
                        if (l.equals(ionTypes[fromType])) break;
                    }
                    for (toType=0; toType < v.ionTypes.length; ++toType) {
                        if (r.equals(v.ionTypes[toType])) break;
                    }
                    if (fromType>=ionTypes.length || toType>=v.ionTypes.length) {
                        throw new RuntimeException("Ion type not specified");
                    }
                    this.edgesPerIonType[fromType].add(new CompatibilityEdge(this, v, fromType, toType, uv));
                    v.edgesPerIonType[toType].add(new CompatibilityEdge(v, this, toType, fromType, uv));
                }else {
                    addAllsame=true;
                }
            }
            if (addAllsame) {
                for (int i=0; i < ionTypes.length; ++i) {
                    for (int j=0; j < v.ionTypes.length; ++j) {
                        if (ionTypes[i].equals(v.ionTypes[j])) {
                            this.edgesPerIonType[i].add(new CompatibilityEdge(this, v, i, j, uv));
                            v.edgesPerIonType[j].add(new CompatibilityEdge(v, this, j, i, uv));
                            break;
                        }
                    }
                }
            }
        }
    }

    protected static class CompatibilityEdge {
        private final double score;
        private final CompatibilityNode from, to;
        private final AdductEdge underlyingEdge;
        private final int fromType, toType;

        public CompatibilityEdge(CompatibilityNode from, CompatibilityNode to, int fromType, int toType, AdductEdge edge) {
            this.score = edge.getScore();
            this.from = from;
            this.to = to;
            this.fromType = fromType;
            this.toType = toType;
            this.underlyingEdge = edge;
        }
    }

}
