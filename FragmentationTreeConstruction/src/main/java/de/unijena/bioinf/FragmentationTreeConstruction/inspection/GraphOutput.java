package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.FragmentationTreeConstruction.model.Fragment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;

import java.io.*;
import java.util.Iterator;

/**
 * Output the graph as plain text
 */
public class GraphOutput {

    public GraphOutput() {

    }

    public void printToFile(FragmentationPathway graph, double score, File file) throws IOException {
        final FileWriter fw = new FileWriter(file);
        print(graph, score, fw);
        fw.close();
    }

    public void print(FragmentationPathway graph, double score, Writer output) throws IOException {
        final BufferedWriter writer = new BufferedWriter(output);
        writer.write(String.valueOf(graph.numberOfVertices()));
        writer.write('\n');
        writer.write(String.valueOf(graph.numberOfEdges()));
        writer.write('\n');
        writer.write(String.valueOf(graph.numberOfColors()));
        writer.write('\n');
        writer.write(String.valueOf(score));
        writer.write("\n");
        for (Fragment u : graph.getFragments()) {
            writer.write(String.valueOf(u.getIndex()));
            writer.write(' ');
            writer.write(String.valueOf(u.getColor()));
            writer.write(' ');
            writer.write('\n');
        }
        final Iterator<Loss> iter = graph.lossIterator();
        while (iter.hasNext()) {
            final Loss uv = iter.next();
            writer.write(String.valueOf(uv.getHead().getIndex()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getTail().getIndex()));
            writer.write(' ');
            writer.write(String.valueOf(uv.getWeight()));
            writer.write('\n');
        }
        writer.flush();
    }

}
