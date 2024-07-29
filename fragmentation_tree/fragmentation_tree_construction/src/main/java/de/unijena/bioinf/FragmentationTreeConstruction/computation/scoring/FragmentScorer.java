/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A scorer for each fragment node in the graph
 */
public interface FragmentScorer<T> {

    public T prepare(ProcessedInput input, AbstractFragmentationGraph graph);

    public static Optional<Fragment> getDecompositionRootNode(AbstractFragmentationGraph graph) {
        if (graph instanceof FTree) {
            return Optional.of(graph.getRoot()); // todo: what is with in-source fragments
        } else {
            if (graph.getRoot().getOutDegree()>1) {
                LoggerFactory.getLogger(FragmentScorer.class).warn("Cannot score root for graph with multiple roots.");
                return Optional.empty();
            }
            return Optional.of(graph.getRoot().getChildren(0));
        }
    }

    public double score(Fragment graphFragment, ProcessedPeak correspondingPeak, boolean isRoot, T prepared);

}
