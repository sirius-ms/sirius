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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IntergraphMapping;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.concurrent.Callable;

public interface TreeBuilder {

    public FluentInterface computeTree();

    public Result computeTree(ProcessedInput input, FGraph graph, FluentInterface options);

    public boolean isThreadSafe();

    public static class FluentInterface {
        private final TreeBuilder treeBuilder;
        private final double minimalScore;
        private final double timeLimitsInSeconds;
        private final int numberOfCPUS;
        private final FTree template;
        private final Callable<Boolean> check;

        public FluentInterface(TreeBuilder treeBuilder) {
            this(treeBuilder, Double.NEGATIVE_INFINITY, 0, 1, null, null);
        }

        public FluentInterface(TreeBuilder treeBuilder, double minimalScore, double timeout, int numberOfCPUS, FTree template, Callable<Boolean> check) {
            this.treeBuilder = treeBuilder;
            this.minimalScore = minimalScore;
            this.timeLimitsInSeconds = timeout;
            this.numberOfCPUS = numberOfCPUS;
            this.template = template;
            this.check = check;
        }

        public double getMinimalScore() {
            return minimalScore;
        }

        public double getTimeLimitsInSeconds() {
            return timeLimitsInSeconds;
        }

        public int getNumberOfCPUS() {
            return numberOfCPUS;
        }

        public FTree getTemplate() {
            return template;
        }

        public Callable<Boolean> getInterruptionCheck() {
            return check;
        }

        public FluentInterface withInterruptionCheck(Callable<Boolean> r) {
            return new FluentInterface(treeBuilder, minimalScore, timeLimitsInSeconds, numberOfCPUS, template, r);
        }

        public FluentInterface withMinimalScore(double score) {
            return new FluentInterface(treeBuilder, score, timeLimitsInSeconds, numberOfCPUS, template, check);
        }

        public FluentInterface withTimeLimit(double seconds) {
            return new FluentInterface(treeBuilder, minimalScore, seconds, numberOfCPUS, template, check);
        }

        public FluentInterface withMultithreading(int numberOfCPUS) {
            return new FluentInterface(treeBuilder, minimalScore, timeLimitsInSeconds, numberOfCPUS, template, check);
        }

        public FluentInterface withTemplate(FTree tree) {
            return new FluentInterface(treeBuilder, minimalScore, timeLimitsInSeconds, numberOfCPUS, tree, check);
        }

        public Result solve(ProcessedInput input, FGraph graph) {
            return treeBuilder.computeTree(input,graph,this);
        }
    }

    public static enum AbortReason {
        COMPUTATION_CORRECT, // when everything is fine
        INFEASIBLE,     // should never happen
        TIMEOUT,        // is used when timeout reached
        NO_SOLUTION     // is used when no tree with reasonable score is found
    };

    public static class Result {

        public final boolean isOptimal;
        public final AbortReason error;
        public final FTree tree;
        public final IntergraphMapping mapping;

        public Result(FTree tree, boolean isOptimal, AbortReason error, IntergraphMapping mapping) {
            this.isOptimal = isOptimal;
            this.error = error;
            this.tree = tree;
            this.mapping = mapping;
        }
    }

}
