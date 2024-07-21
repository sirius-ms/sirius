/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.annotations;

import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Simple and easy serializable fragmentation tree model with annotated fragments/nodes abd losses/edges
 * Root fragment has index 0;
 */
@Getter
@Setter
@Builder
public class FragmentationTree {
    protected List<FragmentNode> fragments;
    protected List<LossEdge> losses;
    Double treeScore;

    public static FragmentationTree fromFtree(FTree sourceTree) {
        final List<LossEdge> lossEdges = new ArrayList<>();
        final Int2IntMap fragmentIdToIndex = new Int2IntOpenHashMap();
        final AtomicInteger idx = new AtomicInteger(0);

        FragmentationTreeBuilder treeBuilder = FragmentationTree.builder()
                .treeScore(sourceTree.getTreeWeight())

                .fragments(sourceTree.getFragments().stream().sorted(Comparator.comparing(Fragment::getVertexId)).map(f -> {
                    final FragmentNode fn = new FragmentNode();
                    fn.setFragmentId(f.getPeakId());
                    fn.setMolecularFormula(f.getFormula().toString());
                    fn.setIonType(f.getIonization().toString());

                    {
                        final FragmentAnnotation<Peak> peakInfo = sourceTree.getFragmentAnnotationOrThrow(Peak.class);
                        fn.setMz(peakInfo.get(f).getMass());
                        fn.setIntensity(peakInfo.get(f).getIntensity());
                    }

                    {
                        final FragmentAnnotation<AnnotatedPeak> anoPeak = sourceTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
                        if (anoPeak.get(f).isMeasured()) {
                            Deviation dev = sourceTree.getMassError(f);
                            fn.setMassDeviationDa(dev.getAbsolute());
                            fn.setMassDeviationPpm(dev.getPpm());
                        }
                    }

                    {
                        final FragmentAnnotation<Score> scores = sourceTree.getFragmentAnnotationOrThrow(Score.class);
                        fn.setScore(scores.get(f).sum());
                    }

                    fragmentIdToIndex.put(fn.getFragmentId(), idx.getAndIncrement());
                    return fn;
                }).toList());


        for (Loss l : sourceTree.losses()) {
            LossEdge loss = LossEdge.builder()
                    .sourceFragmentIdx(fragmentIdToIndex.get(l.getSource().getVertexId()))
                    .targetFragmentIdx(fragmentIdToIndex.get(l.getTarget().getVertexId()))
                    .molecularFormula(l.getFormula().toString())
                    .score(l.getWeight())
                    .build();
            lossEdges.add(loss);
        }

        treeBuilder.losses(lossEdges);

        return treeBuilder.build();
    }
}