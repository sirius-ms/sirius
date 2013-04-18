package de.unijena.bioinf.FragmentationTreeConstruction.graph.format;


import de.unijena.bioinf.graphUtils.tree.TreeAdapter;

import java.io.*;

/**
 * @author Kai Dührkop
 */
public abstract class TreeFormatter<T> {

    protected final VertexFormatter<T> vertexFormatter;
    protected final EdgeFormatter<T> edgeFormatter;
    protected final TreeAdapter<T> treeAdapter;

    protected TreeFormatter(TreeAdapter<T> treeAdapter, VertexFormatter<T> vertexFormatter, EdgeFormatter<T> edgeFormatter) {
        this.vertexFormatter = vertexFormatter;
        this.edgeFormatter = edgeFormatter;
        this.treeAdapter = treeAdapter;
    }

    protected TreeFormatter(TreeAdapter<T> treeAdapter, VertexFormatter<T> vertexFormatter) {
        this(treeAdapter, vertexFormatter, new NoEdgeNameFormatter<T>());
    }

    protected TreeFormatter(TreeAdapter<T> treeAdapter) {
        this(treeAdapter, new ToStringFormatter<T>());
    }

    public abstract void format(T tree, Writer stream) throws IOException;

    public String formatToString(T tree) throws IOException {
        final StringWriter writer = new StringWriter();
        format(tree, writer);
        return writer.toString();
    }
    public void formatToFile(T tree, File file) throws IOException {
        final Writer writer = new BufferedWriter(new FileWriter(file));
        format(tree, writer);
        writer.close();
    }

    public static class ToStringFormatter<T> implements VertexFormatter<T> {
        @Override
        public String format(T vertex) {
            return vertex.toString();
        }
    }

    public static class NoEdgeNameFormatter<T> implements EdgeFormatter<T> {

        @Override
        public String format(T parent, T child) {
            return "";
        }
    }



}
