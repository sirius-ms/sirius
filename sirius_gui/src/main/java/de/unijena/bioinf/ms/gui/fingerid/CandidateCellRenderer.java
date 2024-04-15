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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
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


    protected final static Font nameFont, propertyFont, rankFont, matchFont, headlineFont;

    static {
        //init fonts
        final Font tempFont = Fonts.FONT_BOLD;
        if (tempFont != null) {
            nameFont = tempFont.deriveFont(13f);
            propertyFont = tempFont.deriveFont(16f);
            matchFont = tempFont.deriveFont(18f);
            rankFont = tempFont.deriveFont(20f);
        } else {
            nameFont = propertyFont = matchFont = rankFont = Font.getFont(Font.SANS_SERIF);
        }

        headlineFont = nameFont.deriveFont(Map.of(
                TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON,
                TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD
        ));
    }

    private CompoundStructureImage image;
    private DescriptionPanel descriptionPanel;

    private double structDistanceThreshold;

    private FingerprintCandidateBean currentCandidate;

    private final DoubleListStats stats;

    private final JLabel nameLabel, rankLabel;

    protected final CandidateListDetailView candidateJList; //todo remove me to make conversion complete
    private final SiriusGui gui;

    public CandidateCellRenderer(DoubleListStats stats, CandidateListDetailView candidateJList, SiriusGui gui, double structDistanceThreshold) {
        this.candidateJList = candidateJList;
        this.stats = stats;
        this.gui = gui;
        this.structDistanceThreshold = structDistanceThreshold;
        setLayout(new BorderLayout());

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.setBorder(new EmptyBorder(1, 1, 1, 1));

        nameLabel = new JLabel("");
        nameLabel.setFont(nameFont);
        rankLabel = new JLabel("");
        rankLabel.setFont(rankFont);

        north.add(nameLabel, BorderLayout.WEST);
        north.add(rankLabel, BorderLayout.EAST);

        image = new CompoundStructureImage();
        descriptionPanel = new DescriptionPanel();
        add(north, BorderLayout.NORTH);
        add(image, BorderLayout.WEST);
        add(descriptionPanel, BorderLayout.CENTER);

    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FingerprintCandidateBean> list, FingerprintCandidateBean value, int index, boolean isSelected, boolean cellHasFocus) {
        if ((value.getName() != null) && (!"null".equalsIgnoreCase(value.getName()))) {
            nameLabel.setText(value.getName());
        } else {
            nameLabel.setText("");
        }

        if (value.getCandidate().getRank() != null) {
            rankLabel.setText(value.getCandidate().getRank().toString());
        } else {
            rankLabel.setText("");
        }

        image.molecule = value;
        if (gui.getProperties().isConfidenceViewMode(ConfidenceDisplayMode.EXACT) || value.getCandidate().getMcesDistToTopHit() == null) {
            image.backgroundColor = value.getScore() >= stats.getMax() ? Colors.LIST_LIGHT_GREEN : (index % 2 == 0 ? EVEN : ODD);
        } else {
            if (value.getCandidate().getMcesDistToTopHit() != null)
                image.backgroundColor = value.getCandidate().getMcesDistToTopHit() <= this.structDistanceThreshold ? Colors.LIST_LIGHT_GREEN : (index % 2 == 0 ? EVEN : ODD);
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
        // memorize coordinates of substructures boxes
        final Rectangle ra = descriptionPanel.ag.getBounds();
        // add offset of parents
        ra.setLocation(
                ra.x + descriptionPanel.getX() + descriptionPanel.agpanel.getX(),
                ra.y + descriptionPanel.getY() + descriptionPanel.agpanel.getY());
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
            this.agreement.setNumberOfCols(Math.min(agreement.indizes.length, (getWidth() - 2) / CELL_SIZE));
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (agreement == null || agreement.indizes.length == 0) return;
            final Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int numberOfCols = Math.min(agreement.indizes.length, (getWidth() - 2) / CELL_SIZE);
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
            setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
            setPreferredSize(new Dimension(Integer.MAX_VALUE,
                    ((int) (new TextLayout("W", DB_PANEL_FONT, new FontRenderContext(null, true, false)).getBounds().getHeight()) + 2 * DB_LABEL_PADDING + 10) * 3));
        }

        public void setCompound(FingerprintCandidateBean candidate) {
            removeAll();
            if (candidate == null || candidate.getCandidate() == null) return;

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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
            if (s.name().equals(DataSource.TRAIN.name())) return Colors.DB_TRAINING;
            if (s.name().startsWith(DataSource.LIPID.name())) return Colors.DB_ELGORDO;
            return label.values.length == 0 || s.URI() == null ? Colors.DB_UNLINKED : Colors.DB_LINKED;
        }

    }

    public class DescriptionPanel extends JPanel {

        protected JLabel agreements;
        protected FingerprintView ag;
        protected JPanel agpanel;
        protected DatabasePanel databasePanel;

        private ReferenceMatchPanel referenceMatchPanel;

        public DescriptionPanel() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(5, 2, 2, 2));

            //CENTER
            {
                agpanel = new JPanel(new BorderLayout());
                agpanel.setOpaque(false);
                agreements = new JLabel("Substructures:", SwingConstants.LEFT);
                agreements.setFont(headlineFont);
                agpanel.add(agreements, BorderLayout.NORTH);

                ag = new FingerprintView(100);
                agpanel.add(ag, BorderLayout.CENTER);

                add(agpanel, BorderLayout.CENTER);
            }

            //EAST
            {
                referenceMatchPanel = new ReferenceMatchPanel();
                add(referenceMatchPanel, BorderLayout.EAST);
            }

            //SOUTH
            {
                final JPanel dbLabelPanel = new JPanel(new BorderLayout());
                final JLabel dbl = new JLabel("Sources", SwingConstants.LEFT);
                dbl.setFont(headlineFont);
                dbLabelPanel.setOpaque(false);
                dbLabelPanel.add(dbl, BorderLayout.NORTH);
                databasePanel = new DatabasePanel();
                dbLabelPanel.add(databasePanel, BorderLayout.CENTER);
                //add to main description panel
                add(dbLabelPanel, BorderLayout.SOUTH);
            }

            setVisible(true);
        }

        public void setCompound(FingerprintCandidateBean value) {
            setFont(propertyFont);
            databasePanel.setCompound(value);
            referenceMatchPanel.setCompound(value);
            ag.agreement = null;

            if (value.getPredictedFingerprint() == null)
                return;

            int charge = PrecursorIonType.fromString(value.getCandidate().getAdduct()).getCharge();
            // runs in awt event queue but seems to be fast enough
            ag.setAgreement(value.getSubstructures(value.getPredictedFingerprint(), gui.getProjectManager().getFingerIdData(charge)
                    .getPerformances()));
        }
    }

    public class ReferenceMatchPanel extends JPanel {
        private Box innerBox = null;

        public ReferenceMatchPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        public void setCompound(FingerprintCandidateBean value) {
            if (innerBox != null)
                remove(innerBox);
            if (value != null && value.bestRefMatchLabel != null) {
                innerBox = Box.createVerticalBox();
                innerBox.setAlignmentY(Component.TOP_ALIGNMENT);
                innerBox.setAlignmentX(Component.CENTER_ALIGNMENT);
                innerBox.setOpaque(false);

                JLabel refLabel = new JLabel("Reference Spectra");
                refLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                refLabel.setFont(headlineFont);
                innerBox.add(refLabel);
                innerBox.add(Box.createVerticalStrut(2));
                LabelPanel lp = new LabelPanel(Colors.DB_CUSTOM, value.bestRefMatchLabel, false);
                lp.setAlignmentX(Component.CENTER_ALIGNMENT);
                innerBox.add(lp);

                // check if we have more matches
                if (value.moreRefMatchesLabel != null)
                    innerBox.add(new LabelPanel(value.moreRefMatchesLabel));
                innerBox.add(Box.createVerticalGlue());

                add(innerBox, BorderLayout.NORTH);
            }
        }
    }
}
