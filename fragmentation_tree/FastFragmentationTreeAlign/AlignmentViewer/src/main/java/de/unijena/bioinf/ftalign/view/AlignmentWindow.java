
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

package de.unijena.bioinf.ftalign.view;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ftalign.CommonLossScoring;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.ftalign.analyse.TreeSizeNormalizer;
import de.unijena.bioinf.ftalign.graphics.GraphicalBacktrace2;
import de.unijena.bioinf.treealign.AlignmentTree;
import de.unijena.bioinf.treealign.AlignmentTreeBacktrace;
import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.treealign.sparse.DPSparseTreeAlign;
import net.miginfocom.swing.MigLayout;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AlignmentWindow extends JPanel {

    private final static String[] PATHS = new String[]{"/usr/bin/dot", "/usr/local/bin/dot"};

    private final ApplicationState state;
    private PairList pairList;
    private float activeAlignmentScore;
    private View view;
    private ComputeAlignment computer;
    private ApplicationWindow window;
    private File dotPath;
    private InfoBox infoBox;
    private ScoringParameters params;

    private Pair activePair;

    public AlignmentWindow(ApplicationWindow window, ApplicationState state) {
        super();
        this.state = state;
        this.window = window;
        System.out.println(state.getSubset().size());
        this.setLayout(new BorderLayout());
        pairList = new PairList();
        view = new View();
        setPreferredSize(new Dimension(1024, 768));
        add(pairList, BorderLayout.WEST);
        add(view, BorderLayout.CENTER);
        this.infoBox = new InfoBox();
        add(infoBox, BorderLayout.SOUTH);
        this.params = new ScoringParameters();
        add(params, BorderLayout.EAST);
        setActivePair(state.getSubset().get(0));
    }

    public synchronized void setActivePair(Pair activePair) {
        final Pair oldPair = this.activePair;
        if (activePair != oldPair) {
            System.out.println("UPDATE ACTIVE PAIR");
            this.activePair = activePair;
            final ComputeAlignment oldComputer = computer;
            this.computer = new ComputeAlignment(activePair);
            window.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
            if (oldComputer != null) {
                System.out.println("CANCEL old pair");
                oldComputer.cancel(false);
            }
            computer.execute();
        }
    }

    public Pair getActivePair() {
        return activePair;
    }

    private class InfoBox extends JPanel {

        private JLabel alignScore, tanimotoScore, treeSizeLeft, treeSizeRight;

        public InfoBox() {
            this.alignScore = new JLabel("");
            this.tanimotoScore = new JLabel("");
            this.treeSizeLeft = new JLabel("");
            this.treeSizeRight = new JLabel("");
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(alignScore);
            add(Box.createHorizontalGlue());
            add(tanimotoScore);
            add(Box.createHorizontalGlue());
            add(treeSizeLeft);
            add(Box.createHorizontalGlue());
            add(treeSizeRight);
        }

        public void updateLabels() {
            alignScore.setText(String.format(Locale.US, "Alignment Score: %.2f", activeAlignmentScore));
            tanimotoScore.setText(String.format(Locale.US, "Tanimoto Score: %.2f %%", 100d * activePair.getTanimoto()));
            treeSizeLeft.setText(String.format(Locale.US, "Left Size: %3d;", activePair.getLeft().getTree().numberOfVertices()));
            treeSizeRight.setText(String.format(Locale.US, "Right Size: %3d", activePair.getRight().getTree().numberOfVertices()));
        }

    }

    private class PairList extends JPanel {

        private JList<Pair> alignments;
        private JList<Pair> decoys;
        private DefaultListModel<Pair> model;
        private DefaultListModel<Pair> decoyModel;

        private PairList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("Alignments"));
            this.model = new DefaultListModel<Pair>();
            this.alignments = new JList<Pair>(model);
            int k = 0;
            for (Pair pair : state.getSubset()) model.add(k++, pair);
            System.out.println(model.size() + " alignments");
            alignments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            alignments.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    setActivePair(model.get(alignments.getSelectedIndex()));
                }
            });
            //alignments.setPreferredSize(new Dimension(110, 768));
            final JScrollPane scrollPane = new JScrollPane(alignments);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            add(scrollPane);
            final JPanel decoyCreationPanel = new JPanel();
            decoyCreationPanel.setLayout(new BoxLayout(decoyCreationPanel, BoxLayout.X_AXIS));
            decoyCreationPanel.setPreferredSize(new Dimension(120, 32));
            decoyCreationPanel.setMaximumSize(new Dimension(200, 32));
            final JButton createDecoys = new JButton();
            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.4, 0d, 1d, 0.05));
            createDecoys.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createDecoys(((SpinnerNumberModel) spinner.getModel()).getNumber().floatValue());
                }
            });
            createDecoys.setText("Create Decoys");
            decoyCreationPanel.add(createDecoys);
            decoyCreationPanel.add(Box.createHorizontalGlue());
            decoyCreationPanel.add(spinner);
            add(decoyCreationPanel);
            decoyModel = new DefaultListModel<Pair>();
            decoys = new JList<Pair>(decoyModel);
            decoys.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    setActivePair(decoyModel.get(decoys.getSelectedIndex()));
                }
            });
            final JScrollPane decoyPane = new JScrollPane(decoys);
            add(decoyPane);
        }

        private void createDecoys(final float v) {
            window.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
            SwingWorker<Object, Pair> worker = new SwingWorker<Object, Pair>() {
                @Override
                protected Object doInBackground() throws Exception {
                    final Set<DataElement> elems = new HashSet<DataElement>();
                    double avgTanimoto = 0d;
                    double avgAlign = 0d;
                    for (Pair a : state.getSubset()) {
                        elems.add(a.getLeft());
                        elems.add(a.getRight());
                        avgTanimoto += a.getTanimoto();
                        avgAlign += align(a.getLeft(), a.getRight());
                    }
                    avgAlign /= state.getSubset().size();
                    state.getDecoys().clear();
                    for (DataElement e : elems) {
                        // find decoys
                        int i = 0;
                        for (DataElement f : state.getTreeMap().values()) {
                            if (e.tanimoto(f) < v && align(e, f) > avgAlign) {
                                ++i;
                                publish(new Pair(e, f));
                            }
                        }
                        System.out.println(i + " alignments");
                    }
                    return null;
                }

                private double align(DataElement a, DataElement b) {
                    final double value = new DPSparseTreeAlign<Fragment>(new StandardScoring(true), true, a.getTree().getRoot(),
                            b.getTree().getRoot(),
                            FTree.treeAdapterStatic()).compute();
                    return new TreeSizeNormalizer(0.5d).normalize(a.getTree(), b.getTree(), new StandardScoring(true), (float) value);
                }

                @Override
                protected void process(List<Pair> chunks) {
                    super.process(chunks);
                    for (Pair pair : chunks)
                        decoyModel.add(decoyModel.size(), pair);
                }

                @Override
                protected void done() {
                    /*
                    decoyModel.clear();
                    int k=0;
                    for (Pair x : state.getDecoys()) {
                        decoyModel.add(k++, x);
                    }
                    */
                    window.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    PairList.this.repaint();
                }
            };
            worker.execute();
        }

    }

    private Scoring<Fragment> getScoring() {
        return params.scoring;
    }

    private int getNumberOfJoins() {
        return 1;
    }

    private class View extends JPanel {

        private JSVGCanvas canvas;

        private View() {
            this.canvas = new JSVGCanvas();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            canvas.setMinimumSize(new Dimension(1024 - 120, 768 - 64));
            add(canvas);
            canvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
                @Override
                public void gvtRenderingCompleted(GVTTreeRendererEvent gvtTreeRendererEvent) {
                    window.getContentPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    view.canvas.resetRenderingTransform();
                }
            });
        }

    }

    private class ComputeAlignment extends SwingWorker<SVGDocument, Object> {

        private final Pair pair;
        private float score;

        private ComputeAlignment(Pair pair) {
            this.pair = pair;
        }

        @Override
        protected SVGDocument doInBackground() throws Exception {
            System.out.println("START COMPUTING");
            final Scoring<Fragment> scoring = getScoring();
            final DPMultiJoin<Fragment> dp = new DPMultiJoin<Fragment>(scoring, getNumberOfJoins(),
                    pair.getLeft().getTree().getRoot(), pair.getRight().getTree().getRoot(), FTree.treeAdapterStatic());
            this.score = dp.compute();
            if (params.normalize) {
                this.score = (float) new TreeSizeNormalizer(0.5d).normalize(pair.getLeft().getTree(),
                        pair.getRight().getTree(), scoring, score);
            }
            if (score == 0) return getSvgFromDot("strict digraph {\nv1 [label=\"Empty Alignment\"];\n}");
            if (isCancelled()) return null;
            final ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
            final AlignmentTreeBacktrace<Fragment> backtrace = new AlignmentTreeBacktrace<Fragment>(FTree.treeAdapterStatic());
            dp.backtrace(backtrace);
            if (isCancelled()) return null;
            final AlignmentTree<Fragment> alignment = backtrace.getAlignmentTree();
            if (isCancelled()) return null;
            final GraphicalBacktrace2 gfx = new GraphicalBacktrace2(new PrintStream(bout), pair.getLeft().getTree(), pair.getRight().getTree(),
                    alignment);
            gfx.setPrintPrettyFormulas(false);

            gfx.setCellPadding(20);
            gfx.print();
            if (isCancelled()) return null;
            System.out.println("FINISH COMPUTING");
            return getSvgFromDot(new String(bout.toByteArray()));
        }

        private SVGDocument getSvgFromDot(String dotString) throws IOException {
            System.out.println(dotString);
            final ProcessBuilder builder = new ProcessBuilder(getDotPath().getAbsolutePath(), "-T", "svg");
            final Process proc = builder.start();
            final Writer writer = new OutputStreamWriter(proc.getOutputStream());
            writer.write(dotString);
            writer.close();
            final String parser = XMLResourceDescriptor.getXMLParserClassName();
            final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            return (SVGDocument) factory.createDocument("file://", new BufferedInputStream(proc.getInputStream()));
        }

        private File getDotPath() {
            if (dotPath != null) return dotPath;
            final String os = System.getProperty("os.name").toLowerCase();
            final boolean isWindoof = os.contains("win");
            // check PATH variable
            final String path = System.getenv("PATH");
            if (path != null) {
                for (String dir : path.split(isWindoof ? ";" : ":")) {
                    final File exec = new File(dir, isWindoof ? "dot.exe" : "dot");
                    if (exec.exists()) {
                        dotPath = exec;
                        return exec;
                    }
                }
            }
            if (!isWindoof) {
                for (String s : PATHS) {
                    final File f = new File(s);
                    if (f.exists()) {
                        dotPath = f;
                        return f;
                    }
                }
            }
            throw new RuntimeException("Can't find Graphviz. Please add dot command line tool to PATH variable.");
        }

        @Override
        protected void done() {
            if (computer != this) {
                System.out.println("IS CANCELED");
                return;
            }
            System.out.println("SETUP CANVAS");
            try {
                final SVGDocument document = get();
                synchronized (view.canvas) {
                    if (document == null || computer != this) {
                        System.out.println("IS CANCELED");
                        return;
                    }
                    activeAlignmentScore = score;
                    infoBox.updateLabels();
                    infoBox.repaint();
                    view.canvas.setDocument(document);
                    view.canvas.setMinimumSize(new Dimension(1024 - 120, 768 - 64));
                    //view.canvas.setPreferredSize(new Dimension(1024-120, 768-64));
                    //view.canvas.repaint();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private class ScoringParameters extends JPanel {

        private final List<JSpinner> spinners;
        private final List<ScoringParameter> parameters;
        private StandardScoring scoring;
        private boolean normalize = false;

        private String[] ALL_ATTRS = new String[]{
                "matchScore", "scoreForEachNonHydrogen", "missmatchPenalty", "penaltyForEachNonHydrogen",
                "lossMatchScore", "lossScoreForEachNonHydrogen", "lossMissmatchPenalty", "lossPenaltyForEachNonHydrogen",
                "penaltyForEachJoin", "gapScore",
                "joinMatchScore", "joinScoreForEachNonHydrogen", "joinMissmatchPenalty", "joinPenaltyForEachNonHydrogen"
        };

        private List<ScoringParameter> getParametersAll() {
            final ArrayList<ScoringParameter> params = new ArrayList<ScoringParameter>(ALL_ATTRS.length);
            for (String n : ALL_ATTRS) params.add(new ScoringParameter(n, n));
            return params;
        }

        private ScoringParameters() {
            setLayout(new MigLayout("", "[left][right]"));
            this.scoring = new StandardScoring(true);
            this.parameters = getParametersAll();
            this.spinners = new ArrayList<JSpinner>();
            final JCheckBox useTolerantScoring = new JCheckBox();
            add(new JLabel("Tolerant Scoring"));
            add(useTolerantScoring, "wrap");
            useTolerantScoring.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (useTolerantScoring.getModel().isSelected())
                        scoring = new CommonLossScoring(true);
                    else
                        scoring = new StandardScoring(true);
                    updateScoring();
                }
            });
            add(new JLabel("normalize"));
            final JCheckBox doNormalize = new JCheckBox();
            doNormalize.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    normalize = doNormalize.getModel().isSelected();
                }
            });
            add(doNormalize, "wrap");
            for (ScoringParameter param : parameters) {
                final JSpinner spinner = new JSpinner(new SpinnerNumberModel(param.get(), -100d, 100d, 0.1d));
                //final JPanel spinnerBox = new JPanel();
                //spinnerBox.setLayout(new BoxLayout(spinnerBox, BoxLayout.X_AXIS));
                spinner.setName(param.name);
                spinner.addChangeListener(param);
                spinners.add(spinner);
                add(new JLabel(param.name));
                add(spinner, "wrap");
            }
            final JButton recompute = new JButton();
            recompute.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final Pair old = activePair;
                    activePair = null;
                    updateScoring();
                    setActivePair(old);
                }
            });
            recompute.setText("Recompute");
            add(recompute, "span 2");
        }

        private void updateScoring() {
            int k = 0;
            for (ScoringParameter param : parameters)
                param.set(((Number) spinners.get(k++).getModel().getValue()).floatValue());
        }

        private class ScoringParameter implements ChangeListener {

            private final String attrName;
            private final String name;

            private ScoringParameter(String name, String attrName) {
                this.name = name;
                this.attrName = attrName;
            }

            public float get() {
                try {
                    return (Float) (scoring.getClass().getDeclaredField(attrName).get(scoring));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

            public void set(float value) {
                try {
                    scoring.getClass().getDeclaredField(attrName).set(scoring, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void stateChanged(ChangeEvent e) {
                set(
                        (
                                (SpinnerNumberModel) (((JSpinner) e.getSource()).getModel())).getNumber().floatValue()
                );
            }
        }

    }
}
