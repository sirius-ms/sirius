package de.unijena.bioinf.networks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NetworkNode implements Cloneable {

    protected final int vertexId;
    protected final double mz;
    protected final ArrayList<NetworkEdge> edges;
    int subnetwork;

    public NetworkNode(int vertexId, double mz) {
        this.vertexId = vertexId;
        this.mz = mz;
        this.edges = new ArrayList<>();
        this.subnetwork = -1;
    }

    void addEdge(NetworkEdge edge) {
        edges.add(edge);
    }

    public int getVertexId() {
        return vertexId;
    }

    public double getMz() {
        return mz;
    }

    public List<NetworkEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int getSubnetwork() {
        return subnetwork;
    }

    public Optional<NetworkEdge> getEdgeTo(NetworkNode v) {
        for (NetworkEdge e : edges) {
            if (e.left==v || e.right==v) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public NetworkNode clone() {
        try {
            return (NetworkNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
