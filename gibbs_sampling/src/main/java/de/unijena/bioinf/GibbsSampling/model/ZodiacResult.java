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

package de.unijena.bioinf.GibbsSampling.model;

public class ZodiacResult<C extends Candidate<?>> {
    private String[] ids;
    private Graph<C> graph;
    private CompoundResult<C>[] results;

    public ZodiacResult(String[] ids, Graph<C> graph, CompoundResult<C>[] results) {
        this.ids = ids;
        this.graph = graph;
        this.results = results;
    }

    public String[] getIds() {
        return ids;
    }

    public Graph<C> getGraph() {
        return graph;
    }

    public CompoundResult<C>[] getResults() {
        return results;
    }
}
