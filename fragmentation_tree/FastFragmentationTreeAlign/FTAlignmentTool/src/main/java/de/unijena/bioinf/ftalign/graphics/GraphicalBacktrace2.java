
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign.graphics;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.graphUtils.tree.PreOrderTraversal;
import de.unijena.bioinf.treealign.AlignmentTree;

import java.awt.*;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.regex.Pattern;

public class GraphicalBacktrace2 {

    private final PrintStream outWriter;
    private FragmentAnnotation<Peak> leftAno, rightAno;
    private final AlignmentTree<Fragment> alignment;
    private boolean printPrettyFormulas = true;
    private int cellPadding;

    public GraphicalBacktrace2(PrintStream outWriter, FTree left, FTree right, AlignmentTree<Fragment> alignment) {
        this.outWriter = outWriter;
        leftAno = left.getFragmentAnnotationOrThrow(Peak.class);
        rightAno = right.getFragmentAnnotationOrThrow(Peak.class);
        this.alignment = alignment;
        this.cellPadding = 5;
    }

    public void print() {
        outWriter.println("strict digraph {\nnode[shape=none];");
        if (alignment.getRoot() != null) {
            final Iterator<AlignmentTree.Node<Fragment>> iterator =
                    new PreOrderTraversal.TreeIterator<AlignmentTree.Node<Fragment>>(alignment.getCursor());
            while (iterator.hasNext()) {
                final AlignmentTree.Node<Fragment> u = iterator.next();
                printNode(u);
            }
        }
        outWriter.println("}");
    }

    public boolean isPrintPrettyFormulas() {
        return printPrettyFormulas;
    }

    public void setPrintPrettyFormulas(boolean printPrettyFormulas) {
        this.printPrettyFormulas = printPrettyFormulas;
    }

    private void printNode(AlignmentTree.Node<Fragment> u) {
        printNodeId(u);
        startTable(u);
        if (u.left != null) printNode(u.left, leftAno, u.isJoin());
        else if (u.isJoin()) {
            printDeletion('*');
        } else {
            printDeletion('-');
        }
        if (u.right != null) printNode(u.right, rightAno, u.isJoin());
        else if (u.isJoin()) {
            printDeletion('*');
        } else {
            printDeletion('-');
        }
        endTable(u.score);
        outWriter.print(">];\n");
        if (u.getParent() != null) {
            printEdge(u.getParent(), u);
        }
    }

    public void setTrees(FTree left, FTree right) {
        leftAno = left.getFragmentAnnotationOrThrow(Peak.class);
        rightAno = right.getFragmentAnnotationOrThrow(Peak.class);
    }

    private void printNodeId(AlignmentTree.Node<Fragment> node) {
        outWriter.print("v");
        outWriter.print(node.getIndex());
    }

    private void printEdge(AlignmentTree.Node<Fragment> a, AlignmentTree.Node<Fragment> b) {
        final boolean joinEdge = b.isJoin();
        printNodeId(a);
        outWriter.print(" -> ");
        printNodeId(b);
        outWriter.print(" [label=<");
        outWriter.print(b.left != null ? prettyFormula(b.left.getIncomingEdge().getFormula()) : (joinEdge ? "*" : "-"));
        outWriter.print(" / ");
        outWriter.print(b.right != null ? prettyFormula(b.right.getIncomingEdge().getFormula()) : (joinEdge ? "*" : "-"));
        outWriter.print(">");
        if (b.left != null && b.right != null && b.left.getIncomingEdge().getFormula().equals(b.right.getIncomingEdge().getFormula())) {
            outWriter.print(", penwidth=3, color=\"#04B404\"");
        }
        outWriter.print("];\n");
    }

    private String colorScheme(AlignmentTree.Node<Fragment> a) {
        if (a.isJoinTerminalNode()) return "#FFB600";
        if (a.isJoin()) return "#FFFF00";
        if (a.left == null || a.right == null) return "#F78181";
        if (a.getParent() == null) return "#E6E6E6";
        if (a.left.getFormula().equals(a.right.getFormula()) && a.left.getIncomingEdge().getFormula().equals(a.right.getIncomingEdge().getFormula()))
            return "#04B404";
        if (a.score > 0) return "#81F781";
        return "#E6E6E6";
    }

    private String joinColor(int join) {
        final Color c = Color.getHSBColor(0.13f, 1f - Math.min(1f, (join - 1) * 0.5f), 1f - Math.min(1f, (join - 1) * 0.05f));
        return String.format("#%x", c.getRGB() & 0xffffff);
    }

    private void printDeletion(char symbol) {
        outWriter.print("<td bgcolor='#DF0101'>");
        outWriter.append(symbol);
        outWriter.print("</td>");
    }

    private static Pattern famount = Pattern.compile("(\\d+)");

    private String prettyFormula(MolecularFormula f) {
        if (printPrettyFormulas) {
            final String s = f.toString();
            return famount.matcher(s).replaceAll("<sub>$1</sub>");
        } else return f.toString();
    }

    private void printNode(Fragment node, FragmentAnnotation<Peak> ano, boolean isJoin) {
        outWriter.print("<td");
        //if (isJoin) outWriter.print(" bgcolor='yellow'");
        outWriter.append('>');
        outWriter.print(prettyFormula(node.getFormula()));
        outWriter.print("<br />");
        outWriter.print(String.format("%.5f Da<br />%.1f %%", ano.get(node).getMass(), ano.get(node).getIntensity() * 100d));
        outWriter.print("</td>");
    }

    private void endTable(double score) {
        outWriter.print("</tr>");
        if (score != 0) {
            outWriter.print("<tr><td colspan='2'>");
            outWriter.print(score);
            outWriter.print("</td></tr>");
        }
        outWriter.print("</table>");
    }

    private void startTable(AlignmentTree.Node<Fragment> node) {
        outWriter.print(" [label=<<table style='rounded' cellpadding='" + cellPadding + "' bgcolor='" + colorScheme(node) + "'><tr>");
    }

    public void setCellPadding(int cellPadding) {
        this.cellPadding = cellPadding;
    }

    public int getCellPadding() {
        return cellPadding;
    }
}
