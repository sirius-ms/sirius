

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

package de.unijena.bioinf.babelms.utils;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class FTGraphWriter {

    public void writeGraph(FGraph graph, Writer writer) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("#graph\t");
        buffer.append(graph.getRoot().getChildren(0).getFormula().toString());
        buffer.append('\t');
        buffer.append(String.valueOf(graph.numberOfVertices()));
        buffer.append('\t');
        buffer.append(String.valueOf(graph.numberOfEdges()));
        buffer.append('\t');
        buffer.append(String.valueOf(graph.maxColor()));
        buffer.append('\n');
        for (Fragment f : graph) {
            buffer.append(f.getVertexId());
            buffer.append('\t');
            buffer.append(f.getColor());
            buffer.append('\n');
        }
        for (Loss l : graph.losses()) {
            buffer.append(l.getSource().getVertexId());
            buffer.append('\t');
            buffer.append(l.getTarget().getVertexId());
            buffer.append('\t');
            buffer.append(l.getWeight());
            buffer.append('\n');
        }
        writer.write(buffer.toString());
    }

    public void writeGraphToFile(File file, FGraph graph) throws IOException {
        try(final FileWriter fw = new FileWriter(file)) {
            writeGraph(graph, fw);
        }
    }

}
