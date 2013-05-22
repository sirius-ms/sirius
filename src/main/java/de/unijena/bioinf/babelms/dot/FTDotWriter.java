package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTFragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTLoss;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.io.*;
import java.util.*;

public class FTDotWriter {

    public <Fragment extends FTFragment> void writeTreeToFile(File file, FTGraph<Fragment> graph, Map<Fragment, Map<Class<?>, Double>> vertexScores,
                                                        Map<? extends FTLoss<Fragment>, Map<Class<?>, Double>> edgeScores) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writeTree(writer, graph, vertexScores, edgeScores);
        } finally {
            if (writer!=null) writer.close();
        }
    }

    public <Fragment extends FTFragment> void writeTree(Writer writer, FTGraph<Fragment> graph, Map<Fragment, Map<Class<?>, Double>> vertexScores,
                                                    Map<? extends FTLoss<Fragment>, Map<Class<?>, Double>> edgeScores) throws IOException {
        final Locale locale = Locale.US;
        final BufferedWriter buf = new BufferedWriter(writer);
        final HashMap<Fragment, Integer> ids = new HashMap<Fragment, Integer>();
        buf.write("strict digraph {\n");
        final TreeCursor<Fragment> cursor = TreeCursor.getCursor(graph.getRoot(), new FTAdapter<Fragment>());
        int id = 0;
        final ArrayList<FTLoss> losses = new ArrayList<FTLoss>();
        for (Fragment f : new PostOrderTraversal<Fragment>(cursor)) {
            ids.put(f, ++id);
            buf.write("v" + id + " [label=\"" );
            buf.write(f.getFormula().toString());
            buf.write(String.format(locale, "\\n%.4f Da, %.2f %%", f.getPeak().getMass(), f.getRelativePeakIntensity()*100));
            final double dev = f.getPeak().getMass()-graph.getIonization().addToMass(f.getFormula().getMass());
            buf.write(String.format(locale, "\\nMassDev: %.4f ppm, %.4f Da", dev*1e6d/f.getPeak().getMass(), dev));
            buf.write("\\ncE: " + f.getCollisionEnergies().toString());

            final Map<Class<?>, Double> annotations = vertexScores.get(f);
            double sum = 0d;
            for (Class<?> klass : annotations.keySet()) {
                final double score = annotations.get(klass);
                if (score != 0) {
                    buf.write("\\n");
                    buf.write(printClassName(klass));
                    buf.write("=");
                    buf.write(String.format(locale, "%.4f", score));
                    sum += score;
                }
            }
            if (f.getParent()!=null) {
                final FTLoss<Fragment> l = f.getIncomingEdge();
                final Map<Class<?>, Double> edgeAnnotations = edgeScores.get(l);
                for (Class<?> klass : edgeAnnotations.keySet()) {
                    final double score = edgeAnnotations.get(klass);
                    if (score != 0) {
                        buf.write("\\n");
                        buf.write(printClassName(klass));
                        buf.write("=");
                        buf.write(String.format(locale, "%.4f", score));
                        sum += score;
                    }
                }
                buf.write(String.format(locale, "\\nScore: %.4f", sum));
                buf.write("\"];\n");
                losses.add(f.getIncomingEdge());
            } else buf.write(String.format(locale, "\\nScore: %.4f\"];\n", sum));
        }
        for (FTLoss<? extends Fragment> loss : losses) {
            buf.write("v" + ids.get(loss.getHead()));
            buf.write(" -> ");
            buf.write("v" + ids.get(loss.getTail()));
            buf.write(" [label=\"");
            buf.write(loss.getFormula().toString());
            buf.write("\"];\n");
        }
        buf.write("}");
        buf.flush();
    }

    private static String printClassName(Class<?> klass) {
        final Called name = klass.getAnnotation(Called.class);
        return name != null ? name.value() : klass.getSimpleName();
    }

    private static class FTAdapter<T extends FTFragment> implements TreeAdapter<T> {

        @Override
        public int getDegreeOf(T t) {
            return t.getChildren().size();
        }

        @Override
        public List<T> getChildrenOf(T t) {
            return (List<T>)t.getChildren();
        }
    }


}
