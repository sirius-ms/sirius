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

package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.IonizedMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RerootingTreeMethod {

    public RerootedTree randomlySelectRerootedTree(FTree tree) {
        final RerootedTree[] trees = computeAllRerootedTrees(tree);
        final double[] probabilities = new double[trees.length];
        double totalProb = 0d;
        for (int k=0; k < probabilities.length; ++k) {
            probabilities[k] = 1d / (1d+trees[k].numberOfRegrafts);
        }
        for (int k=0; k < probabilities.length; ++k) {
            probabilities[k] /= totalProb;
        }
        final double drawn = Math.random();
        int sel = 0;
        for (int k=1; k < probabilities.length; ++k) {
            if (drawn < probabilities[k]) break;
        }
        return trees[sel];
    }

    public RerootedTree[] computeAllRerootedTrees(FTree tree) {
        RerootedTree[] trees = new RerootedTree[tree.numberOfVertices()-1];
        int k=0;
        for (Fragment f : tree) {
            if (!f.isRoot()) {
                final ArrayList<LEdge> rer = rerootUpDown(tree, f,null);
                trees[k++] = populateNodes(tree,f, rer, tree.getRoot().getFormula(), tree.getRoot().getIonization());
            }
        }
        return trees;
    }

    private RerootedTree populateNodes(FTree original, Fragment originalVertex, ArrayList<LEdge> rer, MolecularFormula formula, Ionization ionization) {
        final RerootingForest forest = new RerootingForest(new FTree(formula, ionization));
        forest.tree.setAnnotation(PrecursorIonType.class, original.getAnnotationOrThrow(PrecursorIonType.class));
        for (LEdge l : rer) {
            forest.insert(l);
        }
        forest.tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class).set(forest.tree.getRoot(), original.getFragmentAnnotationOrThrow(AnnotatedPeak.class).get(originalVertex).withFormula(formula).withIonization(ionization));
        final int tsize = forest.tree.numberOfEdges();
        forest.insertAllOrphans();
        // if there are subtrees left, search for a node to insert them
        int numberOfRegrafts = original.numberOfEdges() - tsize;
        return new RerootedTree(original, forest.tree, numberOfRegrafts);
    }

    private ArrayList<LEdge> reroot(FTree tree, Fragment vertex, Fragment skip) {
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final ArrayList<LEdge> rootLosses = new ArrayList<>();
        // reroot children
        for (int i=0; i < vertex.getOutDegree(); ++i) {
            final Loss l = vertex.getOutgoingEdge(i);
            if (l.getTarget()!=skip) {
                LEdge e = new LEdge(l,peakAno.get(l.getTarget()));
                rootLosses.add(e);
                e.childs.addAll(reroot(tree, l.getTarget(),null));
            }
        }
        return rootLosses;
    }

    private ArrayList<LEdge> rerootUpDown(FTree tree, Fragment vertex, Fragment skip) {
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final ArrayList<LEdge> losses = new ArrayList<>();
        losses.addAll(reroot(tree, vertex,skip));
        if (!vertex.isRoot()) {
            Fragment parent = vertex.getParent();
            final Loss l = vertex.getIncomingEdge();
            LEdge e = new LEdge(l, peakAno.get(l.getTarget()));
            e.childs.addAll(rerootUpDown(tree, parent, vertex));
            losses.add(e);
        }
        return losses;
    }

    protected static class RerootingForest {
        private final List<LEdge> orphans;
        private final FTree tree;
        private final Set<IonizedMolecularFormula> formulas;

        public RerootingForest(FTree tree) {
            this.orphans = new ArrayList<>();
            this.tree = tree;
            this.formulas = new HashSet<>();
        }

        public void insert(LEdge edge) {
            insert(edge, tree.getRoot());
        }

        private void insert(LEdge edge, Fragment node) {
            final MolecularFormula l = node.getFormula().subtract(edge.formula);
            final Ionization ion = edge.adductSwitch==null ? node.getIonization() : edge.adductSwitch;
            if (l.isAllPositiveOrZero() && l.getMass()>0 && formulas.add(new IonizedMolecularFormula(l, ion))) {
                final Fragment v = tree.addFragment(node, l, ion);
                final AnnotatedPeak peak = edge.peak;
                tree.getOrCreateFragmentAnnotation(AnnotatedPeak.class).set(v, peak.withFormula(v.getFormula()));
                for (LEdge child : edge.childs) {
                    insert(child,v);
                }
            } else {
                orphans.add(edge);
            }
        }

        public void insertAllOrphans() {
            final Random random = new Random();
            int tries = 0;
            while (!orphans.isEmpty()) {
                ArrayList<LEdge> todo = new ArrayList<>(orphans);
                orphans.clear();
                for (LEdge edge : todo) {
                    final ArrayList<Fragment> fragments = new ArrayList<>();
                    for (Fragment f : tree) {
                        final IonizedMolecularFormula sub = new IonizedMolecularFormula(f.getFormula().subtract(edge.formula),edge.adductSwitch!=null ? edge.adductSwitch : f.getIonization());
                        if (sub.getFormula().isAllPositiveOrZero() && sub.getFormula().getMass()>0 && !formulas.contains(sub)) {
                            fragments.add(f);
                        }
                    }
                    if (fragments.isEmpty()) {
                        LoggerFactory.getLogger(RerootingTreeMethod.class).warn("Cannot regraft subtree. The formula " + edge.formula + " from the subtree does not fit into any node of the tree.");
                        // try again
                        orphans.add(edge);
                        continue;
                    }
                    final Fragment selected = fragments.get(random.nextInt(fragments.size()));
                    insert(edge, selected);
                }
                if (++tries >= 30) {
                    LoggerFactory.getLogger(RerootingTreeMethod.class).warn("Could not insert subtree into decoy after 10 iterations. This is unusual but not necessarily a bug.");
                    break;
                }
            }
        }
    }

    protected static class LEdge {
        private final MolecularFormula formula;
        private final Ionization adductSwitch;
        private final AnnotatedPeak peak;
        private final ArrayList<LEdge> childs;

        public LEdge(Loss l, AnnotatedPeak peak) {
            this(l.getFormula(), peak, l.getSource().getIonization().equals(l.getTarget().getIonization()) ? null :  l.getTarget().getIonization());
        }

        private LEdge(MolecularFormula formula, AnnotatedPeak peak, Ionization adductSwitch) {
            this.formula = formula;
            this.adductSwitch = adductSwitch;
            this.childs = new ArrayList<>();
            this.peak = peak;
        }

        public MolecularFormula getSubtreeFormula() {
            MolecularFormula f = MolecularFormula.emptyFormula();
            for (LEdge l : childs) f = f.union(l.getSubtreeFormula());
            return f.add(formula);
        }

        public int subtreeSize() {
            int sum = 1;
            for (LEdge e : childs)  {
                sum += e.subtreeSize();
            }
            return sum;
        }
    }

}
