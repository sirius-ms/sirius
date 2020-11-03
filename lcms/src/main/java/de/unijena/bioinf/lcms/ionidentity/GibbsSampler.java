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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class GibbsSampler {
    protected Set<PrecursorIonType> commonTypes;
    public GibbsSampler() {
        this.commonTypes =  new HashSet<>();
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"));
        commonTypes.add(PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"));

    }

    public void sample(IonNode subnetwork) {
        ArrayList<IonNode> nodes = new ArrayList<>();
        spread(nodes, subnetwork);
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
        gibbsSampling(nodes, assignment, null, 100,0);
        // now record it
        int repetitions = 2000;
        int recordEvery = 2;
        int totalSamples = repetitions/recordEvery;
        gibbsSampling(nodes, assignment, posteriorCount, repetitions,recordEvery);
        // compute marginals
        for (int i=0; i < nodes.size(); ++i) {
            for (int j=0; j < posteriorCount[i].length; ++j) {
                nodes.get(i).assignment.probabilities[j] = ((float)posteriorCount[i][j]) / totalSamples;
            }
            System.out.println(nodes.get(i).assignment);
        }
    }

    private void gibbsSampling(ArrayList<IonNode> nodes, int[] assignment, int[][] counting, int repetitions, int recordEvery) {
        double[][] buf = new double[nodes.size()][];
        final Random r = new Random();
        for (int k=0; k < buf.length; ++k) buf[k] = new double[nodes.get(k).assignment.probabilities.length];
        for (int k=0; k < repetitions; ++k) {
            for (int i = 0; i < assignment.length; ++i) {
                double[] probs = buf[i];
                double total = 0;
                for (int j=0; j < probs.length; ++j) {
                    assignment[i] = j;
                    probs[j] = probability(nodes, assignment);
                    total += probs[j];
                }
                final double randomNumber = r.nextDouble()*total;
                for (int j=0; j < probs.length; ++j) {
                    if (probs[j]>randomNumber) {
                        assignment[i] = j;
                        break;
                    }
                }
            }
            if (counting!=null && k%recordEvery == 0) {
                for (int i=0; i < assignment.length; ++i) {
                    ++counting[i][assignment[i]];
                }
            }
        }
    }


    private double probability(ArrayList<IonNode> nodes, int[] assignment) {
        double totalProbability = 0f;
        for (int k=0; k < assignment.length; ++k) {
            totalProbability += nodes.get(k).assignment.probabilities[assignment[k]];
        }
        return Math.exp(totalProbability);
    }

    private void spread(ArrayList<IonNode> nodes, IonNode initial) {
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
            double total = 0;
            for (int k=0; k < ionTypes.length; ++k) {
                final PrecursorIonType ionType = ionTypes[k];
                if (ionType.isIonizationUnknown()) {
                    probs[k] += 1 + node.getFeature().getNumberOfIntensiveFeatures();
                } else {
                    probs[k] += prior(ionType) * node.neighbours.stream().filter(n->ionType.equals(n.fromType)).mapToInt(e->e.totalNumberOfCorrelatedPeaks).sum();
                }

                total += probs[k];
            }
            for (int k=0; k < probs.length; ++k) {
                probs[k] /= total;
                probs[k] = Math.log(probs[k]);
            }
            node.assignment = new IonAssignment(ionTypes,probs);

            nodes.add(node);
            for (Edge e : node.neighbours) {
                if (e.to.assignment==null) {
                    stack.add(e.to);
                }
            }
        }
    }

    // okay, it seems that we have WAY too many adducts, so we need a prior

    private double prior(PrecursorIonType ionType) {
        //if (ionType.hasNeitherAdductNorInsource()) return 1d;
        return 1d;
    }

}
