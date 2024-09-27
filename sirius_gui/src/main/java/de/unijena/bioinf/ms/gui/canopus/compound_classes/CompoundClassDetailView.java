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

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import io.sirius.ms.sdk.model.CompoundClass;
import io.sirius.ms.sdk.model.CompoundClasses;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.WrapLayout;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class CompoundClassDetailView extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {
    private final JScrollPane containerSP;

    protected CompoundClasses compoundClasses = null;
    protected CompoundClass mainClass = null;

    protected JPanel mainClassPanel, descriptionPanel, alternativeClassPanels, npcPanel;

    protected JPanel container;

    public CompoundClassDetailView(FormulaList siriusResultElements) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(0f);
        mainClassPanel = new JPanel();
        mainClassPanel.setAlignmentX(0f);
        mainClassPanel.setLayout(new WrapLayout(WrapLayout.LEFT));

        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout());
        descriptionPanel.setAlignmentX(0f);

        alternativeClassPanels = new JPanel();
        alternativeClassPanels.setLayout(new WrapLayout(WrapLayout.LEFT));
        alternativeClassPanels.setAlignmentX(0f);

        npcPanel = new JPanel();
        npcPanel.setLayout(new WrapLayout(WrapLayout.LEFT));
        npcPanel.setAlignmentX(0f);

        container = new JPanel();
        container.setAlignmentX(0);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        containerSP = new JScrollPane(container, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(containerSP);

        siriusResultElements.addActiveResultChangedListener(this);

    }

    public void updateContainer() {
        container.removeAll();
        if (compoundClasses != null) {

            // cf lineage panel
            mainClassPanel.removeAll();
            if (compoundClasses.getClassyFireLineage() != null && !compoundClasses.getClassyFireLineage().isEmpty()) {
                Iterator<CompoundClass> it = compoundClasses.getClassyFireLineage().iterator();
                while (it.hasNext()) {
                    CompoundClass cfClass =  it.next();
                    mainClassPanel.add(new ClassifClass(cfClass, cfClass == mainClass));
                    if (it.hasNext()) {
                        final JLabel comp = new JLabel("\u27be");
                        comp.setFont(Fonts.FONT_BOLD.deriveFont(18f));
                        mainClassPanel.add(comp);
                    }
                }
            }

            // cf alt panel
            alternativeClassPanels.removeAll();
            if (compoundClasses.getClassyFireAlternatives() != null && !compoundClasses.getClassyFireAlternatives().isEmpty())
                compoundClasses.getClassyFireAlternatives().
                        forEach(cfClass -> alternativeClassPanels.add(new ClassifClass(cfClass, false)));

            // npc panel
            npcPanel.removeAll();
            Stream.of(compoundClasses.getNpcPathway(), compoundClasses.getNpcSuperclass(), compoundClasses.getNpcClass())
                    .filter(Objects::nonNull).forEach(npcClass -> npcPanel.add(new NPClass(npcClass)));


            {
                int width = Math.max(Math.max(mainClassPanel.getPreferredSize().width, alternativeClassPanels.getPreferredSize().width), npcPanel.getPreferredSize().width);
                descriptionPanel.removeAll();
                final JLabel comp = new JLabel();
                comp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

                descriptionPanel.add(comp);
                comp.setText(GuiUtils.formatToolTip(width - 2, description()));
                descriptionPanel.setMaximumSize(new Dimension(width, descriptionPanel.getMaximumSize().height));
            }

            container.add(new TextHeaderBoxPanel("Main Classes", mainClassPanel));
            container.add(new TextHeaderBoxPanel("Description", descriptionPanel));
            container.add(new TextHeaderBoxPanel("Alternative Classes", alternativeClassPanels));
            //if (npcClasses.length>0)
            container.add(new TextHeaderBoxPanel("Natural Product Classes", npcPanel));
        }
        revalidate();
        repaint();
        if (getParent() instanceof JSplitPane)
            ((JSplitPane) getParent()).setDividerLocation(getPreferredSize().height);

    }

    private String description() {
        if (mainClass == null) return "This compound is not classified yet.";
        String m = mainClass.getDescription();
        return "This compound belongs to the class " + mainClass.getName() + ", which describes " + Character.toLowerCase(m.charAt(0)) + m.substring(1, m.length());
    }

    public void setPrediction(@NotNull CompoundClasses compoundClasses) {
        this.compoundClasses = compoundClasses;
        this.mainClass = compoundClasses.getClassyFireLineage().get(compoundClasses.getClassyFireLineage().size() - 1);
        updateContainer();
    }

    public void clear() {
        compoundClasses = null;
        mainClass = null;
        updateContainer();
    }


    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        try {
            backgroundLoaderLock.lock();
            final JJob<Boolean> old = backgroundLoader;
            backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                @Override
                protected Boolean compute() throws Exception {
                    if (old != null && !old.isFinished()) {
                        old.cancel(false);
                        old.getResult(); //await cancellation so that nothing strange can happen.
                    }
                    Jobs.runEDTAndWait(() -> clear());
                    checkForInterruption();
                    if (selectedElement != null) {
                        Optional<CompoundClasses> res = selectedElement.getCompoundClasses();
                        checkForInterruption();
                        if (res.isPresent())
                            Jobs.runEDTAndWait(() -> setPrediction(res.get()));
                    }
                    return true;
                }
            });
        } finally {
            backgroundLoaderLock.unlock();
        }
    }

    protected static class ClassifClass extends JPanel implements MouseListener {

        protected final CompoundClass cfClass;

        protected TextLayout typeName, className;
        protected Rectangle classBox, typeBox;

        protected Font typeFont, classFont;

        protected static final int PADDING = 4, SAFETY_DISTANCE = 4, GAP_TOP = 4;
        protected boolean main;

        public ClassifClass(CompoundClass cfClass, boolean main) {
            super();
            setToolTipText("<html><p>Probability: <b>" + (int) (Math.round(cfClass.getProbability() * 100)) + " %</b></p><p>" + cfClass.getDescription() + "</p>");
            this.main = main;
            this.cfClass = cfClass;
            typeFont = Fonts.FONT_BOLD.deriveFont(10f);
            typeName = new TextLayout(cfClass.getLevel(), typeFont, new FontRenderContext(null, false, false));
            classFont = Fonts.FONT_BOLD.deriveFont(13f);
            className = new TextLayout(cfClass.getName(), classFont, new FontRenderContext(null, false, false));
            setOpaque(false);
            classBox = className.getPixelBounds(null, 0, 0);
            typeBox = typeName.getPixelBounds(null, 0, 0);
            setPreferredSize(new Dimension(Math.max(classBox.width, typeBox.width) + 2 * PADDING + SAFETY_DISTANCE, classBox.height + typeBox.height + 2 * PADDING + SAFETY_DISTANCE + GAP_TOP));
            setMinimumSize(new Dimension(Math.max(classBox.width, typeBox.width) + 2 * PADDING + SAFETY_DISTANCE, classBox.height + typeBox.height + 2 * PADDING + SAFETY_DISTANCE + GAP_TOP));
            addMouseListener(this);

        }

        @Override
        public void paintComponent(Graphics g_) {
            final Graphics2D g = (Graphics2D) g_;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Color color = main ? Colors.CLASSIFIER_MAIN : Colors.CLASSIFIER_OTHER;
            g.setColor(color);
            final int boxwidth = Math.max(classBox.width, typeBox.width) + 2 * PADDING;
            final int boxheight = classBox.height + 2 * PADDING + GAP_TOP;
            g.fillRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(Colors.FOREGROUND);
            g.drawRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(color);
            final int gap = (boxwidth - typeBox.width) / 2;

            g.setFont(classFont);
            g.setColor(Colors.FOREGROUND);
            g.drawString(cfClass.getName(), PADDING, classBox.height + typeBox.height + PADDING + GAP_TOP);

            // draw header string
            g.setFont(typeFont);
            { // draw gaps for header string
                int headerTextWidth = g.getFontMetrics().stringWidth(cfClass.getLevel()) + 4;
                g.setStroke(new BasicStroke(1.5f));

                g.setColor(getBackground());
                g.drawLine(boxwidth / 2 - headerTextWidth / 2, typeBox.height, boxwidth / 2 + headerTextWidth / 2, typeBox.height);
                g.setColor(color);
                g.drawLine(boxwidth / 2 - headerTextWidth / 2, typeBox.height, boxwidth / 2 + headerTextWidth / 2, typeBox.height);
            }
            g.setColor(Colors.FOREGROUND);
            g.drawString(cfClass.getLevel(), gap, typeBox.height + GAP_TOP);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(URI.create(String.format("http://classyfire.wishartlab.com/tax_nodes/C%07d", cfClass.getId())));
            } catch (IOException ex) {
                LoggerFactory.getLogger(CompoundClassDetailView.class).error("Failed to open webbrowser");
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }


    protected static class NPClass extends JPanel {
        protected final CompoundClass npcClass;
        protected TextLayout typeName, className;
        protected Rectangle classBox, typeBox;

        protected Font typeFont, classFont;

        protected static final int PADDING = 4, SAFETY_DISTANCE = 4, GAP_TOP = 4;

        public NPClass(CompoundClass npcClass) {
            super();
            setToolTipText("<html><p>Probability: <b>" + (int) (Math.round(npcClass.getProbability() * 100)) + " %</b></p></html>");
            this.npcClass = npcClass;
            typeFont = Fonts.FONT_BOLD.deriveFont(10f);
            typeName = new TextLayout(npcClass.getLevel(), typeFont, new FontRenderContext(null, false, false));
            classFont = Fonts.FONT_BOLD.deriveFont(13f);
            className = new TextLayout(npcClass.getName(), classFont, new FontRenderContext(null, false, false));
            setOpaque(false);
            classBox = className.getPixelBounds(null, 0, 0);
            typeBox = typeName.getPixelBounds(null, 0, 0);
            setPreferredSize(new Dimension(Math.max(classBox.width, typeBox.width) + 2 * PADDING + SAFETY_DISTANCE, classBox.height + typeBox.height + 2 * PADDING + SAFETY_DISTANCE + GAP_TOP));
            setMinimumSize(new Dimension(Math.max(classBox.width, typeBox.width) + 2 * PADDING + SAFETY_DISTANCE, classBox.height + typeBox.height + 2 * PADDING + SAFETY_DISTANCE + GAP_TOP));

        }

        @Override
        public void paintComponent(Graphics g_) {
            final Graphics2D g = (Graphics2D) g_;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Color color = Colors.CLASSIFIER_OTHER;
            g.setColor(color);
            final int boxwidth = Math.max(classBox.width, typeBox.width) + 2 * PADDING;
            final int boxheight = classBox.height + 2 * PADDING + GAP_TOP;
            g.fillRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(Colors.FOREGROUND);
            g.drawRoundRect(0, typeBox.height, boxwidth, boxheight, 4, 4);
            g.setColor(color);
            final int gap = (boxwidth - typeBox.width) / 2;

            g.setFont(classFont);
            g.setColor(Colors.FOREGROUND);
            g.drawString(npcClass.getName(), PADDING, classBox.height + typeBox.height + PADDING + GAP_TOP);

            // draw header string
            g.setFont(typeFont);
            { // draw gaps for header string
                int headerTextWidth = g.getFontMetrics().stringWidth(npcClass.getLevel()) + 4;
                g.setStroke(new BasicStroke(1.5f));

                g.setColor(getBackground());
                g.drawLine(boxwidth / 2 - headerTextWidth / 2, typeBox.height, boxwidth / 2 + headerTextWidth / 2, typeBox.height);
                g.setColor(color);
                g.drawLine(boxwidth / 2 - headerTextWidth / 2, typeBox.height, boxwidth / 2 + headerTextWidth / 2, typeBox.height);
            }

            g.setColor(Colors.FOREGROUND);
            g.drawString(npcClass.getLevel(), gap, typeBox.height + GAP_TOP);

        }
    }
}
