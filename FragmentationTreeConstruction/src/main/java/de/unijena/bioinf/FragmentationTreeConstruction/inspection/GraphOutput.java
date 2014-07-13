package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.io.*;
import java.util.Iterator;

/**
 * Output the graph as plain text
 */
public class GraphOutput {

    public GraphOutput() {

    }

    public void printToFile(FGraph graph, double score, File file) throws IOException {
        final FileWriter fw = new FileWriter(file);
        print(graph, score, fw);
        fw.close();
    }

    public void printToFile(FGraph graph, double score, double rootScore, File file) throws IOException {
        final FileWriter fw = new FileWriter(file);
        print(graph, score, rootScore, fw);
        fw.close();
    }

    public void print(FGraph graph, double score, double rootScore, Writer output) throws IOException {
        final BufferedWriter writer = new BufferedWriter(output);
        writer.write(String.valueOf(graph.numberOfVertices()));
        writer.write('\n');
        writer.write(String.valueOf(graph.numberOfEdges()));
        writer.write('\n');
        writer.write(String.valueOf(graph.maxColor() + 1));
        writer.write('\n');
        writer.write(String.valueOf(score));
        writer.write(" + ");
        writer.write(String.valueOf(rootScore));
        writer.write("\n");
        for (Fragment u : graph.getFragments()) {
            writer.write(String.valueOf(u.getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(u.getColor()));
            writer.write(' ');
            writer.write('\n');
        }
        final Iterator<Loss> iter = graph.lossIterator();
        while (iter.hasNext()) {
            final Loss uv = iter.next();
            writer.write(String.valueOf(uv.getSource().getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getTarget().getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getWeight()));
            writer.write('\n');
        }
        writer.flush();
    }

    public void print(FGraph graph, double score, Writer output) throws IOException {
        final BufferedWriter writer = new BufferedWriter(output);
        writer.write(String.valueOf(graph.numberOfVertices()));
        writer.write('\n');
        writer.write(String.valueOf(graph.numberOfEdges()));
        writer.write('\n');
        writer.write(String.valueOf(graph.maxColor() + 1));
        writer.write('\n');
        writer.write(String.valueOf(score));
        writer.write("\n");
        for (Fragment u : graph.getFragments()) {
            writer.write(String.valueOf(u.getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(u.getColor()));
            writer.write(' ');
            writer.write('\n');
        }
        final Iterator<Loss> iter = graph.lossIterator();
        while (iter.hasNext()) {
            final Loss uv = iter.next();
            writer.write(String.valueOf(uv.getSource().getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getTarget().getVertexId()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getWeight()));
            writer.write('\n');
        }
        writer.flush();
    }

}
