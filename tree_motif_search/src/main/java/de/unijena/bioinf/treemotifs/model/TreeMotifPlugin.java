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

package de.unijena.bioinf.treemotifs.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Beautified;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.GeneralGraphScorer;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class TreeMotifPlugin extends SiriusPlugin {


    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addGeneralGraphScorer(new TreeMotifScorer());
    }

    protected final HashMap<File, TreeMotifDB> treeMotifDB = new HashMap<>();

    @Called("motif-search")
    public static class TreeMotifScorer implements GeneralGraphScorer {

        @Override
        public double score(AbstractFragmentationGraph graph, ProcessedInput input) {
            return graph.getAnnotation(TreeMotifAnnotation.class, TreeMotifAnnotation::new).score;
        }
    }

    public static class TreeMotifAnnotation implements TreeAnnotation {
        private final double score;

        public TreeMotifAnnotation() {
            this(0d);
        }

        public TreeMotifAnnotation(double score) {
            this.score = score;
        }
    }

    @Override
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
        super.releaseTreeToUser(input, graph, tree);
        if (input.getAnnotation(Beautified.class, Beautified::ugly).isInProcess()) {
            return;
        }
        String motifDBString = input.getExperimentInformation().getAnnotation(MotifDbFile.class, MotifDbFile::new).value;
        if (motifDBString !=null) {
            final File motifDB = new File(motifDBString);
            if (treeMotifDB!=null) {
                synchronized (treeMotifDB) {
                    treeMotifDB.computeIfAbsent(motifDB, (x)-> {
                        try {
                            if (!x.exists() || !x.isFile())
                                return null;
                            LoggerFactory.getLogger(TreeMotifPlugin.class).warn("Load motif plugin");
                            return TreeMotifDB.readFromFile(x);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    });
                }

                final TreeMotifDB db = treeMotifDB.get(motifDB);
                if (db!=null) {
                    MotifMatch motifMatch = tree.numberOfVertices()>=5 ? db.searchInLibraryTopK(tree, 10) : null;
                    if (motifMatch!=null) {
                        //System.out.println("Add " + (motifMatch.getTotalProbability()) + " to total score for " + tree.getRoot().getFormula() + " ( " +  motifMatch.matchingFragments.length + " frags and " + motifMatch.matchingRootLosses.length + " losses ) with sets are " + Arrays.toString(db.getMatchingFragments(motifMatch)) + " and " + Arrays.toString(db.getMatchingRootLosses(motifMatch)));
                        tree.setTreeWeight(tree.getTreeWeight() + motifMatch.getTotalProbability());
                        tree.setAnnotation(TreeMotifAnnotation.class, new TreeMotifAnnotation(motifMatch.getTotalProbability()));
                    }
                }

            }
        }

    }
}
