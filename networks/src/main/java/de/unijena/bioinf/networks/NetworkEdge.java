package de.unijena.bioinf.networks;

import java.util.Arrays;
import java.util.Optional;

public class NetworkEdge {

    NetworkNode left, right;
    EdgeType[] data;
    double mzDifference;

    public NetworkEdge(NetworkNode left, NetworkNode right, EdgeType... types) {
        this.left = left;
        this.right = right;
        this.data = types;
        this.mzDifference = Math.abs(left.mz-right.mz);
    }

    public EdgeType[] getData() {
        return data;
    }

    void addEdgeData(EdgeType newOne) {
        data = Arrays.copyOf(data, data.length+1);
        data[data.length-1] = newOne;
    }

    public <T extends EdgeType> Optional<T> getDatum(Class<T> klass) {
        for (EdgeType e : data) {
            if (e.getClass().equals(klass)) {
                return Optional.of((T)e);
            }
        }
        return Optional.empty();
    }

    public NetworkNode other(NetworkNode node) {
        if (left==node) return right;
        else return left;
    }
}
