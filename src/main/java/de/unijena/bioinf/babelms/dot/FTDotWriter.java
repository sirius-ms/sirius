/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.io.*;
import java.util.*;

public class FTDotWriter {

    private boolean HTML = true;
    private boolean includeIon = true;

    public FTDotWriter(boolean HTML, boolean includeIon) {
        this.HTML = HTML;
        this.includeIon = includeIon;
    }

    public FTDotWriter() {
        this(true, true);
    }

    private static String printClassName(Class<?> klass) {
        final Called name = klass.getAnnotation(Called.class);
        return name != null ? name.value() : klass.getSimpleName();
    }

    public void writeTreeToFile(File file, FTree tree) throws IOException {
        final FileWriter fw = new FileWriter(file);
        writeTree(fw, tree);
    }

    public void writeTree(Writer writer, FTree tree) throws IOException {
        if (!(writer instanceof BufferedWriter)) writer=new BufferedWriter(writer);
        writer.write("strict digraph {\n");
        final FragmentAnnotation<Peak> peakAno = tree.getFragmentAnnotationOrThrow(Peak.class);
        // normalize intensities
        double maxInt = 1e-12;
        for (Fragment f : tree.getFragments())
        maxInt = Math.max(maxInt, peakAno.get(f).getIntensity());

        final boolean hasScores;
        final FragmentAnnotation<Score> fscore = tree.getFragmentAnnotationOrNull(Score.class);
        final LossAnnotation<Score> lscore = tree.getLossAnnotationOrNull(Score.class);
        final TreeScoring scoring = tree.getAnnotationOrNull(TreeScoring.class);
        if (fscore==null || lscore == null || scoring==null) {
            hasScores=false;
        } else {
            hasScores = true;
        }

        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);

        writer.write("\tnode [shape=rect,style=rounded];\n");
        if (hasScores) {
            writer.write("\tlabelloc=\"t\";\n");
            writer.write("\tlabel=\"");
            writer.write("Compound Score: ");
            writer.write(String.format(Locale.US, "%.4f", scoring.getOverallScore()));
            writer.write("\";\n");
        }

        for (Fragment f : tree.getFragments()) {
            final MolecularFormula formula = (includeIon && ion instanceof IonMode) ? ion.getAtoms().add(f.getFormula()) : f.getFormula();
            writer.write("\t");
            writer.write(formula.toString());
            writer.write(" [label=");
            writer.write(htmlStart());
            writer.write(htmlFormula(formula,ion));
            writer.write(htmlSmall());
            writer.write(htmlNewline());
            writer.write(" ");
            writer.write(htmlNewline());
            final Peak p = peakAno.get(f);
            writer.write(htmlLabel(String.format(Locale.US, "%.4f Da, %.2f %%", p.getMass(), p.getIntensity() * 100d / maxInt)));
            if (hasScores) {
                writer.write(htmlNewline());
                double score = fscore.get(f).sum();
                if (!f.isRoot()) score += lscore.get(f.getIncomingEdge()).sum();
                writer.write(htmlLabel(String.format(Locale.US, "%.4f", score)));
            }
            writer.write(htmlEndSmall());
            writer.write(htmlEnd());
            writer.write("];\n");
        }
        writer.write("\n");
        for (Loss l : tree.losses()) {
            final MolecularFormula sf = (includeIon && ion instanceof IonMode) ? ion.getAtoms().add(l.getSource().getFormula()) : l.getSource().getFormula();
            final MolecularFormula tf = (includeIon && ion instanceof IonMode) ? ion.getAtoms().add(l.getTarget().getFormula()) : l.getTarget().getFormula();
            writer.write("\t");
            writer.write(sf.toString());
            writer.write(" -> ");
            writer.write(tf.toString());
            writer.write(" [label=");
            writer.write(htmlStart());
            writer.write(htmlFormula(l.getFormula()));
            writer.write(htmlEnd());
            writer.write("];\n");
        }
        writer.write("}");
        writer.close();
    }

    @Deprecated
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

    @Deprecated
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
            buf.write(String.format(locale, "%.4f Da, %.2f %%", peakAno.get(f).getMass(), peakAno.get(f).getIntensity() / normalization * 100));
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

    public String htmlFormula(MolecularFormula f, Ionization ion) {
        final String s = f.toString();
        if (!HTML) return s;
        final String str = s.replaceAll("(\\d+)", "<SUB>$1</SUB>");
        if (!includeIon) return str;
        if (ion.getCharge() > 0) {
            return str + "<SUP>" + (ion.getCharge()>1 ? ion.getCharge() : "") + "+</SUP>";
        } else {
            return str + "<SUP>" + (ion.getCharge()<-1 ? -ion.getCharge() : "") + "-</SUP>";
        }
    }

    public String htmlEndSmall() {
        if (!HTML) return "";
        return "</FONT>";
    }

    public String htmlStartParagraph() {
        if (!HTML) return "";
        else return "<p>";
    }

    public String htmlEndParagraph() {
        if (!HTML) return "\\n";
        else return "</p>";
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
