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
import de.unijena.bioinf.lcms.LCMSProccessingInstance;

import java.util.*;

class GibbsSampler {
    protected Set<PrecursorIonType> commonTypes;
    public GibbsSampler(LCMSProccessingInstance instance) {
        this.commonTypes =  new HashSet<>();
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+Na]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+H]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+K]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M-H]-"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+Cl]-"));
        this.LAMBDA = Math.sqrt(instance.getSamples().size());
    }

    private double LAMBDA;

    public void sample(IonNode subnetwork) {
        ArrayList<IonNode> nodes = new ArrayList<>();
        final ArrayList<Edge> edges = new ArrayList<>();
        spread(nodes, edges, subnetwork);
        final int[] assignment = new int[nodes.size()];
        final int[][] posteriorCount = new int[assignment.length][];
        int k=0; int conflicts = 0;
        for (IonNode node : nodes) {
            posteriorCount[k++] = new int[node.assignment.ionTypes.length];
            conflicts += node.assignment.ionTypes.length-1;
        }
        if (conflicts<=0) {
            // no conflicts. Normalize probabilities
            for (IonNode n : nodes) {
                if (n.assignment.probabilities.length>0)
                    n.assignment.probabilities[0] = 1d;
            }
            return;
        }
        // warm-up
        gibbsSampling(nodes, edges, null, 100,0);
        // now record it
        int repetitions = 10000;
        int recordEvery = 10;
        int totalSamples = repetitions/recordEvery;
        gibbsSampling(nodes, edges, posteriorCount, repetitions,recordEvery);
        // compute marginals
        for (int i=0; i < nodes.size(); ++i) {
            for (int j=0; j < posteriorCount[i].length; ++j) {
                nodes.get(i).assignment.probabilities[j] = ((float)posteriorCount[i][j]) / totalSamples;
            }
        }
    }

    private void gibbsSampling(ArrayList<IonNode> nodes, ArrayList<Edge> edges, int[][] counting, int repetitions, int recordEvery) {
        double[][] buf = new double[nodes.size()][];
        double[][] draw = new double[nodes.size()][];
        final Random r = new Random();
        for (int k=0; k < buf.length; ++k) {
            buf[k] = new double[nodes.get(k).assignment.probabilities.length];
            draw[k] = new double[nodes.get(k).assignment.probabilities.length];
        }

        double score = probability(nodes, edges);
        for (int k=0; k < repetitions; ++k) {
            // for each node
            for (int i = 0; i < nodes.size(); ++i) {
                IonNode node = nodes.get(i);
                double[] probs = buf[i], drw = draw[i];
                double max = Double.NEGATIVE_INFINITY;
                for (int j=0; j < probs.length; ++j) {
                    probs[j] = node.activeAssignment==j ? score : probabilityUpdate(node, j, score);
                    max = Math.max(probs[j], max);
                }
                double total = 0d;
                for (int j=0; j < probs.length; ++j) {
                    drw[j] = Math.exp(probs[j]-max);
                    total += drw[j];
                }
                double randomNumber = r.nextDouble()*total;
                for (int j=0; j < probs.length; ++j) {
                    if (drw[j]>randomNumber) {
                        node.activeAssignment = j;
                        break;
                    } else randomNumber -= drw[j];
                }
                score = probs[node.activeAssignment];
            }
            if (counting!=null && k%recordEvery == 0) {
                for (int i=0; i < nodes.size(); ++i) {
                    ++counting[i][nodes.get(i).activeAssignment];
                }
            }
        }
    }

    private double probabilityUpdate(IonNode ionNode, int newAssignment, double score) {
        PrecursorIonType newType = ionNode.assignment.ionTypes[newAssignment], oldType = ionNode.activeType();
        final int oldAssignment = ionNode.activeAssignment;
        ionNode.activeAssignment = newAssignment;
        double newScore = 0d, oldScore = 0d;
        if (newType.isIonizationUnknown()) newScore += ionNode.priorForUnknownIonType;
        if (commonTypes.contains(newType)) newScore += IonNode.priorForCommonIonType;
        else newScore += IonNode.priorForUncommonIonType;
        if (!newType.hasNeitherAdductNorInsource()) newScore += ionNode.priorForAdductsAndInsource;
        for (Edge e : ionNode.neighbours) {
            newScore += e.score *compatibilityScore(e);
        }
        ionNode.activeAssignment = oldAssignment;
        if (oldType.isIonizationUnknown()) oldScore += ionNode.priorForUnknownIonType;
        if (commonTypes.contains(oldType)) oldScore += IonNode.priorForCommonIonType;
        else oldScore += IonNode.priorForUncommonIonType;
        if (!oldType.hasNeitherAdductNorInsource()) oldScore += ionNode.priorForAdductsAndInsource;
        for (Edge e : ionNode.neighbours) {
            oldScore += e.score *compatibilityScore(e);
        }
        return score - oldScore + newScore;
    }

    /**
     * Given a subnetwork and an adduct assignment, what is the log probability of
     * the network?
     **/
    private double probability(ArrayList<IonNode> nodes, ArrayList<Edge> edges) {
        double score = 0d;
        // node scores
        for (int i=0; i < nodes.size(); ++i) {
            final IonNode ionNode = nodes.get(i);
            if (ionNode.activeType().isIonizationUnknown()) {
                score += ionNode.priorForUnknownIonType;
            } else if (commonTypes.contains(ionNode.activeType())) {
                score += IonNode.priorForCommonIonType;
            } else {
                score += IonNode.priorForUncommonIonType;
            }
            if (!ionNode.activeType().hasNeitherAdductNorInsource())
                score += IonNode.priorForAdductsAndInsource;
        }
        // edge scores
        for (Edge e : edges) {
            score += e.score *compatibilityScore(e);
        }
        return score;
    }

    private void spread(ArrayList<IonNode> nodes, List<Edge> edges, IonNode initial) {
        final ArrayList<IonNode> stack = new ArrayList<>();
        stack.add(initial);
        while (!stack.isEmpty()) {
            IonNode node = stack.remove(stack.size()-1);
            if (node.assignment!=null) continue; // was already visited
            Set<PrecursorIonType> set = node.possibleIonTypes();
            // we always add the "unknown" ion type
            set.add(PrecursorIonType.unknown(node.getFeature().getRepresentativeIon().getPolarity()));
            final PrecursorIonType[] ionTypes = set.toArray(PrecursorIonType[]::new);
            final double[] probs = new double[ionTypes.length];
            node.assignment = new IonAssignment(ionTypes,probs);

            nodes.add(node);
            for (Edge e : node.neighbours) {
                if (e.to.assignment==null) {
                    stack.add(e.to);
                }
                // only add each edge once
                if (e.from.mz < e.to.mz) {
                    edges.add(e);
                }
            }
        }
    }

    private double compatibilityScore(Edge e) {
        if (e.from.activeType().isIonizationUnknown() ^ e.to.activeType().isIonizationUnknown() && (e.from.activeType().equals(e.fromType) || e.to.activeType().equals(e.toType)) ) {
            return 0.2;
        } else if (e.from.activeType().equals(e.fromType) && e.to.activeType().equals(e.toType)) return 1d;
        else return 0d;
    }

    private double prior(Edge edge) {
        return edge.score;
    }

}
