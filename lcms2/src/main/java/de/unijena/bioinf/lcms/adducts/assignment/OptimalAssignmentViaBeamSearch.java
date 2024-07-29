package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.adducts.*;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

public class OptimalAssignmentViaBeamSearch implements SubnetworkResolver {

    @Override
    public AdductAssignment[] resolve(AdductNode[] subnetwork, int charge) {

        CompatibilityNode[] compatibilityNetwork = transformGraphIntoCompatibilityGraph(subnetwork);
        if (compatibilityNetwork == null) return null;
        // for debugging
        //compareBeamSearch(subnetwork, compatibilityNetwork, charge);
        //
        return beamSearch(subnetwork, compatibilityNetwork, charge);
    }

    private AdductAssignment[] resolveCompatibilityNetwork(AdductNode[] subnetwork, CompatibilityNode[] compatibilityNetwork, int[] bestPermutation, int charge) {
        // first we set each node to correct ion type
        Int2ObjectOpenHashMap<IonType> assignments = new Int2ObjectOpenHashMap<>();
        for (int c = 0; c < compatibilityNetwork.length; ++c) {
            int choice = bestPermutation[c];
            IonType basicIonType;
            if (choice > 0) {
                basicIonType = new IonType(compatibilityNetwork[c].ionTypes[choice - 1], 1f, MolecularFormula.emptyFormula());
            } else {
                // we do not know anything about this adduct
                basicIonType = new IonType(PrecursorIonType.unknown(charge), 1f, MolecularFormula.emptyFormula());
            }
            /*
            // apply incoming edges from compatibility graph
            for (CompatibilityEdge edge : compatibilityNetwork[c].edgesPerIonType[choice-1]) {
                // only use edges we already processed
                if (edge.to.index < c) {
                    int l = indizes.get(edge.underlyingEdge.getLeft().getIndex());
                    int r = indizes.get(edge.underlyingEdge.getRight().getIndex());
                    if (assignments[l]==null) {int i = indizes.get(u.getIndex());
                assignments[i] = basicIonType;
                        assignments[l] = basicIonType.withInsource(assignments[r].getInsource()).withMultimere(assignments[r].getMultimere());
                    } else {
                        assert assignments[r]==null;
                        assignments[r] = basicIonType.withInsource(assignments[l].getInsource()).withMultimere(assignments[l].getMultimere());
                    }
                }
             */
            // set inner nodes
            for (AdductNode u : compatibilityNetwork[c].subnodes) {
                assignments.put(u.getIndex(), basicIonType);
            }
        }
        spreadMultimere(subnetwork, assignments);
        spreadInsource(subnetwork, assignments);

        /*
            // next, apply the underlying insource and multimere edges
            for (AdductNode u : compatibilityNetwork[c].subnodes) {
                for (AdductEdge e : u.getEdges()) {
                    if (!e.isAdductEdge()) {
                        KnownMassDelta explanation = e.getExplanations()[0];
                        if (explanation instanceof LossRelationship) {
                            if (e.getLeft()==u) {
                                assignments[indizes.get(u.getIndex())] = assignments[indizes.get(e.getRight().getIndex())].addInsource(((LossRelationship) explanation).getFormula());
                            } else {
                                assignments[indizes.get(u.getIndex())] = assignments[indizes.get(e.getLeft().getIndex())].addInsource(((LossRelationship) explanation).getFormula().negate());
                            }
                        } else if (explanation instanceof MultimereRelationship) {
                            if (e.getLeft()==u) {
                                assignments[indizes.get(u.getIndex())] = assignments[indizes.get(e.getRight().getIndex())].multiplyMultimere(((MultimereRelationship) explanation).getMultiplicator());
                            } else {
                                assignments[indizes.get(u.getIndex())] = assignments[indizes.get(e.getLeft().getIndex())].multiplyMultimere(1/(((MultimereRelationship) explanation).getMultiplicator()));
                            }
                        }
                    }
                }
            }

         */
        // we might have multimeres of type M*0.25 or insource fragments with negative formula. We have to resolve them by adding an offset
        float offsetMultiplicator = 1;
        MolecularFormula offsetFormula = MolecularFormula.emptyFormula();
        for (AdductNode u : subnetwork) {
            IonType type = assignments.get(u.getIndex());
            offsetMultiplicator = Math.min(offsetMultiplicator, type.getMultimere());
            if (!type.getInsource().isAllPositiveOrZero()) {
                offsetFormula = type.getInsource().negate().union(offsetFormula);
            }
        }
        if (offsetMultiplicator != 1 || !offsetFormula.isEmpty()) {
            offsetMultiplicator = 1 / offsetMultiplicator;
            for (AdductNode u : subnetwork) {
                assignments.put(u.getIndex(), assignments.get(u.getIndex()).multiplyMultimere(offsetMultiplicator).addInsource(offsetFormula));
            }
        }
        return Arrays.stream(subnetwork).map(x->new AdductAssignment(new IonType[]{assignments.get(x.getIndex())}, new double[]{1d})).toArray(AdductAssignment[]::new);

    }

    private void spreadMultimere(AdductNode[] nodes, Int2ObjectOpenHashMap<IonType> previouslyAssigned) {
        final Int2ObjectOpenHashMap<IonType> assigned = new Int2ObjectOpenHashMap();
        ArrayList<AdductNode> stack = new ArrayList<>();
        for (int k = 0; k < nodes.length; ++k) {
            if (!assigned.containsKey(nodes[k].getIndex())) {
                stack.add(nodes[k]);
                IonType init = previouslyAssigned.get(nodes[k].getIndex());
                assigned.put(nodes[k].getIndex(), init);
                while (!stack.isEmpty()) {
                    AdductNode u = stack.remove(stack.size() - 1);
                    IonType ut = assigned.get(u.getIndex());
                    for (AdductEdge e : u.getEdges()) {
                        for (KnownMassDelta m : e.getExplanations()) {
                            if (m instanceof MultimereRelationship) {
                                AdductNode v = e.getOther(u);
                                IonType ot = assigned.get(v.getIndex());
                                if (ot == null) {
                                    float f = ((MultimereRelationship) m).getMultiplicator();
                                    if (u == e.getRight()) f = 1f/f;
                                    ot = previouslyAssigned.get(v.getIndex()).withMultimere(ut.getMultimere() * f);
                                    assigned.put(v.getIndex(), ot);
                                    stack.add(v);
                                }
                            }
                        }
                    }
                }
            }
        }
        previouslyAssigned.putAll(assigned);
    }

    private void spreadInsource(AdductNode[] nodes, Int2ObjectOpenHashMap<IonType> previouslyAssigned) {
        final Int2ObjectOpenHashMap<IonType> assigned = new Int2ObjectOpenHashMap();
        ArrayList<AdductNode> stack = new ArrayList<>();
        for (int k = 0; k < nodes.length; ++k) {
            if (!assigned.containsKey(nodes[k].getIndex())) {
                stack.add(nodes[k]);
                IonType init = previouslyAssigned.get(nodes[k].getIndex());
                assigned.put(nodes[k].getIndex(), init);
                while (!stack.isEmpty()) {
                    AdductNode u = stack.remove(stack.size() - 1);
                    IonType ut = assigned.get(u.getIndex());
                    for (AdductEdge e : u.getEdges()) {
                        for (KnownMassDelta m : e.getExplanations()) {
                            if (m instanceof LossRelationship) {
                                AdductNode v = e.getOther(u);
                                IonType ot = assigned.get(v.getIndex());
                                if (ot == null) {
                                    MolecularFormula f = ((LossRelationship) m).getFormula();
                                    if (u == e.getLeft()) f = f.negate();
                                    ot = previouslyAssigned.get(v.getIndex()).withInsource(ut.getInsource().add(f));
                                    assigned.put(v.getIndex(), ot);
                                    stack.add(v);
                                }
                            }
                        }
                    }
                }
            }
        }
        previouslyAssigned.putAll(assigned);
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

    private AdductAssignment[] beamSearch(AdductNode[] subnetwork, CompatibilityNode[] nodes, int charge) {
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
        // 3.) return best results
        return mergeTopResults(adductBeamSearch.getTopSolutions(), subnetwork, nodes, charge);
    }

    private AdductAssignment[] mergeTopResults(AdductBeamSearch.MatchNode[] topSolutions, AdductNode[] subnetwork, CompatibilityNode[] nodes, int charge) {
        if (topSolutions.length == 0) return null;
        double topScore = topSolutions[0].score();
        final double threshold = topScore - 3;
        topSolutions = Arrays.stream(topSolutions).takeWhile(x -> x.score() >= threshold).toArray(AdductBeamSearch.MatchNode[]::new);
        if (topSolutions.length == 1)
            return resolveCompatibilityNetwork(subnetwork, nodes, topSolutions[0].assignment(), charge);

        AdductAssignment[][] assignments = Arrays.stream(topSolutions).map(x -> resolveCompatibilityNetwork(subnetwork, nodes, x.assignment(), charge)).toArray(AdductAssignment[][]::new);
        double[] scores = Arrays.stream(topSolutions).mapToDouble(AdductBeamSearch.MatchNode::score).map(x -> Math.exp(x - topScore)).toArray();
        AdductAssignment[] merged = new AdductAssignment[subnetwork.length];
        for (int i = 0; i < subnetwork.length; ++i) {
            final int I = i;
            merged[i] = AdductAssignment.merge(charge, Arrays.stream(assignments).map(x -> x[I]).toArray(AdductAssignment[]::new), scores);
        }
        return merged;
    }


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
                        }
                    }

                }
            }
        }
        return nodes;
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
