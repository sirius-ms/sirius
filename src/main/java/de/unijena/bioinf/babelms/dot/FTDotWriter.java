package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.io.*;
import java.util.*;

public class FTDotWriter {

    private boolean HTML = false;

    private static String printClassName(Class<?> klass) {
        final Called name = klass.getAnnotation(Called.class);
        return name != null ? name.value() : klass.getSimpleName();
    }

    public void writeTreeToFile(File file, FTree graph,Map<Fragment, List<String>> additionalProperties,  Map<Fragment, Map<Class<?>, Double>> vertexScores,
                                                        Map<Loss, Map<Class<?>, Double>> edgeScores) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writeTree(writer, graph, additionalProperties, vertexScores, edgeScores);
        } finally {
            if (writer!=null) writer.close();
        }
    }

    public void writeGraph(Writer writer, FTree graph, Map<Fragment, List<String>> additionalProperties, Map<Fragment, Map<Class<?>, Double>> vertexScores,
                           Map<? extends Loss, Map<Class<?>, Double>> edgeScores) throws IOException {
        final Locale locale = Locale.US;
        final BufferedWriter buf = new BufferedWriter(writer);
        final HashMap<Fragment, Integer> ids = new HashMap<Fragment, Integer>();
        buf.write("strict digraph {\n");
        final TreeCursor<Fragment> cursor = graph.getCursor();
        final FragmentAnnotation<Peak> peakAno = graph.getFragmentAnnotationOrThrow(Peak.class);
        final FragmentAnnotation<CollisionEnergy> ceAno = graph.getFragmentAnnotationOrThrow(CollisionEnergy.class);
        final double normalization;
        {
            double maxInt = 0d;
            for (Fragment f : graph.getFragments()) {
                maxInt = Math.max(peakAno.get(f).getIntensity(), maxInt);
            }
            normalization = maxInt;
        }
        int id = 0;
        final ArrayList<Loss> losses = new ArrayList<Loss>();
        for (Fragment f : new PostOrderTraversal<Fragment>(cursor)) {
            if (ids.containsKey(f)) continue;
            ids.put(f, ++id);
            buf.write("v" + id + " [label=\"");
            buf.write(f.getFormula().toString());
            buf.write(String.format(locale, "\\n%.4f Da, %.2f %%", peakAno.get(f).getMass(), peakAno.get(f).getIntensity()/normalization * 100));
            final double dev = peakAno.get(f).getMass() - graph.getAnnotationOrThrow(Ionization.class).addToMass(f.getFormula().getMass());
            buf.write(String.format(locale, "\\nMassDev: %.4f ppm, %.4f Da", dev * 1e6d / peakAno.get(f).getMass(), dev));
            buf.write("\\ncE: " + ceAno.get(f).toString());
            if (additionalProperties != null) {
                final List<String> addProps = additionalProperties.get(f);
                if (addProps != null) {
                    for (String s : addProps) {
                        buf.write("\\n");
                        buf.write(s);
                    }
                }
            }
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
            if (f.getParents() != null) {
                for (Loss incomingEdge : f.getIncomingEdges()) {
                    losses.add(incomingEdge);
                }
                buf.write(String.format(locale, "\\nScore: %.4f\"];\n", sum));

            } else buf.write(String.format(locale, "\\nScore: %.4f\"];\n", sum));
        }
        for (Loss loss : losses) {
            buf.write("v" + ids.get(loss.getSource()));
            buf.write(" -> ");
            buf.write("v" + ids.get(loss.getTarget()));
            buf.write(" [label=\"");

            double sum = 0d;
            final Map<Class<?>, Double> edgeAnnotations = edgeScores.get(loss);
            for (Class<?> klass : edgeAnnotations.keySet()) {
                final double score = edgeAnnotations.get(klass);
                if (score != 0) {
//                    buf.write("\\n");
//                    buf.write(printClassName(klass));
//                    buf.write("=");
//                    buf.write(String.format(locale, "%.4f", score));
                    sum += score;
                }
            }
            buf.write(loss.getFormula().toString());
            buf.write(String.format(locale, "\\nScore: %.4f", sum));
            buf.write("\"];\n");
        }
        buf.write("}");
        buf.flush();

    }

    public void writeTree(Writer writer, FTree graph, Map<Fragment, List<String>> additionalProperties, Map<Fragment, Map<Class<?>, Double>> vertexScores,
                          Map<Loss, Map<Class<?>, Double>> edgeScores) throws IOException {
        final Locale locale = Locale.US;
        final BufferedWriter buf = new BufferedWriter(writer);
        final HashMap<Fragment, Integer> ids = new HashMap<Fragment, Integer>();
        buf.write("strict digraph {\n");
        final TreeCursor<Fragment> cursor = graph.getCursor();
        final FragmentAnnotation<Peak> peakAno = graph.getFragmentAnnotationOrThrow(Peak.class);
        final FragmentAnnotation<CollisionEnergy[]> ceAnos = graph.getFragmentAnnotationOrThrow(CollisionEnergy[].class);
        int id = 0;
        final ArrayList<Loss> losses = new ArrayList<Loss>();
        final double normalization;
        {
            double maxInt = 0d;
            for (Fragment f : graph.getFragments()) {
                maxInt = Math.max(peakAno.get(f).getIntensity(), maxInt);
            }
            normalization = maxInt;
        }
        for (Fragment f : new PostOrderTraversal<Fragment>(cursor)) {
            ids.put(f, ++id);
            buf.write("v" + id + " [label=");
            buf.write(htmlStart());
            buf.write(htmlFormula(f.getFormula()));
            buf.write(htmlNewline());
            buf.write(String.format(locale, "%.4f Da, %.2f %%", peakAno.get(f).getMass(), peakAno.get(f).getIntensity()/normalization * 100));
            buf.write(htmlSmall());
            final double dev = peakAno.get(f).getMass() - graph.getAnnotationOrThrow(Ionization.class).addToMass(f.getFormula().getMass());
            buf.write(htmlNewline());
            buf.write(String.format(locale, "MassDev: %.4f ppm, %.4f Da", dev * 1e6d / peakAno.get(f).getMass(), dev));
            buf.write(htmlNewline());
            buf.write("cE: " + Arrays.toString(ceAnos.get(f)));
            if (additionalProperties != null) {
                final List<String> addProps = additionalProperties.get(f);
                if (addProps != null) {
                    for (String s : addProps) {
                        buf.write(htmlNewline());
                        buf.write(htmlLabel(s));
                    }
                }
            }
            final Map<Class<?>, Double> annotations = vertexScores.get(f);
            double sum = 0d;
            for (Class<?> klass : annotations.keySet()) {
                final double score = annotations.get(klass);
                if (score != 0) {
                    buf.write(htmlNewline());
                    buf.write(printClassName(klass));
                    buf.write("=");
                    buf.write(String.format(locale, "%.4f", score));
                    sum += score;
                }
            }
            if (!f.isRoot()) {
                final Loss l = f.getIncomingEdge();
                final Map<Class<?>, Double> edgeAnnotations = edgeScores.get(l);
                for (Class<?> klass : edgeAnnotations.keySet()) {
                    final double score = edgeAnnotations.get(klass);
                    if (score != 0) {
                        buf.write(htmlNewline());
                        buf.write(printClassName(klass));
                        buf.write("=");
                        buf.write(String.format(locale, "%.4f", score));
                        sum += score;
                    }
                }
                buf.write(htmlNewline());
                buf.write(htmlEndSmall());
                buf.write(String.format(locale, "Score: %.4f", sum));
                buf.write(htmlEnd());
                buf.write("];\n");
                losses.add(f.getIncomingEdge());
            } else {
                buf.write(htmlNewline());
                buf.write(htmlEndSmall());
                buf.write(String.format(locale, "Score: %.4f" + htmlEnd() + "];\n", sum));
            }
        }
        for (Loss loss : losses) {
            buf.write("v" + ids.get(loss.getSource()));
            buf.write(" -> ");
            buf.write("v" + ids.get(loss.getTarget()));
            buf.write(" [label=");
            buf.write(htmlStart());
            buf.write(htmlFormula(loss.getFormula()));
            buf.write(htmlEnd());
            buf.write("];\n");
        }
        buf.write("}");
        buf.flush();
    }

    public String htmlLabel(String label) {
        if (!HTML) return label;
        return label.replaceAll("<", "&#60;").replaceAll(">", "&#62;");
    }

    public String htmlStart() {
        if (HTML) return "<"; else return "\"";
    }

    public String htmlEnd() {
        if (HTML) return ">";
        else return "\"";
    }

    public String htmlSmall() {
        if (!HTML) return "";
        return "<FONT POINT-SIZE=\"8\">";
    }

    public String htmlFormula(MolecularFormula f) {
        final String s = f.toString();
        if (!HTML) return s;
        return s.replaceAll("(\\d+)", "<SUB>$1</SUB>");
    }

    public String htmlEndSmall() {
        if (!HTML) return "";
        return "</FONT>";
    }

    public String htmlNewline() {
        if (!HTML) return "\\n";
        return "<BR />";
    }

    private static class FTAdapter implements TreeAdapter<Fragment> {

        @Override
        public int getDegreeOf(Fragment t) {
            return t.getOutDegree();
        }

        @Override
        public List<Fragment> getChildrenOf(Fragment t) {
            return t.getChildren();
        }
    }


}
