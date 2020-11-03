

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

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotParser {

    private final static Pattern pattern = Pattern.compile("^\\s*(\\d+)\\s*->\\s*(\\d+)");

    public FingerprintTree parseFromFile(File f) throws IOException {
        final FileReader fileReader = new FileReader(f);
        try {
            return parse(fileReader);
        } finally {
            fileReader.close();
        }
    }

    public FingerprintTree parse(Reader reader) throws IOException {
        final BufferedReader r = new BufferedReader(reader);
        final HashMap<Integer, Integer> edges = new HashMap<>();
        final HashSet<Integer> nodes = new HashSet<>();

        String line;
        while ((line=r.readLine())!=null) {
            if (line.contains("->")) {
                final Matcher m = pattern.matcher(line);
                if (m.find()) {
                    final Integer u = Integer.parseInt(m.group(1));
                    final Integer v = Integer.parseInt(m.group(2));
                    nodes.add(u);
                    nodes.remove(v);
                    if (edges.containsKey(v)) {
                        throw new RuntimeException("Cycle detected! Graph is not a tree!");
                    }
                    edges.put(v, u);
                }
            }
        }
        for (Map.Entry<Integer, Integer> e : edges.entrySet()) {
            nodes.remove(e.getKey());
        }
        if (nodes.size() > 1) {
            System.err.println("Warning: Tree has more than one root. Add artificial edges to connect the forest.");
            final Iterator<Integer> nodeIter = nodes.iterator();
            final Integer root = nodeIter.next();
            while (nodeIter.hasNext()) {
                edges.put(nodeIter.next(), root);
                nodeIter.remove();
            }
        } else if (nodes.size()==0) {
            throw new RuntimeException("Cycle detected! Graph is not a tree!");
        }
        final Integer rootVertex = nodes.iterator().next();
        return buildTree(rootVertex, edges);
    }

    private FingerprintTree buildTree(Integer rootVertex, HashMap<Integer, Integer> edges) {
        // step 1: create variables
        final HashMap<Integer, Tree<FPVariable>> nodeMap = new HashMap<>();
        nodeMap.put(rootVertex, new Tree<FPVariable>(new FPVariable(-1, rootVertex)));
        for (Map.Entry<Integer, Integer> entry : edges.entrySet()) {
            nodeMap.put(entry.getKey(), new Tree<FPVariable>(new FPVariable(entry.getValue(), entry.getKey())));
        }
        for (Map.Entry<Integer, Integer> entry : edges.entrySet()) {
            nodeMap.get(entry.getValue()).addChild(nodeMap.get(entry.getKey()));
        }
        return new FingerprintTree(nodeMap.get(rootVertex));
    }

}
