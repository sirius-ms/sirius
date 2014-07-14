package de.unijena.bioinf.babelms.dot;

public interface DotHandler<NodeType, EdgeType> {

    public NodeType addVertex(String name);

    public void addVertexProperty(NodeType node, String key, String value);

    public EdgeType addEdge(NodeType u, NodeType v);

    public void addEdgeProperty(EdgeType edge, String key, String value);

}
