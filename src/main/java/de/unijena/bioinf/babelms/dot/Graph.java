package de.unijena.bioinf.babelms.dot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Graph implements Cloneable {

    private final ArrayList<Vertex> vertices;
    private final ArrayList<Edge> edges;

    public Graph() {
        this.vertices = new ArrayList<Vertex>();
        this.edges = new ArrayList<Edge>();
    }

    public Graph(Graph g) {
        this.vertices = new ArrayList<Vertex>(g.getVertices().size());
        this.edges = new ArrayList<Edge>(g.getEdges().size());
        for (Vertex v : g.getVertices()) vertices.add(new Vertex(v));
        for (Edge e : g.getEdges()) edges.add(new Edge(e));
    }

    public String writeToString() {
        final StringWriter strw = new StringWriter();
        try {
            write(strw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strw.toString();
    }

    public void write(Writer writer) throws IOException {
        final BufferedWriter bw = new BufferedWriter(writer);
        bw.write("strict digraph {\n");
        for (Vertex v : vertices) {
            bw.write(v.toString());
            bw.write('\n');
        }
        for (Edge e : edges) {
            bw.write(e.toString());
            bw.write('\n');
        }
        bw.write("}");
        bw.flush();
    }

    public List<Edge> getEdgesFor(Vertex vertex) {
        return getIncommingEdgesFor(vertex.getName());
    }

    public Vertex getRoot() {
        final HashSet<String> vertices = new HashSet<String>();
        for (Edge e : edges) {
            vertices.add(e.getHead());
        }
        for (Edge e : edges) {
            vertices.remove(e.getTail());
        }
        if (vertices.isEmpty()) return null;
        final String s = vertices.iterator().next();
        return getVertex(s);

    }

    public Vertex getVertex(String name) {
        for (Vertex u : vertices)
            if (u.getName().equals(name)) return u;
        return null;
    }

    public List<Edge> getIncommingEdgesFor(String vertex) {
        final ArrayList<Edge> neighbours = new ArrayList<Edge>();
        for (Edge e : edges) {
            if (e.getTail().equals(vertex)) neighbours.add(e);
        }
        return neighbours;
    }

    public List<Edge> getOutgoingEdgesFor(Vertex vertex) {
        return getOutgoingEdgesFor(vertex.getName());
    }

    public List<Edge> getOutgoingEdgesFor(String vertex) {
        final ArrayList<Edge> neighbours = new ArrayList<Edge>();
        for (Edge e : edges) {
            if (e.getHead().equals(vertex)) neighbours.add(e);
        }
        return neighbours;
    }

    public Edge getEdgeFor(Vertex u, Vertex v) {
        return getEdgeFor(u.getName(), v.getName());
    }

    public Edge getEdgeFor(String u, String v) {
        for (Edge e : edges) {
            if (e.getHead().equals(u) && e.getTail().equals(v)) return e;
        }
        return null;
    }

    public ArrayList<Vertex> getVertices() {
        return vertices;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public Graph clone() {
        return new Graph(this);
    }
}
