

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

package de.unijena.bioinf.fingerid.pvalues;

import de.unijena.bioinf.graphUtils.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ConditionalProbabilitiesEstimator {

    private final static double PSEUDO_COUNTS = 0.5f;
    private FingerprintTree tree;

    public ConditionalProbabilitiesEstimator(FingerprintTree tree) {
        this.tree = tree;
    }

    public void estimateFromFile(File f) throws IOException {
        estimate(Main.readFingerprints(f));
    }

    public void fitByCorrelation(Iterator<boolean[]> fingerprints) {
        final List<Tree<FPVariable>> variables = tree.nodes.subList(0, tree.nodes.size() - 1);

        for (Tree<FPVariable> node : tree.nodes) {
            FPVariable v = node.getLabel();
            //v.oo = v.II = v.oI = v.Io = v.frequency = 0d;
            //v.o = v.I = Probability.ZERO;
            v.frequency=PSEUDO_COUNTS;
        }


        final FPVariable root = tree.root.getLabel();
        int intcounter=0;
        while (fingerprints.hasNext()) {
            ++intcounter;
            final boolean[] fp = fingerprints.next();
            if (fp[root.to]) root.frequency += 1;
            for (Tree<FPVariable> node : variables) {
                final FPVariable uv = node.getLabel();
                if (fp[uv.from]) {
                    if (fp[uv.to]) {
                        ++uv.frequency;
                    } else {
                    }
                } else {
                    if (fp[uv.to]) {
                        ++uv.frequency;
                    } else {
                    }
                }
            }
        }
        final double counter = intcounter+PSEUDO_COUNTS*4;
        for (Tree<FPVariable> node : tree.nodes) {
            node.getLabel().frequency /= counter;
        }
        for (Tree<FPVariable> node : variables) {
            final FPVariable uv = node.getLabel();
            final FPVariable parent = tree.varMap.get(uv.from);
            // calculate correlation: X=child, Y=parent
            final double expX = uv.frequency;
            final double expY = parent.frequency;

            final double II = uv.correlation * (expX-expX*expX) * (expY-expY*expY) + expX*expY;
            final double Io = uv.frequency - II;
            final double oI = parent.frequency - II;
            final double oo = 1d - (II+Io+oI);

            uv.II = II;
            uv.Io = Io;
            uv.oI = oI;
            uv.oo = oo;

            uv.calculateProbabilities();
        }
    }

    public void estimate(Iterator<boolean[]> fingerprints) {
        for (Tree<FPVariable> node : tree.nodes) {
            final FPVariable variable = node.getLabel();
            variable.oo=variable.oI=variable.Io=variable.II=variable.frequency=PSEUDO_COUNTS;
        }
        final List<Tree<FPVariable>> variables = tree.nodes.subList(0, tree.nodes.size() - 1);
        final FPVariable root = tree.root.getLabel();
        int intcounter=0;
        while (fingerprints.hasNext()) {
            ++intcounter;
            final boolean[] fp = fingerprints.next();
            if (fp[root.to]) root.frequency += 1;
            for (Tree<FPVariable> node : variables) {
                final FPVariable uv = node.getLabel();
                if (fp[uv.from]) {
                    if (fp[uv.to]) {
                        ++uv.frequency;
                        ++uv.II;
                    } else {
                        ++uv.oI;
                    }
                } else {
                    if (fp[uv.to]) {
                        ++uv.frequency;
                        ++uv.Io;
                    } else {
                        ++uv.oo;
                    }
                }
            }
        }
        final double counter = intcounter+PSEUDO_COUNTS*4;
        for (Tree<FPVariable> node : variables) {
            final FPVariable uv = node.getLabel();
            final FPVariable parent = tree.varMap.get(uv.from);
            assert (int)(uv.oo+uv.Io+uv.II+uv.oI) == (int)counter;
            // calculate correlation: X=child, Y=parent
            final double expX = uv.frequency/counter;
            final double expY = parent.frequency/counter;
            final double expXY = uv.II / counter;
            uv.correlation = (expXY - expX*expY) / (Math.sqrt(expX-expX*expX) * Math.sqrt(expY - expY*expY));
            uv.oo /= counter;
            uv.Io /= counter;
            uv.oI /= counter;
            uv.II /= counter;
            uv.frequency /= counter;
            uv.calculateProbabilities();
        }
    }

}
