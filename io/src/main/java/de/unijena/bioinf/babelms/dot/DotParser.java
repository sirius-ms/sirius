
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

package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotParser<NodeType, EdgeType> {
    final static Pattern TOKENIZE = Pattern.compile("strict|(?:di)?graph|=|\"|\\|,|->|\\[|\\]|\\{|\\}|#|\n|;", Pattern.MULTILINE);
    private static int NOESCAPE = 0, QUOTED = 1, ESCAPED = 2;
    private MODE m = MODE.INITIAL;
    private Matcher scanner;
    private int lineNumber = 0;
    private int lastMatch;
    private StringBuilder source;
    private HashMap<String, NodeType> vertices = new HashMap<String, NodeType>();
    private NodeType currentVertex;
    private EdgeType currentEdge;
    private int escapeStatus = 0;
    private StringBuilder buffer = new StringBuilder();
    private String propertyName;
    private DotHandler<NodeType, EdgeType> handler;

    private DotParser(DotHandler<NodeType, EdgeType> handler) {
        this.handler = handler;
    }

    public static <NodeType, EdgeType> void parse(Reader input, DotHandler<NodeType, EdgeType> handler) throws IOException {
        DotParser p = new DotParser(handler);
        p.parseIt(input);
    }

    public static Graph parseGraph(Reader input) throws IOException {
        final Graph graph = new Graph();
        final DotHandler<Vertex, Edge> handler = new DotHandler<Vertex, Edge>() {
            @Override
            public Vertex addVertex(String name) {
                final Vertex v = new Vertex(name);
                graph.vertices.add(v);
                return v;
            }

            @Override
            public void addVertexProperty(Vertex node, String key, String value) {
                node.getProperties().put(key, value);
            }

            @Override
            public Edge addEdge(Vertex u, Vertex v) {
                final Edge e = new Edge(u.getName(), v.getName());
                graph.edges.add(e);
                return e;
            }

            @Override
            public void addEdgeProperty(Edge edge, String key, String value) {
                edge.getProperties().put(key, value);
            }
        };
        parse(input, handler);
        return graph;
    }

    private void parseIt(Reader input) throws IOException {
        final BufferedReader r = FileUtils.ensureBuffering(input);
        source = new StringBuilder();
        while (r.ready()) source.append(r.readLine()).append('\n');
        scanner = TOKENIZE.matcher(source);
        m = MODE.INITIAL;
        lastMatch = 0;
        while (scanner.find()) {
            String token = scanner.group();
            if (escapeStatus == QUOTED) {
                if (token.equals("\\")) {
                    buffer.append(before());
                    int now = scanner.end();
                    scanner.find();
                    int after = scanner.start();
                    if (after - now == 1) {
                        // escape character
                        buffer.append(source.subSequence(now, after));
                    } else buffer.append(source.subSequence(now - 1, after));
                } else if (token.equals("\"")) {
                    escapeStatus = NOESCAPE;
                    buffer.append(before());
                    lastMatch = scanner.end();
                    continue;
                } else {
                    buffer.append(before());
                    buffer.append(token);
                    lastMatch = scanner.end();
                    continue;
                }
            } else if (token.equals("\\")) {
                if (TOKENIZE.matcher(source.subSequence(scanner.end(), scanner.end() + 1)).find()) scanner.find();
                else error("Unexpected '\\'");

            }
            if (token.equals("#")) commentLine();
            token = ignoreWhitespaces();
            if (m == MODE.INITIAL) {
                if (token.equals("strict")) {
                    // ignore, boring
                } else if (token.contains("graph")) {
                    m = MODE.INGRAPH;
                    jumpTo("{");
                } else if (token.equals("\n")) {
                    ++lineNumber;
                } else {
                    error("Expect '(di)graph {' at beginning but '" + token + "' given");
                }
            } else if (m == MODE.INGRAPH) {
                if (token.equals("->")) {
                    // parse edge
                    String a = before().trim();
                    jumpTo("[");
                    String b = before().trim();
                    currentEdge = handler.addEdge(getVertex(a), getVertex(b));
                    m = MODE.INEDGE;
                } else if (token.equals("[")) {
                    // parse vertex
                    String vertexName = before().trim();
                    if (vertexName.equals("graph") || vertexName.equals("node") || vertexName.equals("edge")) {
                        // ignore this part
                        jumpTo("]");
                    }
                    currentVertex = handler.addVertex(vertexName);
                    vertices.put(vertexName, currentVertex);
                    m = MODE.INVERTEX;
                } else if (token.equals("}")) {
                    break;
                } else {

                }
            } else if (m == MODE.INVERTEX) {
                if (token.equals("]")) {
                    m = MODE.INGRAPH;
                    continue;
                }
                expect("=");
                String key = before().trim();
                propertyName = key;
                m = MODE.INVERTEXPROPERTY;
                clearBuffer();


            } else if (m == MODE.INEDGE) {
                if (token.equals("]")) {
                    m = MODE.INGRAPH;
                    continue;
                }
                expect("=");
                String key = before().trim();
                propertyName = key;
                m = MODE.INEDGEPROPERTY;
                clearBuffer();
            } else if (m == MODE.INVERTEXPROPERTY || m == MODE.INEDGEPROPERTY) {
                if (token.equals("\"")) {
                    escapeStatus = QUOTED;
                } else {
                    if (!token.equals(",") && !token.equals("]")) error("Expect ',' or ']' but '" + token + "' given.");
                    String propertyValue;
                    if (buffer.length() == 0) {
                        if (lastMatch > 0 && source.charAt(lastMatch - 1) == '=')
                            propertyValue = before();
                        else
                            propertyValue = "";
                    } else {
                        propertyValue = buffer.toString();
                        clearBuffer();
                    }
                    if (m == MODE.INEDGEPROPERTY)
                        handler.addEdgeProperty(currentEdge, propertyName, propertyValue);
                    else
                        handler.addVertexProperty(currentVertex, propertyName, propertyValue);
                    if (token.equals("]")) {
                        m = MODE.INGRAPH;
                    } else {
                        m = (m == MODE.INEDGEPROPERTY) ? MODE.INEDGE : MODE.INVERTEX;
                    }
                }
            }
            lastMatch = scanner.end();
        }
    }

    private void clearBuffer() {
        buffer.delete(0, buffer.length());
    }

    private void expect(String token) {
        if (!scanner.group().equals(token)) error("Expected '" + token + "'");
    }

    private NodeType getVertex(String name) {
        final NodeType av = vertices.get(name);
        if (av == null) error("Unknown vertex: '" + name + "'");
        return av;
    }

    private String before() {
        return source.substring(lastMatch, scanner.start());
    }

    private void error(String s) {
        throw new RuntimeException(lineNumber + ": " + s);
    }

    private void jumpTo(String token) throws IOException {
        lastMatch = scanner.end();
        while (scanner.find()) {
            final String tok = scanner.group();
            if (escapeStatus == 0 && tok.equals("#")) commentLine();
            if (tok.equals(token)) break;
            lastMatch = scanner.end();
        }
        if (!scanner.group().equals(token)) error("Expect: '" + token + "'");
    }

    private void commentLine() {
        while (scanner.find()) {
            if (scanner.group().equals("\n")) break;
        }
    }

    private String ignoreWhitespaces() throws IOException {
        while (scanner.group().equals("\n")) {
            ++lineNumber;
            scanner.find();
        }
        return scanner.group();
    }

    private enum MODE {
        INITIAL, INGRAPH, INEDGE, INVERTEX, INVERTEXPROPERTY, INEDGEPROPERTY
    }
}
