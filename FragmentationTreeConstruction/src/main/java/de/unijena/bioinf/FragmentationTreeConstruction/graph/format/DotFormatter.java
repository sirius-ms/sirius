package de.unijena.bioinf.FragmentationTreeConstruction.graph.format;


import de.unijena.bioinf.graphUtils.tree.PreOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class DotFormatter<T> extends TreeFormatter<T> {
    public DotFormatter(TreeAdapter<T> treeAdapter, VertexFormatter<T> vertexFormatter, EdgeFormatter<T> edgeFormatter) {
        super(treeAdapter, vertexFormatter, edgeFormatter);
    }

    public DotFormatter(TreeAdapter<T> treeAdapter, VertexFormatter<T> vertexFormatter) {
        super(treeAdapter, vertexFormatter);
    }

    public DotFormatter(TreeAdapter<T> treeAdapter) {
        super(treeAdapter);
    }

    @Override
    public void format(T tree, final Writer stream) throws IOException {
        stream.write("strict digraph {\n");
        final PreOrderTraversal<T> trav = new PreOrderTraversal<T>(tree, treeAdapter);
        try {
            trav.call(new PreOrderTraversal.Call<T, Integer>() {
                final List<T> nodes = new ArrayList<T>();
                @Override
                public Integer call(Integer parentIndex, T node) {
                    final String label = vertexFormatter.format(node).replace('"', '\'');
                    final int index = nodes.size();
                    nodes.add(node);
                    try {
                        stream.write("\tv" + index + " [label=\"" + label + "\"];\n");
                        if (parentIndex != null) {
                            final String edgeLabel = edgeFormatter.format(nodes.get(parentIndex), node);
                            stream.write("\t\tv" + parentIndex + " -> v" + index + " [label=\"" + edgeLabel + "\"];\n");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return index;
                }
            });
        } finally {

        }
        stream.write("}");

    }
}
