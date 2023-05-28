package de.unijena.bioinf.networks;

import de.unijena.bioinf.networks.serialization.AbstractConnection;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.networks.serialization.CorrelationConnection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MolecularNetwork {
    protected final Object2IntMap<String> id2index;
    protected final String[] ids;
    protected final ArrayList<NetworkNode> nodes;

    protected MolecularNetwork(ArrayList<NetworkNode> nodes, Object2IntMap<String> idMapper) {
        this.id2index = idMapper;
        this.nodes = nodes;
        this.ids = new String[nodes.size()];
        id2index.forEach((key, id) -> ids[id] = key);
    }

    public MolecularNetwork.NetworkBuilder modify() {
        return new NetworkBuilder(new ArrayList<>(nodes.stream().map(NetworkNode::clone).collect(Collectors.toList())), new Object2IntOpenHashMap<>(id2index));
    }

    public ConnectionTable[] toConnectionTables() {
        final ConnectionTable[] tables = new ConnectionTable[nodes.size()];
        for (int k = 0; k < tables.length; ++k) {
            tables[k] = getConnectionTable(k);
        }
        return tables;
    }

    private ConnectionTable getConnectionTable(int vertexId) {
        final NetworkNode node = nodes.get(vertexId);
        final List<NetworkEdge> edges = node.edges;

        final List<CorrelationConnection> correlations = new ArrayList<>();

        for (NetworkEdge e : edges) {
            e.getDatum(Correlation.class).ifPresent(x -> correlations.add(new CorrelationConnection(ids[e.other(node).vertexId], (float) e.mzDifference, x.weight)));
        }

        final ConnectionTable table = new ConnectionTable(
                ids[vertexId], node.subnetwork, (float) node.mz,
                correlations.toArray(CorrelationConnection[]::new)
        );
        return table;
    }

    public static MolecularNetwork fromConnectionTables(ConnectionTable[] tables) {
        final MolecularNetwork.NetworkBuilder b = new NetworkBuilder();
        for (ConnectionTable t : tables) {
            b.addNode(t.id, t.mz).subnetwork = t.subNetwork;
        }
        for (ConnectionTable t : tables) {
            final NetworkNode u = b.nodes.get(b.id2index.get(t.id));
            final HashMap<String, List<EdgeType>> data = new HashMap<>();
            for (AbstractConnection c : t.edges()) {
                data.computeIfAbsent(c.targetName, (x) -> new ArrayList<>()).add(c.asEdgeType());
            }
            for (Map.Entry<String, List<EdgeType>> entry : data.entrySet()) {
                b.ensureEdge(u.vertexId, b.id2index.get(entry.getKey()), entry.getValue().toArray(EdgeType[]::new));
            }
        }
        return b.done(false);
    }

    public MolecularNetwork restrictTo(Class<? extends EdgeType>... allowedEdgeTypes) {
        final Set<Class<? extends EdgeType>> klasses = new HashSet<>(Arrays.asList(allowedEdgeTypes));
        final NetworkBuilder b = new NetworkBuilder();
        for (NetworkNode u : nodes) {
            b.addNode(ids[u.vertexId], u.mz);
        }
        for (NetworkNode u : nodes) {
            for (NetworkEdge e : u.edges) {
                if (e.left.vertexId > e.right.vertexId) continue;
                boolean use = false;
                for (EdgeType t : e.getData()) {
                    if (klasses.contains(t.getClass())) {
                        use = true;
                        break;
                    }
                }
                if (use) {
                    b.addEdge(e.left.vertexId, e.right.vertexId, Arrays.stream(e.getData()).filter(x -> klasses.contains(x.getClass())).toArray(EdgeType[]::new));
                }
            }
        }
        return b.done(true);
    }

    public MolecularNetwork relabel(Function<String, String> newIndexMapping) {
        final Object2IntMap<String> newMapping = new Object2IntOpenHashMap<>();
        id2index.forEach((key, index) -> {
            newMapping.put(newIndexMapping.apply(key), index.intValue());
        });
        return new MolecularNetwork(nodes, newMapping);
    }

    public NetworkNode getNode(String name) {
        return nodes.get(id2index.getInt(name));
    }

    public Optional<NetworkEdge> getEdge(String u, String v) {
        final NetworkNode U = getNode(u);
        final NetworkNode V = getNode(v);
        return U.getEdgeTo(V);
    }

    public Set<NetworkNode> getMolecularFamily(String u) {
        return getMolecularFamily(getNode(u));
    }

    public Set<NetworkNode> getMolecularFamily(NetworkNode u) {
        final HashSet<NetworkNode> nodes = new HashSet<>();
        final ArrayList<NetworkNode> stack = new ArrayList<>();
        stack.add(u);
        nodes.add(u);
        while (!stack.isEmpty()) {
            NetworkNode v = stack.remove(stack.size() - 1);
            for (NetworkEdge e : v.edges) {
                if (!nodes.contains(e.left)) {
                    nodes.add(e.left);
                    stack.add(e.left);
                }
                if (!nodes.contains(e.right)) {
                    nodes.add(e.right);
                    stack.add(e.right);
                }
            }
        }
        return nodes;
    }

    public NetworkNode getNode(int index) {
        return nodes.get(index);
    }

    public static class NetworkBuilder {
        private final ArrayList<NetworkNode> nodes;
        private final Object2IntMap<String> id2index;

        public NetworkBuilder() {
            nodes = new ArrayList<>();
            id2index = new Object2IntOpenHashMap<>();
        }

        public NetworkBuilder(ArrayList<NetworkNode> nodes, Object2IntMap<String> id2index) {
            this.nodes = nodes;
            this.id2index = id2index;
        }

        public NetworkNode getNode(String name) {
            return nodes.get(id2index.getInt(name));
        }

        public NetworkNode addNode(String id, double mz) {
            if (id2index.containsKey(id))
                throw new IllegalArgumentException("ID '" + id + "' is already in use.");
            final int vertexId = nodes.size();
            final NetworkNode u = new NetworkNode(vertexId, mz);
            nodes.add(u);
            id2index.put(id, vertexId);
            return u;
        }

        public NetworkEdge addEdge(int u, int v, EdgeType... edgeTypes) {
            final NetworkNode U = nodes.get(u);
            final NetworkNode V = nodes.get(v);
            return addEdge(U, V, edgeTypes);
        }

        public NetworkEdge addEdge(String u, String v, EdgeType... edgeTypes) {
            final NetworkNode U = nodes.get(id2index.getInt(u));
            final NetworkNode V = nodes.get(id2index.getInt(v));
            return addEdge(U, V, edgeTypes);
        }

        public NetworkEdge addEdge(NetworkNode u, NetworkNode v, EdgeType[] edgeTypes) {
            final NetworkEdge edge = new NetworkEdge(u, v, edgeTypes);
            u.addEdge(edge);
            v.addEdge(edge);
            return edge;
        }

        public MolecularNetwork done(boolean findConnectionComponents) {
            if (findConnectionComponents) {
                // find connected components
                List<NetworkNode> components = new ArrayList<>();
                final boolean[] used = new boolean[nodes.size()];
                final int[] componentSize = new int[nodes.size()];
                for (final NetworkNode node : nodes) {
                    if (!used[node.vertexId]) {
                        traverse(node, used, (u) -> {
                            componentSize[node.vertexId]++;
                        });
                        components.add(node);
                    }
                }
                components.sort(Comparator.comparingInt(x -> -componentSize[x.vertexId]));
                Arrays.fill(used, false);
                int networkId = 0;
                for (final NetworkNode component : components) {
                    if (componentSize[component.vertexId] > 1) {
                        final int subnetworkId = networkId++;
                        traverse(component, used, (u) -> {
                            u.subnetwork = subnetworkId;
                        });
                    }
                }
            }
            return new MolecularNetwork(nodes, id2index);
        }

        private void traverse(NetworkNode u, boolean[] vec, Consumer<NetworkNode> functor) {
            final ArrayList<NetworkNode> stack = new ArrayList<>();
            functor.accept(u);
            vec[u.vertexId] = true;
            stack.add(u);
            while (!stack.isEmpty()) {
                NetworkNode node = stack.remove(stack.size() - 1);
                for (NetworkEdge edge : node.edges) {
                    if (!vec[edge.left.vertexId]) {
                        vec[edge.left.vertexId] = true;
                        stack.add(edge.left);
                        functor.accept(edge.left);
                    }
                    if (!vec[edge.right.vertexId]) {
                        vec[edge.right.vertexId] = true;
                        stack.add(edge.right);
                        functor.accept(edge.right);
                    }
                }
            }
        }

        public void ensureEdge(int u, int v, EdgeType... edges) {
            Optional<NetworkEdge> e = nodes.get(u).getEdgeTo(nodes.get(v));
            if (e.isEmpty()) addEdge(u, v, edges);
        }
    }
}
