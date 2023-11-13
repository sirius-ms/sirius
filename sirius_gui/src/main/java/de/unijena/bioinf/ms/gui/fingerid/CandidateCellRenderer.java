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

import ca.odell.glazedlists.EventList;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.compute.jjobs.BackgroundRunsGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.SpectralSearchResultBean;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer
 */
public class CandidateCellRenderer extends JPanel implements ListCellRenderer<FingerprintCandidateBean> {
    public static final int MIN_CELL_SIZE = 5;
    public final static int CELL_SIZE = 15;
    private static Color LOW = Color.RED, MED = Color.WHITE, HIGH = new Color(100, 149, 237);
    private final static Color EVEN = Colors.LIST_EVEN_BACKGROUND;
    private final static Color ODD = Colors.LIST_UNEVEN_BACKGROUND;


    protected final Font nameFont, propertyFont, rankFont, matchFont;

    private CompoundStructureImage image;
    private DescriptionPanel descriptionPanel;
    private FingerprintCandidateBean currentCandidate;

    private final DoubleListStats stats;

    private final CompoundList compoundList;
    protected final CandidateListDetailView candidateJList; //todo remove me to make conversion complete

    public CandidateCellRenderer(final CompoundList compoundList, DoubleListStats stats, CandidateListDetailView candidateJList) {
        this.compoundList = compoundList;
        this.candidateJList = candidateJList;
        this.stats = stats;
        setLayout(new BorderLayout());

        //init fonts
        final Font tempFont = Fonts.FONT_BOLD;
        if (tempFont != null) {
            nameFont = tempFont.deriveFont(13f);
            propertyFont = tempFont.deriveFont(16f);
            matchFont = tempFont.deriveFont(18f);
            rankFont = tempFont.deriveFont(32f);
        } else {
            nameFont = propertyFont = matchFont = rankFont = Font.getFont(Font.SANS_SERIF);

        }

        image = new CompoundStructureImage();
        descriptionPanel = new DescriptionPanel();
        add(image, BorderLayout.WEST);
        add(descriptionPanel, BorderLayout.CENTER);

    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FingerprintCandidateBean> list, FingerprintCandidateBean value, int index, boolean isSelected, boolean cellHasFocus) {

        image.molecule = value;
        if (value != null && value.getScore() >= stats.getMax()) {
            image.backgroundColor = Colors.LIST_LIGHT_GREEN;
        } else {
            image.backgroundColor = (index % 2 == 0 ? EVEN : ODD);
        }
        setOpaque(true);
        setBackground(image.backgroundColor);
        descriptionPanel.setCompound(value);
        currentCandidate = value;
        return this;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // memoize coordinates of substructures boxes
        final Rectangle ra = descriptionPanel.ag.getBounds();

        // add offset of parents
        ra.setLocation(ra.x + descriptionPanel.getX(), ra.y + descriptionPanel.getY());
        currentCandidate.substructures.setBounds(ra.x, ra.y, ra.width, ra.height);
    }

    public class FingerprintView extends JPanel {

        private FingerprintAgreement agreement;

        public FingerprintView(int height) {
            setOpaque(false);
            setPreferredSize(new Dimension(Integer.MAX_VALUE, height));
        }

        public void setAgreement(FingerprintAgreement agreement) {
            this.agreement = agreement;
            final int numberOfCols = Math.min(agreement.indizes.length, (getWidth() - 2) / CELL_SIZE);
            final int numberOfRows = numberOfCols == 0 ? 1 : ((agreement.indizes.length + numberOfCols - 1) / numberOfCols);
            agreement.setNumberOfCols(numberOfCols);
            final int W = numberOfCols * CELL_SIZE;
            final int H = numberOfRows * CELL_SIZE;
//            setPreferredSize(new Dimension(Integer.MAX_VALUE, H + 8));
//            revalidate();
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (agreement == null || agreement.indizes.length == 0) return;
            final Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

            final int numberOfCols = Math.min(agreement.indizes.length, (getWidth() - 2) / CELL_SIZE);
            final int numberOfRows = ((agreement.indizes.length + numberOfCols - 1) / numberOfCols);
            agreement.setNumberOfCols(numberOfCols);

            // highlight current INDEX
            final int useable_cell_size = CELL_SIZE - 1;
            for (int i = 0; i < agreement.indizes.length; ++i) {
                final float weight = (float) agreement.weights[i];

                final double colorWeight;
                final Color primary, secondary;
                if (weight >= 0.5) {
                    colorWeight = 2 * (weight - 0.5d);
                    primary = HIGH;
                    secondary = MED;
                } else {
                    colorWeight = 2 * (0.5d - weight);
                    primary = LOW;
                    secondary = MED;
                }

                final int row = i / numberOfCols;
                final int col = i % numberOfCols;

                final double weight2 = Math.max(0.25, agreement.weights2[i]);
                final int reduction = (int) Math.round((useable_cell_size - (((useable_cell_size - MIN_CELL_SIZE) / 0.75) * weight2)) / 2d) + 2;
                final int b;

                if (agreement.indizes[i] == candidateJList.highlightAgree) {
                    g.setColor(Color.BLUE);
                    b = 2;
                } else {
                    b = 1;
                    g.setColor(Colors.FOREGROUND);
                }

                g.fillRect((CELL_SIZE * col) + reduction - b, (CELL_SIZE * row) + reduction - b, (CELL_SIZE - reduction - reduction) + b + b, (CELL_SIZE - reduction - reduction) + b + b);

                g.setColor(gradient(primary, secondary, colorWeight));

                g.fillRect(reduction + CELL_SIZE * col, reduction + CELL_SIZE * row, CELL_SIZE - reduction - reduction, CELL_SIZE - reduction - reduction);
            }
        }

        private Color gradient(Color primary, Color secondary, double colorWeight) {
            final double w = 1d - colorWeight;
            final int r = (int) Math.round(primary.getRed() * colorWeight + secondary.getRed() * w);
            final int g = (int) Math.round(primary.getGreen() * colorWeight + secondary.getGreen() * w);
            final int b = (int) Math.round(primary.getBlue() * colorWeight + secondary.getBlue() * w);
            return new Color(r, g, b);
        }
    }

    private static final int DB_LABEL_PADDING = 4;

    public static class DatabasePanel extends JPanel {
        public static final Font DB_PANEL_FONT = Fonts.FONT_BOLD.deriveFont(11f);

        public DatabasePanel() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT,2,2));
            setPreferredSize(new Dimension(Integer.MAX_VALUE,
                    ((int) (new TextLayout("W", DB_PANEL_FONT, new FontRenderContext(null, false, false)).getBounds().getHeight()) +  2 * DB_LABEL_PADDING + 10) * 3));
        }

        public void setCompound(FingerprintCandidateBean candidate) {
            removeAll();
            if (candidate == null || candidate.candidate == null) return;

            for (DatabaseLabel label : candidate.labels) {
                add(new DatabaseLabelPanel(label));
            }
        }
    }

    private static class LabelPanel extends JPanel {

        private final Color color;

        private final DatabaseLabel label;

        public LabelPanel(DatabaseLabel label) {
            this(null, label, false);
        }

        public LabelPanel(Color color, DatabaseLabel label, boolean tight) {
            this.color = color;
            this.label = label;
            Font font = DatabasePanel.DB_PANEL_FONT;
            setFont(font);
            setOpaque(false);

            final TextLayout tlayout = new TextLayout(label.name(), font, new FontRenderContext(null, false, false));
            final Rectangle2D r = tlayout.getBounds();
            final int X = (int) r.getWidth() + 2 * DB_LABEL_PADDING + 6;
            final int Y = (int) r.getHeight() + 2 * DB_LABEL_PADDING + (tight ? 6 : 10);

            setPreferredSize(new Dimension(X, Y));
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            final FontMetrics m = getFontMetrics(getFont());
            final int tw = m.stringWidth(label.name());
            final int th = m.getHeight();
            final int w = tw + DB_LABEL_PADDING;
            final int h = th + DB_LABEL_PADDING;
            Rectangle gggp = getParent().getParent().getParent().getParent().getBounds();
            Rectangle ggp = getParent().getParent().getParent().getBounds();
            Rectangle gp = getParent().getParent().getBounds();
            Rectangle p = getParent().getBounds();
            Rectangle s = getBounds();
            final int rx = (int) (s.getX() + p.getX() + gp.getX() + ggp.getX() + gggp.getX());
            final int ry = (int) (s.getY() + p.getY() + gp.getY() + ggp.getY() + gggp.getY());

            label.rect.setBounds(rx, ry, w, h);
            if (color != null) {
                g.setColor(color);
                g.fillRoundRect(2, 2, w, h, 4, 4);
                g.setColor(Colors.FOREGROUND);
                g.drawRoundRect(2, 2, w, h, 4, 4);
                g.setColor(color.equals(Colors.DB_CUSTOM) ? Color.BLACK : Color.WHITE);
            } else {
                g.setColor(Colors.FOREGROUND);
            }
            g.drawString(label.name(), 2 + (w - tw) / 2, h - (h - th) / 2);
        }
    }

    private static class DatabaseLabelPanel extends LabelPanel {

        public DatabaseLabelPanel(DatabaseLabel label) {
            super(color(label), label, true);
        }

        private static Color color(DatabaseLabel label) {
            CustomDataSources.Source s = CustomDataSources.getSourceFromName(label.sourceName);
            if (s == null) return Colors.DB_UNKNOWN;
            if (s.isCustomSource()) return Colors.DB_CUSTOM;
            if (s.name().equals(DataSource.TRAIN.realName)) return Colors.DB_TRAINING;
            if (s.name().startsWith(DataSource.LIPID.realName)) return Colors.DB_ELGORDO;
            return label.values.length == 0 || s.URI() == null ? Colors.DB_UNLINKED : Colors.DB_LINKED;
        }

    }

    public class DescriptionPanel extends JPanel {

        protected JLabel inchi, agreements;
        private Box referenceBox;

        protected FingerprintView ag;
        protected JPanel agpanel;
        protected DatabasePanel databasePanel;

        protected JPanel namePanel;

        public DescriptionPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(5, 2, 2, 2));

            namePanel = new JPanel(new BorderLayout());
            inchi = new JLabel("", SwingConstants.LEFT);
            inchi.setFont(nameFont);

            namePanel.setOpaque(false);
            namePanel.add(inchi, BorderLayout.WEST);
            add(namePanel);

            Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);

            agpanel = new JPanel(new BorderLayout());
            agpanel.setOpaque(false);
            agreements = new JLabel("Substructures:", SwingConstants.LEFT);
            agreements.setFont(nameFont.deriveFont(map));
            agpanel.add(agreements, BorderLayout.WEST);
            add(agpanel);

            ag = new FingerprintView(100);
            add(ag);


            final JPanel b1 = new JPanel(new BorderLayout());

            final JLabel dbl = new JLabel("Sources");
            dbl.setFont(nameFont.deriveFont(map));
            b1.setOpaque(false);
            b1.add(dbl, BorderLayout.WEST);
            add(b1);

            final Box b2 = Box.createHorizontalBox();
            databasePanel = new DatabasePanel();
            b2.add(databasePanel);
            add(b2);

            setVisible(true);
        }

        public void setCompound(FingerprintCandidateBean value) {
            setFont(propertyFont);
            inchi.setText(value.candidate.getInchi().key2D());
            databasePanel.setCompound(value);
            ag.agreement = null;

            if (referenceBox != null) {
                namePanel.remove(referenceBox);
            }
            EventList<InstanceBean> selectedInstances = compoundList.getCompoundListSelectionModel().getSelected();
            if (!selectedInstances.isEmpty()) {
                selectedInstances.get(0).getSpectralSearchResults().ifPresent(search -> {
                    if (search.isFPCandidateInResults(value.getInChiKey())) {
                        if (value.referenceLabel.name().isBlank()) {
                            search.getBestMatchingSpectrumForFPCandidate(value.getInChiKey()).ifPresent(r -> {
                                SpectralSearchResultBean.MatchBean bean = new SpectralSearchResultBean.MatchBean(r);
                                value.referenceLabel.displayName = Math.round(100 * r.getSimilarity().similarity) + "% " + bean.getReference().getLibraryName();
                                value.referenceLabel.sourceName = bean.getReference().getLibraryName();
                                value.referenceLabel.values = new String[]{bean.getReference().getLibraryId()};
                            });
                        }

                        referenceBox = Box.createVerticalBox();
                        JLabel refLabel = new JLabel("Reference Spectra");
                        refLabel.setFont(agreements.getFont());
                        referenceBox.add(refLabel);
                        referenceBox.add(Box.createVerticalStrut(2));
                        referenceBox.add(new LabelPanel(Colors.DB_CUSTOM, value.referenceLabel, false));

                        search.getMatchingSpectraForFPCandidate(value.getInChiKey()).ifPresent(l -> {
                            if (l.size() > 1) {
                                value.moreLabel.displayName = String.format("and %d more...", l.size() - 1);
                                referenceBox.add(new LabelPanel(value.moreLabel));
                            }
                        });
                        namePanel.add(referenceBox, BorderLayout.EAST);
                    }
                });
            }

            if (value.fp == null)
                return;

            //todo is this down in background. i am not competley sure which methods run im background and which in EDT here.
            BackgroundRunsGui.getProject().getProjectSpaceProperty(FingerIdDataProperty.class).map(p -> p.getByIonType(value.adduct)).ifPresent(f ->
                    ag.setAgreement(value.getSubstructures(value.getPlatts(), f.getPerformances())));
        }
    }
}
