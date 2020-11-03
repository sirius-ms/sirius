
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
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ftalign.analyse.FTDataElement;
import de.unijena.bioinf.treealign.AbstractBacktrace;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 */
public class GraphicalBacktrace extends AbstractBacktrace<Fragment> {


    private final static Pattern nodePattern = Pattern.compile("([A-z_0-9]+)\\s*\\[label=\"([^\\\\]+)");
    private final static Pattern edgePattern = Pattern.compile("([A-z_0-9]+)\\s*->\\s*([A-z_0-9]+)\\s*\\[label=\"([^\"]+)");
    private final static int minEdgeLength = 1;
    private final static double ranksep = 1;
    private final static boolean enumerateNodes = false;
    private final static boolean polyLines = false;
    private final static boolean firstTreeMirrored = true;
    private Color color;
    private List<String> dotFile;
    private Map<MolecularFormula, String> leftNodes;       //to save nodes and labels of the existing dot-Files
    private Map<MolecularFormula, String> leftNodesLabel;
    private Map<MolecularFormula, String> rightNodes;
    private Map<MolecularFormula, String> rightNodesLabel;
    private Map<FormulaEdge, String> leftEdges;  // 2 Formula and the label
    private Map<FormulaEdge, String> rightEdges;
    private List<Fragment[]> matchedList; //left and right matched
    private Set<FragmentationTreeWrapper> addedWrapper;
    private Map<Fragment, FragmentationTreeWrapper> treeToWrapper;
    private int enumerator;


    /*
     *	Wird vor Berechnung des Alignments aufgerufen. Enthält die Eingabedateien und
     *  deren Bäume
     */
    public GraphicalBacktrace(FTDataElement left, FTDataElement right) {
        if (enumerateNodes) enumerator = 0;

        color = new Color();
        dotFile = new LinkedList<String>();

        leftNodes = new HashMap<MolecularFormula, String>();
        leftNodesLabel = new HashMap<MolecularFormula, String>();
        rightNodes = new HashMap<MolecularFormula, String>();
        rightNodesLabel = new HashMap<MolecularFormula, String>();

        leftEdges = new HashMap<FormulaEdge, String>();
        rightEdges = new HashMap<FormulaEdge, String>();

        matchedList = new LinkedList<Fragment[]>();


        try {
            BufferedReader bufferedReader = FileUtils.ensureBuffering(new FileReader(left.getSource().getFile()));
            String line;
            while (bufferedReader.ready()) {
                line = bufferedReader.readLine();
                if (!line.contains("->")) {
                    final Matcher m = nodePattern.matcher(line);
                    if (m.find()) {
                        final MolecularFormula formula = MolecularFormula.parseOrThrow(m.group(2));
                        leftNodes.put(formula, m.group(1));
                        leftNodesLabel.put(formula, m.group(2));

                    }
                } else {
                    final Matcher m = edgePattern.matcher(line);
                    if (m.find()) {
                        final FormulaEdge formulaEdge = new FormulaEdge(MolecularFormula.parseOrThrow(m.group(1)), MolecularFormula.parseOrThrow(m.group(2)));
                        leftEdges.put(formulaEdge, m.group(3));
                    }
                }

            }
            bufferedReader = FileUtils.ensureBuffering(new FileReader(right.getSource().getFile()));
            while (bufferedReader.ready()) {
                line = bufferedReader.readLine();
                if (!line.contains("->")) {
                    final Matcher m = nodePattern.matcher(line);
                    if (m.find()) {
                        final MolecularFormula formula = MolecularFormula.parseOrThrow(m.group(2));
                        rightNodes.put(formula, m.group(1));
                        rightNodesLabel.put(formula, m.group(2));
                    }
                } else {
                    final Matcher m = edgePattern.matcher(line);
                    if (m.find()) {
                        final FormulaEdge formulaEdge = new FormulaEdge(MolecularFormula.parseOrThrow(m.group(1)), MolecularFormula.parseOrThrow(m.group(2)));
                        ;
                        rightEdges.put(formulaEdge, m.group(3));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        dotFile.add("digraph DiG {");
        dotFile.add("label=\"" + left.getName() + " compared with " + right.getName() + "\"");
        if (polyLines) dotFile.add("splines=polyline");
        dotFile.add("ranksep=" + ranksep);
        dotFile.add("node[odering=out]");

        dotFile.add("{rank=min");
        dotFile.add(makeNode(leftNodes.get(left.getTree().getRoot().getFormula()) + "Left", leftNodesLabel.get(left.getTree().getRoot().getFormula()), null, null));
        dotFile.add(makeNode(rightNodes.get(right.getTree().getRoot().getFormula()) + "Right", rightNodesLabel.get(right.getTree().getRoot().getFormula()), null, null));
        dotFile.add("}");


        addedWrapper = new HashSet<FragmentationTreeWrapper>();
        treeToWrapper = new HashMap<Fragment, FragmentationTreeWrapper>();

        Queue<FragmentationTreeWrapper> queue = new LinkedList<FragmentationTreeWrapper>();

        FragmentationTreeWrapper leftWrapperRoot = new FragmentationTreeWrapper(left.getTree().getRoot());
        treeToWrapper.put(left.getTree().getRoot(), leftWrapperRoot);
        queue.add(leftWrapperRoot);
        while (!queue.isEmpty()) {
            FragmentationTreeWrapper current = queue.poll();
            for (Fragment child : current.getFragmentationTree().getChildren()) {
                FragmentationTreeWrapper newWrapper = new FragmentationTreeWrapper(child);
                treeToWrapper.put(child, newWrapper);
                newWrapper.setParent(current);
                queue.add(newWrapper);
            }
        }

        queue = new LinkedList<FragmentationTreeWrapper>();
        FragmentationTreeWrapper rightWrapperRoot = new FragmentationTreeWrapper(right.getTree().getRoot());
        treeToWrapper.put(right.getTree().getRoot(), rightWrapperRoot);
        queue.add(rightWrapperRoot);
        while (!queue.isEmpty()) {
            FragmentationTreeWrapper current = queue.poll();
            for (Fragment child : current.getFragmentationTree().getChildren()) {
                FragmentationTreeWrapper newWrapper = new FragmentationTreeWrapper(child);
                treeToWrapper.put(child, newWrapper);
                newWrapper.setParent(current);
                queue.add(newWrapper);
            }
        }
    }

    /*
     * Wird am Ende des Alignments aufgerufen. Bekommt die beiden Eingabedateien und den
     * Zielort an dem die grafische Ausgabe abgespeichert werden soll.
     */
    public void writeGraphicalOutput(FTDataElement left, FTDataElement right, File dir) {

        for (int i = 0; i < 2; i++) {
            //add deleted Notes
            for (Fragment fragTree : (i == 0 ? getTraversal(left.getTree().getRoot()) : getTraversal(right.getTree().getRoot()))) {
                FragmentationTreeWrapper treeWrapper = treeToWrapper.get(fragTree);
                if (!addedWrapper.contains(treeWrapper)) {
                    if (!treeWrapper.isRoot()) {
                        treeWrapper.getParent().addChild(treeWrapper);
                    }
                }
            }

            //write tree to dotFileList
            for (FragmentationTreeWrapper treeWrapper : (i == 0 ? (firstTreeMirrored ? getTraversalWrapperReverse(treeToWrapper.get(left.getTree().getRoot())) : getTraversalWrapper(treeToWrapper.get(left.getTree().getRoot()))) : getTraversalWrapper(treeToWrapper.get(right.getTree().getRoot())))) {
                MolecularFormula formula = treeWrapper.getFragmentationTree().getFormula();

                dotFile.add(makeNode((i == 0 ? leftNodes : rightNodes).get(formula) + (i == 0 ? "Left" : "Right"),
                        (i == 0 ? leftNodesLabel : rightNodesLabel).get(formula),
                        treeWrapper.getShape(), treeWrapper.getColor()));

                if (!treeWrapper.isRoot())
                    addEdge(treeWrapper.getParent().getFragmentationTree().getFormula(), formula, treeWrapper.getColor(), (i == 0 ? true : false), treeWrapper.getEdgeLength());
            }
        }

        //add edges for matched nodes
        dotFile.add("edge[dir=none]");
        for (Fragment[] fragmentationTrees : matchedList) {
            if (fragmentationTrees[0] == null || fragmentationTrees[1] == null) continue;


            dotFile.add(makeEdge("LeftLoss" + leftNodes.get(fragmentationTrees[0].getFormula()), "RightLoss" + rightNodes.get(fragmentationTrees[1].getFormula()), null, "\"dotted,bold\"", 4, null, false));
            dotFile.add("{rank=same; LeftLoss" + leftNodes.get(fragmentationTrees[0].getFormula()) + "; RightLoss" + rightNodes.get(fragmentationTrees[1].getFormula()) + "}");
        }

        //end
        dotFile.add("}");


        //create dot and svg File
        try {

            assert dir.isDirectory();


            //find next numeration
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String[] split = name.split("\\.");
                    //if ((split.length==1 || split.length==2) && isIntNumber(split[0])) return true;
                    if ((split.length == 2) && split[1].equals("dot") && isIntNumber(split[0])) return true;
                    return false;
                }
            });


            int lastIndex = 0;
            for (int i = 0; i < files.length; i++) {
                int number = Integer.parseInt(files[i].getName().split("\\.")[0]);
                if (number > lastIndex) lastIndex = number;
            }

            File logFile = new File(dir + "/" + "log");


            BufferedWriter bwLog = null;
            if (!logFile.exists()) {
                if (!logFile.exists()) {
                    bwLog = new BufferedWriter(new FileWriter(logFile,/* append */ true));
                    //System.out.println("don't exist");
                    bwLog.write("FileNumber Metabolite1 Metabolite2");
                    bwLog.newLine();
                }
            }

            if (bwLog == null) bwLog = new BufferedWriter(new FileWriter(logFile,/* append */ true));
            bwLog.write((lastIndex + 1) + ": " + left.getName() + " " + right.getName());
            bwLog.newLine();
            bwLog.flush();
            bwLog.close();


            //File file = new File(dir+"/"+left.getName()+right.getName()+".dot");
            File file = new File(dir + "/" + (lastIndex + 1) + ".dot");

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (String s : dotFile) {
                bw.write(s);
                bw.newLine();
            }
            bw.flush();
            bw.close();


            //save as svg
            final String dotPath; // = "C:\\Program Files (x86)\\Graphviz 2.28\\bin\\dot";
            {
                if (new File("/usr/local/bin/dot").exists()) {
                    dotPath = "/usr/local/bin/dot";
                } else if (new File("/usr/bin/dot").exists()) {
                    dotPath = "/usr/bin/dot";
                } else {
                    dotPath = "dot";
                    //throw new RuntimeException("Can't find dot");
                }
            }


            final ProcessBuilder proc = new ProcessBuilder(dotPath, "-T", "svg",
                    "-o", file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4) + ".svg", file.getAbsolutePath());

            proc.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


    }

	/*
     * Die folgenden Methoden werden während des Alignments aufgerufen - immer dann wenn Knoten gematcht,
	 * gejoined, deleted etc. werden. Du kannst die Funktionen entweder überschreiben und dir eine
	 * Alignmentdatenstruktur zusammenbasteln, oder direkt bei Methodenaufruf in die grafische Ausgabe
	 * etwas schreiben. Die Reihenfolge, in der die Methoden aufgerufen werden, ist nicht festgelegt.
	 * Theoretisch muesste dies in Post-Order Reihenfolge geschehen, also erst wird ein Blatt
	 * gematcht, dann seine Geschwister, dann seine Eltern usw. Aber auf diese Reihenfolge sollte man
	 * sich nicht verlassen.
	 * 
	 * Deleten ist etwas unintuitiv: Der übergebene Knoten ist NICHT der geloeschte Knoten, sondern der
	 * Knoten, der mit dem geloeschten Knoten aligniert wird. Ich glaub, die einfachste Art um an
	 * die geloeschten Knoten zu kommen, ist alle Knoten zu markieren die gematcht oder gejoined wurden
	 * und danach alle Knoten zu nehmen die nicht markiert sind.
	 */

    @Override
    public void deleteLeft(float score, Fragment node) {
        super.deleteLeft(score, node);
    }

    @Override
    public void deleteRight(float score, Fragment node) {
        super.deleteRight(score, node);
        ;
    }

    @Override
    public void match(float score, Fragment left,
                      Fragment right) {
        super.match(score, left, right);

        matchedList.add(new Fragment[]{left, right});

        String colorString = color.nextColor();
        addMatched(left, colorString);
        addMatched(right, colorString);


    }

    @Override
    public void matchVertices(float score, Fragment left,      //Methode für die root?
                              Fragment right) {
        super.matchVertices(score, left, right);

        matchedList.add(new Fragment[]{left, right});

        String colorString = color.nextColor();
        addMatched(left, colorString);
        addMatched(right, colorString);
    }


    private void addMatched(Fragment fragmentationTree, String color) {
        boolean backToRoot = true;
        Fragment current = fragmentationTree;

        FragmentationTreeWrapper currentWrapper = treeToWrapper.get(current);
        currentWrapper.setColor(color);

        while (backToRoot) {
            if (!addedWrapper.contains(currentWrapper) && !currentWrapper.isRoot()) {
                currentWrapper.getParent().addChild(currentWrapper);
                addedWrapper.add(currentWrapper);
                currentWrapper = currentWrapper.getParent();
            } else {
                backToRoot = false;
            }
        }
    }

    @Override
    public void join(float score, Iterator<Fragment> left,
                     Iterator<Fragment> right, int leftNumber, int rightNumber) {          //iteriert nach oben im baum
        super.join(score, left, right, leftNumber, rightNumber);
//                System.out.println("join");

        addJoined(left, right, color.nextColor());
    }

    private void addJoined(Iterator<Fragment> left, Iterator<Fragment> right, String color) {
        int leftLength = 0;
        Iterator<Fragment> fragmentationTreeIterator = left;
        Fragment last1 = null;
        while (fragmentationTreeIterator.hasNext()) {
            Fragment fT = fragmentationTreeIterator.next();
            leftLength++;
            if (last1 == null) last1 = fT;
        }

        int rightLength = 0;
        fragmentationTreeIterator = right;
        Fragment last2 = null;
        while (fragmentationTreeIterator.hasNext()) {
            Fragment fT = fragmentationTreeIterator.next();
            rightLength++;
            if (last2 == null) last2 = fT;
        }

        dotFile.add("{rank=same; " + leftNodes.get(last1.getFormula()) + "Left ; " + rightNodes.get(last2.getFormula()) + "Right}");

        int max = Math.max(leftLength, rightLength);

        for (int i = 0; i <= 1; i++) {
            FragmentationTreeWrapper currentWrapper = treeToWrapper.get((i == 0 ? last1 : last2));
            boolean backToRoot = true;

            for (int j = 0; j < (i == 0 ? leftLength : rightLength); j++) {
                currentWrapper.setColor(color);
                currentWrapper.setEdgeLength((1.0 * max / (i == 0 ? leftLength : rightLength) * minEdgeLength));
                currentWrapper.setShape("octagon");

                if (backToRoot && !addedWrapper.contains(currentWrapper) && !currentWrapper.isRoot()) {
                    currentWrapper.getParent().addChild(currentWrapper);
                    addedWrapper.add(currentWrapper);
                    currentWrapper = currentWrapper.getParent();
                } else {
                    backToRoot = false;
                }
            }
        }
    }


    //helper methods for graph building

    private String makeNode(String name, String label, String shape, String color) {
        return name
                + "["
                + (label == null ? (enumerateNodes ? ("label=\"" + (enumerator++) + "\", ") : "") : "label=\"" + (enumerateNodes ? ((enumerator++) + "\\n") : "") + label + "\", ")
                + (shape == null ? "" : "shape=" + shape + ", ")
                //+ "ordering=out, "
                + "style=filled, "
                + (color == null ? "" : "color=\"" + color + "\", ")
                + "]";
    }

    private String makeEdge(String out, String in, String label, String style, double length, String group, boolean constraint) {
        return out
                + " -> "
                + in
                + " ["
                + (label == null ? (enumerateNodes ? ("label=\"" + (enumerator++) + "\", ") : "") : "label=\"" + (enumerateNodes ? ((enumerator++) + "\\n") : "") + label + "\", ")
                + (style == null ? "" : "style=" + style + ", ")
                + (length == 0 ? "" : "len=" + length + ", ")
                //+ (weight==0 ? "" : "weight="+weight+", ")
                + (group == null ? "" : "group=" + group + ", ")
                + (constraint == true ? "" : "constraint=false, ")
                + (length == 0 ? "" : "minlen=" + length + ", ")
                + "]";

    }


    private void addEdge(MolecularFormula parent, MolecularFormula child, String color, boolean left, double length) {
        FormulaEdge formulaEdge = new FormulaEdge(parent, child);

        if (!(left == true ? leftEdges : rightEdges).containsKey(formulaEdge)) {
            return;
        }

        String fragment = (left == true ? leftNodes.get(formulaEdge.getIn()) : rightNodes.get(formulaEdge.getIn()));

        //Edge to Node
        dotFile.add(makeNode((left == true ? "LeftLoss" : "RightLoss") + fragment,
                (left == true ? leftEdges : rightEdges).get(formulaEdge),
                "box", color));


        //add Edges
        if (left == true) {
            dotFile.add(makeEdge(leftNodes.get(formulaEdge.getOut()) + "Left", "LeftLoss" + fragment, null, null, Math.round(length * 100) / 100, "group1", true));
            dotFile.add(makeEdge("LeftLoss" + fragment, leftNodes.get(formulaEdge.getIn()) + "Left", null, null, Math.round(length * 100) / 100, "group1", true));
        } else {
            dotFile.add(makeEdge(rightNodes.get(formulaEdge.getOut()) + "Right", "RightLoss" + fragment, null, null, Math.round(length * 100) / 100, "group2", true));
            dotFile.add(makeEdge("RightLoss" + fragment, rightNodes.get(formulaEdge.getIn()) + "Right", null, null, Math.round(length * 100) / 100, "group2", true));
        }
    }


    private List<Fragment> getTraversal(Fragment fragmentationTree) {
        List<Fragment> ordering = new LinkedList<Fragment>();
        Queue<Fragment> queue = new LinkedList<Fragment>();

        queue.add(fragmentationTree);
        while (!queue.isEmpty()) {
            Fragment current = queue.poll();
            ordering.add(current);
            for (Fragment child : current.getChildren()) {
                queue.add(child);
            }
        }

        return ordering;
    }

    private List<FragmentationTreeWrapper> getTraversalWrapper(FragmentationTreeWrapper fragmentationTree) {
        List<FragmentationTreeWrapper> ordering = new LinkedList<FragmentationTreeWrapper>();
        Queue<FragmentationTreeWrapper> queue = new LinkedList<FragmentationTreeWrapper>();

        queue.add(fragmentationTree);
        while (!queue.isEmpty()) {
            FragmentationTreeWrapper current = queue.poll();
            ordering.add(current);
            for (FragmentationTreeWrapper child : current.getChildren()) {
                queue.add(child);
            }
        }

        return ordering;
    }

    private List<FragmentationTreeWrapper> getTraversalWrapperReverse(FragmentationTreeWrapper fragmentationTree) {
        List<FragmentationTreeWrapper> ordering = new LinkedList<FragmentationTreeWrapper>();
        Queue<FragmentationTreeWrapper> queue = new LinkedList<FragmentationTreeWrapper>();

        queue.add(fragmentationTree);
        while (!queue.isEmpty()) {
            FragmentationTreeWrapper current = queue.poll();
            ordering.add(current);
            while (!current.getChildren().isEmpty()) {
                queue.add(current.getChildren().removeLast());
            }
        }

        return ordering;
    }

    public boolean isIntNumber(String num) {
        try {
            Integer.parseInt(num);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static class FormulaEdge {
        private MolecularFormula formula1;
        private MolecularFormula formula2;

        FormulaEdge(MolecularFormula formula1, MolecularFormula formula2) {
            this.formula1 = formula1;
            this.formula2 = formula2;
        }

        MolecularFormula getOut() {
            return formula1;
        }

        MolecularFormula getIn() {
            return formula2;
        }

        public int hashCode() {
            return 17 * formula1.hashCode() + 37 * formula2.hashCode();
        }

        public boolean equals(Object object) {
            if (!(object instanceof FormulaEdge)) return false;
            FormulaEdge formulaEdge = (FormulaEdge) object;
            return ((formula1.equals(formulaEdge.getOut()) && formula2.equals(formulaEdge.getIn())) ? true : false);
        }

    }

    private static class Color {
        private final static String firstColor = "ff0000";
        private final int stepsize = 11983725;
        private final int maxValue = 16777216;
        private String color = firstColor;

        public String nextColor() {
            String current = color;
            changeColor();
            return "#" + current;
        }

        private void changeColor() {
            color = Integer.toHexString(((Integer.parseInt(color, 16) + stepsize) % maxValue));
        }

        public void reset() {
            color = firstColor;
        }
    }

    private static class FragmentationTreeWrapper {
        private String color;
        private String shape;
        private double edgeLength;
        private Fragment fragmentationTree;
        private FragmentationTreeWrapper parent;
        private LinkedList<FragmentationTreeWrapper> children;


        private FragmentationTreeWrapper(Fragment fragmentationTree) {
            this.fragmentationTree = fragmentationTree;
            children = new LinkedList<FragmentationTreeWrapper>();
            color = "gray";

        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        public double getEdgeLength() {
            return edgeLength;
        }

        public void setEdgeLength(double edgeLength) {
            this.edgeLength = edgeLength;
        }

        public LinkedList<FragmentationTreeWrapper> getChildren() {
            return children;
        }

        public void setChildren(LinkedList<FragmentationTreeWrapper> children) {
            this.children = children;
        }

        public void addChild(FragmentationTreeWrapper child) {
            children.add(child);
        }

        public FragmentationTreeWrapper getParent() {
            return parent;
        }

        public void setParent(FragmentationTreeWrapper parent) {
            this.parent = parent;
        }

        public boolean isRoot() {
            return (fragmentationTree.isRoot());
        }

        public Fragment getFragmentationTree() {
            return fragmentationTree;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FragmentationTreeWrapper)) return false;

            FragmentationTreeWrapper that = (FragmentationTreeWrapper) o;

            if (fragmentationTree != null ? !fragmentationTree.equals(that.fragmentationTree) : that.fragmentationTree != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return fragmentationTree != null ? fragmentationTree.hashCode() : 0;
        }
    }


}


