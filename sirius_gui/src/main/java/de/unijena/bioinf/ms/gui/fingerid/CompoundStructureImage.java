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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.jjobs.PropertyChangeListenerEDT;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.properties.MolecularStructuresDisplayColors;
import de.unijena.bioinf.ms.gui.utils.ThemedAtomColors;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

class CompoundStructureImage extends JPanel implements PropertyChangeListenerEDT {

    protected static final Font formulaFont, scoreFont;
    private static final DecimalFormat decimalFormat = new DecimalFormat("#0.000");

    static {
        //init fonts
        final Font tempFont = Fonts.FONT_MEDIUM;
        formulaFont = tempFont.deriveFont(14f);
        scoreFont = tempFont.deriveFont(18f);
    }

    protected FingerprintCandidateBean molecule;
    protected AtomContainerRenderer renderer;
    protected Color backgroundColor;

    public CompoundStructureImage(SiriusGui gui) {
        this(StandardGenerator.HighlightStyle.OuterGlow, gui);
    }

    public CompoundStructureImage(StandardGenerator.HighlightStyle highlightStyle, SiriusGui gui) {
        gui.getProperties().addPropertyChangeListener("molecularStructuresDisplayColors", this);

        setOpaque(false);
        setPreferredSize(new Dimension(374, 215));
        // make generators
        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(formulaFont));

        // setup the renderer
        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());

        renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                highlightStyle);
        setAtomColoring(gui.getProperties().getMolecularStructureDisplayColors());

        renderer.getRenderer2DModel().set(StandardGenerator.AnnotationColor.class,
                Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
        setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (molecule.hasAtomContainer())
            renderImage((Graphics2D) g);
    }

    private void renderImage(final Graphics2D gg) {
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(molecule.getMolecule(), false);
        try {
            sdg.generateCoordinates();
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
        renderer.getRenderer2DModel().set(BasicSceneGenerator.BackgroundColor.class, backgroundColor);
        synchronized (molecule.getCandidate()) {
            renderer.paint(molecule.getMolecule(), new AWTDrawVisitor(gg),
                    new Rectangle2D.Double(7, 14, 360, 185), true);
        }

        gg.setFont(formulaFont);
        gg.setColor(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
        final String fromulaString = molecule.getMolecularFormula();
        final Rectangle2D bound = gg.getFontMetrics().getStringBounds(fromulaString, gg);
        {
            final int x = 3;
            final int y = 0; // top-left
            final int h = (int) (y + bound.getHeight());
            gg.drawString(fromulaString, x, h - 2);
        }

        //todo change to gif
        final String scoreText = decimalFormat.format(molecule.getScore());
        double tw = gg.getFontMetrics(scoreFont).getStringBounds(scoreText, gg).getWidth();

        gg.setFont(scoreFont);
        gg.setColor(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
        gg.drawString(scoreText, (int) (getWidth() - (tw + 4)), getHeight() - 4);
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
            IAtomColorer atomColorer = new UniColorHighlightingAware();
            renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class, atomColorer);
        } else {
            renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class, new ThemedAtomColors());
        }
    }

    private static class UniColorHighlightingAware implements IAtomColorer {

        @Override
        public Color getAtomColor(IAtom atom) {
            if (atom.getProperty(StandardGenerator.HIGHLIGHT_COLOR) == null) {
                //normal atom without highlighting
                return Colors.MolecularStructures.SELECTED_SUBSTRUCTURE;
            } else {
                //with substructure highlighting
                return Colors.MolecularStructures.SELECTED_SUBSTRUCTURE_WITH_GLOW_HIGHLIGHT;
            }
        }

    }
}
