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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;

import java.util.*;
import java.util.stream.Collectors;

public class FragTree {
    protected List<FragmentNode> fragments;
    protected List<LossEdge> losses;
    double treeScore;
    FragmentNode root;


    public List<FragmentNode> getFragments() {
        return fragments;
    }

    public void setFragments(List<FragmentNode> fragments) {
        this.fragments = fragments;
    }

    public List<LossEdge> getLosses() {
        return losses;
    }

    public void setLosses(List<LossEdge> losses) {
        this.losses = losses;
    }

    public double getTreeScore() {
        return treeScore;
    }

    public void setTreeScore(double treeScore) {
        this.treeScore = treeScore;
    }

    public FragmentNode getRoot() {
        return root;
    }

    public void setRoot(FragmentNode root) {
        this.root = root;
    }

    public static FragTree fromFtree(FTree sourceTree) {
        DescriptorRegistry registry = DescriptorRegistry.getInstance();

        final FragTree tree = new FragTree();
        Map<Integer, FragmentNode> fragmentNodes = new HashMap<>();
        List<LossEdge> lossEdges = new ArrayList<>();

        tree.setTreeScore(sourceTree.getTreeWeight());

        for (Fragment f : sourceTree.getFragments()) {
            final FragmentNode fn = new FragmentNode();
            fn.setId(f.getVertexId());
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
                    fn.setMassErrorPpm(dev.getPpm());
                }
            }

            {
                final FragmentAnnotation<Score> scores = sourceTree.getFragmentAnnotationOrThrow(Score.class);
                fn.setScore(scores.get(f).sum());
            }

            fragmentNodes.put(fn.getId(), fn);
        }

        tree.setFragments(fragmentNodes.values().stream().sorted(Comparator.comparing(FragmentNode::getId)).collect(Collectors.toList()));
        tree.setRoot(fragmentNodes.get(0));

        for (Loss l : sourceTree.losses()) {
            LossEdge loss = new LossEdge();
            loss.setSourceFragment(fragmentNodes.get(l.getSource().getVertexId()));
            loss.setTargetFragment(fragmentNodes.get(l.getTarget().getVertexId()));
            loss.setMolecularFormula(l.getFormula().toString());
            loss.setScore(l.getWeight());
            lossEdges.add(loss);
        }
        tree.setLosses(lossEdges);

        return tree;
    }
}