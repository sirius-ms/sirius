package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

class GibbsSampler {

    protected double p=0.5;

    public void learnP(IonNetwork network) {
        int sum = 0;
        int n=0;
        for (IonNode node : network.nodes) {
            for (Edge e : node.neighbours) {
                if (e.deltaMz()>=0) {
                    sum += e.numberOfCommonSamples()-1;
                    ++n;
                }
            }
        }
        this.p = (((double)n)/sum);
        System.out.println("p = " + p);
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
        int recordEvery = 4;
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

    private void spread(ArrayList<IonNode> nodes, IonNode node) {
        final PrecursorIonType[] ionTypes = node.possibleIonTypes().toArray(PrecursorIonType[]::new);
        final double[] probs = new double[ionTypes.length];
        double total = 0;
        for (int k=0; k < ionTypes.length; ++k) {
            final PrecursorIonType ionType = ionTypes[k];
            probs[k] = node.neighbours.stream().filter(n->ionType.equals(n.fromType)).mapToInt(Edge::numberOfCommonSamples).reduce(Integer::sum).orElse(0);
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
                spread(nodes, e.to);
            }
        }
    }

}
