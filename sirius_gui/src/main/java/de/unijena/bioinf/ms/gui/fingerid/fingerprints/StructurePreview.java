/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.ExtendedConnectivityProperty;
import de.unijena.bioinf.ChemistryBase.fp.SubstructureProperty;
import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.properties.MolecularStructuresDisplayColors;
import de.unijena.bioinf.ms.gui.utils.ThemedAtomColors;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.color.UniColor;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StructurePreview extends JPanel implements Runnable, PropertyChangeListenerEDT {

    protected static Logger logger = LoggerFactory.getLogger(StructurePreview.class);
    protected final FingerprintVisualization[] visualizations;
    protected final SMARTSQueryTool queryTool;
    protected final Thread backgroundThread;
    protected final AtomContainerRenderer renderer;
    protected volatile FingerIdPropertyBean entry;
    protected volatile IAtomContainer[] depiction;
    protected volatile int state = 0; // needRefresh=0, recalculated=1, done=2
    protected volatile boolean shutdown = false;

    public StructurePreview(FingerprintList table) {
        this(table.visualizations, table.gui);
    }

    public StructurePreview(FingerprintVisualization[] visualizations, SiriusGui gui) {
        gui.getProperties().addPropertyChangeListener("molecularStructuresDisplayColors", this);

        setBackground(Colors.BACKGROUND);
        this.visualizations = visualizations;
        this.entry = null;
        this.backgroundThread = new Thread(this);
        this.queryTool = new SMARTSQueryTool("C=C", SilentChemObjectBuilder.getInstance());
        queryTool.setQueryCacheSize(visualizations.length);
        state = 2;

        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(Fonts.FONT_MEDIUM.deriveFont(13f)));
        // setup the renderer
        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());

        renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                StandardGenerator.HighlightStyle.Colored);
        setAtomColoring(gui.getProperties().getMolecularStructureDisplayColors());

        setPreferredSize(new Dimension(0, 220));
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state < 1) return;
        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setBackground(Colors.BACKGROUND);
        final IAtomContainer[] mols = depiction;
        if (mols == null || mols.length==0) {
            g2d.clearRect(0,0,getWidth(),getHeight());
            return;
        }
        final int widthPerCompound = getWidth() / 6;
        final int offset = (getWidth() - (mols.length*widthPerCompound))/2;
        final int length = depiction.length;
        for (int i = 0; i < length; ++i) {
            final int x=offset+widthPerCompound*i, y=5, w=widthPerCompound, h=200;
            if (depiction[i] == null) {
                g2d.clearRect(x,y,w,h);
            }else {
                renderer.paint(depiction[i], new AWTDrawVisitor(g2d),
                        new Rectangle2D.Double(x,y,w,h), true);
            }
        }


    }

    public void setMolecularProperty(FingerIdPropertyBean entry) {
        if (entry == this.entry) return;
        synchronized (this) {
            this.entry = entry;
            state = 0;
            this.notify();
        }
    }

    /**
     * Called when the panel is added to a visible container.
     * This is the best place to start the background thread.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (backgroundThread != null && !backgroundThread.isAlive()) {
            backgroundThread.start();
        }
    }

    /**
     * Called just before the panel is removed from its container.
     * This is the perfect place to stop the background thread.
     */
    @Override
    public void removeNotify() {
        stop();
        super.removeNotify();
    }

    /**
     * Stops the background thread.
     */
    public void stop() {
        synchronized (this) {
            this.shutdown = true;
            if (backgroundThread != null) {
                backgroundThread.interrupt(); // Wake up the thread from wait()
            }
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (state == 0 && entry != null) {
                    final FingerprintVisualization viz;
                    final FingerIdPropertyBean entry;
                    synchronized (this) {
                        entry = this.entry;
                        viz = this.visualizations[entry.absoluteIndex];
                    }
                    IAtomContainer[] depiction;
                    if (viz==null) {
                        //todo kaidu: this code seems to be obsolete and induced errors
                        /*if (entry.getMolecularProperty().getClass() == SubstructureProperty.class) {
                            // try to parse the SMARTS itself
                            SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                            parser.kekulise(true);
                            depiction = new IAtomContainer[]{parser.parseSmiles(((SubstructureProperty)(entry.getMolecularProperty())).getSmarts())};

                        }*/

                        depiction = new IAtomContainer[0];
                    } else {
                        final SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                        parser.kekulise(true);
                        StructureDiagramGenerator sdg = new StructureDiagramGenerator();

                        if (entry.getMolecularProperty() instanceof SubstructureProperty) {
                            depiction = new IAtomContainer[viz.getNumberOfExamples()];
                            for (int i = 0; i < viz.getNumberOfExamples(); ++i) {
                                try {
                                    depiction[i] = parser.parseSmiles(viz.getExample(i));
                                    sdg.setMolecule(depiction[i]);
                                    sdg.generateCoordinates();
                                    depiction[i] = sdg.getMolecule();
                                    highlightWithFp(depiction[i], (SubstructureProperty) entry.getMolecularProperty());
                                } catch (CDKException | NullPointerException e) {
                                    logger.error(e.getMessage(), e);
                                    depiction[i] = null; // do not draw the molecule
                                }
                            }
                        } else if (entry.getMolecularProperty() instanceof ExtendedConnectivityProperty) {
                            depiction = new IAtomContainer[viz.getNumberOfExamples()];
                            for (int i = 0; i < viz.getNumberOfExamples(); ++i) {
                                try {
                                    depiction[i] = parser.parseSmiles(viz.getExample(i));
                                    sdg.setMolecule(depiction[i]);
                                    sdg.generateCoordinates();
                                    depiction[i] = sdg.getMolecule();
                                    highlight(depiction[i], viz.getSmarts());
                                } catch (CDKException | NullPointerException e) {
                                    logger.error(e.getMessage(), e);
                                    depiction[i] = null; // do not draw the molecule
                                }
                            }
                        } else {
                            depiction = new IAtomContainer[0];
                        }
                        // remove null elements from depiction array
                        int nonNull=0;
                        for (int i=0; i < depiction.length; ++i)
                            if (depiction[i]!=null)
                                ++nonNull;
                        if (nonNull != depiction.length) {
                            final IAtomContainer[] compact = new IAtomContainer[nonNull];
                            int k=0;
                            for (int i=0; i < depiction.length; ++i)
                                if (depiction[i]!=null)
                                    compact[k++] = depiction[i];
                            depiction = compact;
                        }
                    }
                    synchronized (this) {
                        this.depiction = depiction;
                        state = 1;
                    }
                    repaint();
                } else {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            logger.debug(e.getMessage(), e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    private void highlightWithFp(IAtomContainer molecule, SubstructureProperty molecularProperty) throws CDKException {
        queryTool.setSmarts(molecularProperty.getSmarts());
        if (queryTool.matches(molecule)) {
            final List<Integer> inds = queryTool.getMatchingAtoms().get(0);
            if (inds.isEmpty()) {
                return;
            }
            final HashSet<IAtom> atoms = new HashSet<>(inds.size());
            for (int i : inds) {
                atoms.add(molecule.getAtom(i));
            }
            colorBackgroundAtomsAndBonds(molecule, atoms);
        } else {
        }
    }

    private void highlight(IAtomContainer molecule, String highlightingExample) throws CDKException {
        queryTool.setSmarts(highlightingExample);
        if (queryTool.matches(molecule)) {
            final List<Integer> inds = queryTool.getMatchingAtoms().get(0);
            final HashSet<IAtom> atoms = new HashSet<>(inds.size());
            for (int i : inds) {
                atoms.add(molecule.getAtom(i));
            }
            colorBackgroundAtomsAndBonds(molecule, atoms);
        }
    }

    private void colorBackgroundAtomsAndBonds(IAtomContainer molecule, HashSet<IAtom> atoms) {
        //put unselected atoms to background by "highlighting" with unobtrusive color
        for (IAtom atom : molecule.atoms()) {
            if (atoms.contains(atom)) continue;

            atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, Colors.MolecularStructures.BACKGROUND_STRUCTURE);
            for (IBond b : molecule.getConnectedBondsList(atom)) {
                if (!atoms.contains(b.getAtom(0)) || !atoms.contains(b.getAtom(1))) {
                    b.setProperty(StandardGenerator.HIGHLIGHT_COLOR, Colors.MolecularStructures.BACKGROUND_STRUCTURE);
                }
            }
        }
    }

    @Override
    public void propertyChangeInEDT(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getNewValue() instanceof MolecularStructuresDisplayColors mode) {
            setAtomColoring(mode);
            repaint();
        }
    }

    private void setAtomColoring(MolecularStructuresDisplayColors mode) {
        if (mode == MolecularStructuresDisplayColors.MONOCHROME) {
            Color chosenColor = Colors.FOREGROUND_DATA;
            IAtomColorer atomColorer = new UniColor(chosenColor);
            renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class, atomColorer);
        } else {
            renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class,
                    new ThemedAtomColors(Colors.MolecularStructures.SELECTED_SUBSTRUCTURE));
        }
    }
}
