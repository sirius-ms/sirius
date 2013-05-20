package de.unijena.bioinf.treeviewer.dot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 5/16/13
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Parser2 {
    final static Pattern TOKENIZE = Pattern.compile("strict|(?:di)?graph|=|\"|\\|,|\\[|\\]|\\{|\\}|#|\n|;", Pattern.MULTILINE);

    private static enum MODE {
        INITIAL, INGRAPH, INEDGE, INVERTEX, INVERTEXPROPERTY, INEDGEPROPERTY;
    }
    private static int NOESCAPE = 0, QUOTED=1, ESCAPED=2;

    private MODE m = MODE.INITIAL;
    private Matcher scanner;
    private int lineNumber = 0;
    private int lastMatch;
    private StringBuilder source;
    private HashMap<String, Vertex> vertices = new HashMap<String, Vertex>();
    private List<Edge> edges = new ArrayList<Edge>();
    private Vertex currentVertex;
    private Edge currentEdge;
    private int escapeStatus = 0;
    private StringBuilder buffer = new StringBuilder();
    private String propertyName;

    private Parser2() {

    }

    public static Graph parse(Reader input) throws IOException {
        Parser2 p = new Parser2();
        p.parseIt(input);
        final Graph g = new Graph();
        g.getEdges().addAll(p.edges);
        g.getVertices().addAll(p.vertices.values());
        return g;
    }



    private void parseIt(Reader input) throws IOException {
        final BufferedReader r = new BufferedReader(input);
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
                    if (after-now == 1) {
                        // escape character
                        buffer.append(source.subSequence(now, after));
                    } else buffer.append(source.subSequence(now-1, after));
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
                };
            } else if (token.equals("\\")) {
                if (TOKENIZE.matcher(source.subSequence(scanner.end(), scanner.end()+1)).find()) scanner.find();
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
                    currentEdge = new Edge(a, b);
                    edges.add(currentEdge);
                    m = MODE.INEDGE;
                } else if (token.equals("[")) {
                    // parse vertex
                    String vertexName = before().trim();
                    currentVertex = new Vertex(vertexName);
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
                    if (buffer.length()==0) {
                        if (lastMatch > 0 && source.charAt(lastMatch-1) == '=')
                            propertyValue = before();
                        else
                            propertyValue = "";
                    } else {
                        propertyValue = buffer.toString();
                        clearBuffer();
                    }
                    if (propertyName.equals("label")) {
                        if (m == MODE.INEDGEPROPERTY) currentEdge.getProperties().put(propertyName, propertyValue);
                        else currentVertex.getProperties().put(propertyName, propertyValue);
                    }
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

    private Vertex getVertex(String name) {
        final Vertex av = vertices.get(name);
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
        while (scanner.find()) {
            final String tok = scanner.group();
            if (escapeStatus==0 && tok.equals("#")) commentLine();
            if (tok.equals(token)) break;
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

}
