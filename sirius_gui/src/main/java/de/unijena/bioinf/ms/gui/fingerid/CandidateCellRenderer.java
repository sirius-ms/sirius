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
import java.util.function.Function;

/**
 * @author Markus Fleischauer
 */
public class CandidateCellRenderer extends JPanel implements ListCellRenderer<FingerprintCandidateBean> {
    public static final int MIN_CELL_SIZE = 5;
    public final static int CELL_SIZE = 15;
    private static Color LOW = Colors.StructuresView.SUBSTRUCTURE_BOX_GRADIENT_LOW_PROBABILITY, MED = Colors.StructuresView.SUBSTRUCTURE_BOX_GRADIENT_MEDIUM_PROBABILITY, HIGH = Colors.StructuresView.SUBSTRUCTURE_BOX_GRADIENT_HIGH_PROBABILITY;
    private final static Color EVEN = Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_1;
    private final static Color ODD = Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_2;


    protected static final int maxRankFontSize = 30;
    protected static final int minRankFontSize = 10;
    protected final static Font nameFont, propertyFont, rankFont, headlineFont;

    static {
        //init fonts
        final Font tempFont = Fonts.FONT_MEDIUM;
        if (tempFont != null) {
            nameFont = tempFont.deriveFont(16f);
            propertyFont = tempFont.deriveFont(16f);
            rankFont = tempFont.deriveFont(maxRankFontSize);
        } else {
            nameFont = propertyFont = rankFont = Font.getFont(Font.SANS_SERIF);
        }

        headlineFont = nameFont.deriveFont(Map.of(
                TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON,
                TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD
        ));
    }

    private final Function<FingerprintCandidateBean, Boolean> isBest;

    private CompoundStructureImage image;
    private DescriptionPanel descriptionPanel;
    private JPanel allStructureInfoWithoutRank;


    private FingerprintCandidateBean currentCandidate;

    private final DoubleListStats stats;

    private final JLabel nameLabel, rankLabel;

    protected final CandidateListDetailView candidateJList; //todo remove me to make conversion complete
    private final SiriusGui gui;

    public CandidateCellRenderer(DoubleListStats stats, CandidateListDetailView candidateJList, SiriusGui gui, Function<FingerprintCandidateBean, Boolean> isBest) {
        this.candidateJList = candidateJList;
        this.stats = stats;
        this.gui = gui;
        this.isBest = isBest;
        setLayout(new BorderLayout());

        rankLabel = new JLabel("");
        rankLabel.setFont(rankFont);
        rankLabel.setPreferredSize(new Dimension(62, 100)); //border plus width of 2-digit number in font 30
        rankLabel.setSize(new Dimension(62, 100)); //to make sure also first rendering is correct.
        rankLabel.setVerticalAlignment(SwingConstants.CENTER);
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rankLabel.setBorder(new EmptyBorder(1, 10, 1, 10));

        rankLabel.setOpaque(true);

        allStructureInfoWithoutRank = new JPanel(new BorderLayout());
        {

            JPanel north = new JPanel(new BorderLayout());
            north.setOpaque(false);
            north.setBorder(new EmptyBorder(1, 1, 1, 1));

            nameLabel = new JLabel("");
            nameLabel.setFont(nameFont);
            nameLabel.setForeground(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
            nameLabel.setBorder(new EmptyBorder(0,3,0,0));

            north.add(nameLabel, BorderLayout.WEST);

            image = new CompoundStructureImage(gui);
            descriptionPanel = new DescriptionPanel();
            allStructureInfoWithoutRank.add(north, BorderLayout.NORTH);
            allStructureInfoWithoutRank.add(image, BorderLayout.WEST);
            allStructureInfoWithoutRank.add(descriptionPanel, BorderLayout.CENTER);
        }

        add(rankLabel, BorderLayout.WEST);
        add(allStructureInfoWithoutRank, BorderLayout.CENTER);

    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FingerprintCandidateBean> list, FingerprintCandidateBean value, int index, boolean isSelected, boolean cellHasFocus) {
        if ((value.getName() != null) && (!"null".equalsIgnoreCase(value.getName()))) {
            nameLabel.setText(value.getName());
        } else {
            nameLabel.setText("-");
        }

        if (value.getCandidate().getRank() != null) {
            rankLabel.setText(value.getCandidate().getRank().toString());
        } else {
            rankLabel.setText("");
        }

        // Calculate the optimal font size for this label
        adjustFontSizeToFitText(rankLabel, maxRankFontSize, minRankFontSize);

        image.molecule = value;

        image.backgroundColor = (index % 2 == 0 ? EVEN : ODD);
        allStructureInfoWithoutRank.setBackground((index % 2 == 0 ? EVEN : ODD));
        if (gui.getProperties().isConfidenceViewMode(ConfidenceDisplayMode.EXACT) || value.getCandidate().getMcesDistToTopHit() == null) {
            rankLabel.setForeground(value.getScore() >= stats.getMax() ? Colors.CellsAndRows.BEST_HIT_TEXT : Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
            rankLabel.setBackground(value.getScore() >= stats.getMax() ? (index % 2 == 0 ? Colors.CellsAndRows.BEST_HIT_ALTERNATING_CELL_1 : Colors.CellsAndRows.BEST_HIT_ALTERNATING_CELL_2) : (index % 2 == 0 ? EVEN : ODD));
        } else {
            if (value.getCandidate().getMcesDistToTopHit() != null) {
                rankLabel.setForeground(isBest.apply(value) ? Colors.CellsAndRows.BEST_HIT_TEXT : Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
                rankLabel.setBackground(isBest.apply(value) ? (index % 2 == 0 ? Colors.CellsAndRows.BEST_HIT_ALTERNATING_CELL_1 : Colors.CellsAndRows.BEST_HIT_ALTERNATING_CELL_2) : (index % 2 == 0 ? EVEN : ODD));
            }
        }

        setOpaque(true);
        setBackground(image.backgroundColor);
        descriptionPanel.setCompound(value);
        currentCandidate = value;
        return this;
    }

    private void adjustFontSizeToFitText(JLabel label, int maxFontSize, int minFontSize) {
        String text = label.getText();
        Font currentFont = label.getFont();
        int fontSize = maxFontSize;

        Insets borderInsets = label.getInsets(); // Get the border insets (padding around the text)
        int availableWidth =  label.getWidth() - (borderInsets.left + borderInsets.right); // width subtracting borders


        FontMetrics fm = label.getFontMetrics(new Font(currentFont.getName(), Font.PLAIN, fontSize));
        int stringWidth = fm.stringWidth(text);

        // Adjust the font size until the text fits within the available width
        while (stringWidth > availableWidth && fontSize > minFontSize) {
            fontSize--;
            fm = label.getFontMetrics(new Font(currentFont.getName(), Font.PLAIN, fontSize));
            stringWidth = fm.stringWidth(text);
        }

        label.setFont(new Font(currentFont.getName(), Font.PLAIN, fontSize));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // memorize coordinates of substructures boxes
        final Rectangle ra = descriptionPanel.ag.getBounds();
        // add offset of parents
        ra.setLocation(
                ra.x + descriptionPanel.getX() + descriptionPanel.agpanel.getX() + rankLabel.getWidth(), //todo coloring: someone please check if this is the correct way to fix the offset of fingerprint property squares after adding rank label to front
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
                    g.setColor(Colors.StructuresView.SELECTED_SUBSTUCTURE_HIGHLIGHTING_PRIMARY);
                    b = 4;
                } else {
                    b = 1;
                    g.setColor(Colors.FOREGROUND_INTERFACE);
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
        public static final Font DB_PANEL_FONT = Fonts.FONT_MEDIUM.deriveFont(11f);

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
        private final Color textColor;
        private final Color borderColor;

        private final DatabaseLabel label;

        public LabelPanel(DatabaseLabel label) {
            this(null, null, null, label, false);
        }

        public LabelPanel(Color color, Color textColor, Color borderColor, DatabaseLabel label, boolean tight) {
            this.color = color;
            this.textColor = (textColor != null ? textColor : Colors.FOREGROUND_DATA);
            this.borderColor = (borderColor != null ? borderColor : Colors.FOREGROUND_INTERFACE);
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
                g.setColor(borderColor);
                g.drawRoundRect(2, 2, w, h, 4, 4);
            }
            g.setColor(textColor);
            g.drawString(label.name(), 2 + (w - tw) / 2, h - (h - th) / 2);
        }
    }

    private static class DatabaseLabelPanel extends LabelPanel {

        public DatabaseLabelPanel(DatabaseLabel label) {
            this(label, true);
        }

        public DatabaseLabelPanel(DatabaseLabel label, boolean tight) {
            super(color(label), textColor(label), borderColor(label), label, tight);
        }

        private static Color color(DatabaseLabel label) {
            if ("De Novo".equals(label.sourceName))
                return Colors.StructuresView.Sources.DE_NOVO;
            CustomDataSources.Source s = CustomDataSources.getSourceFromName(label.sourceName);
            if (s == null) return Colors.StructuresView.Sources.CUSTOM_DB_NOT_LOADED;
            if (s.isCustomSource()) return Colors.StructuresView.Sources.CUSTOM_DB;
            if (s.name().equals(DataSource.TRAIN.name())) return Colors.StructuresView.Sources.TRAINING_DATA;
            if (s.name().startsWith(DataSource.LIPID.name())) return Colors.StructuresView.Sources.SPECIAL;
            return label.values.length == 0 || s.URI() == null ? Colors.StructuresView.Sources.MAIN_DB_NO_LINK : Colors.StructuresView.Sources.MAIN_DB_WITH_LINK;
        }

        private static Color textColor(DatabaseLabel label) {
            if ("De Novo".equals(label.sourceName))
                return Colors.StructuresView.Sources.DE_NOVO_TEXT;
            CustomDataSources.Source s = CustomDataSources.getSourceFromName(label.sourceName);
            if (s == null) return Colors.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_TEXT;
            if (s.isCustomSource()) return Colors.StructuresView.Sources.DEFAULT_TEXT;
            if (s.name().equals(DataSource.TRAIN.name())) return Colors.StructuresView.Sources.TRAINING_DATA_TEXT;
            if (s.name().startsWith(DataSource.LIPID.name())) return Colors.StructuresView.Sources.TRAINING_DATA_TEXT;
            return label.values.length == 0 || s.URI() == null ? Colors.StructuresView.Sources.MAIN_DB_NO_LINK_TEXT : Colors.StructuresView.Sources.DEFAULT_TEXT;
        }

        private static Color borderColor(DatabaseLabel label) {
            if ("De Novo".equals(label.sourceName))
                return Colors.StructuresView.Sources.DE_NOVO_BORDER;
            CustomDataSources.Source s = CustomDataSources.getSourceFromName(label.sourceName);
            if (s == null) return Colors.StructuresView.Sources.CUSTOM_DB_NOT_LOADED_BORDER;
            if (s.isCustomSource()) return Colors.StructuresView.Sources.DEFAULT_BORDER;;
            if (s.name().equals(DataSource.TRAIN.name())) return Colors.StructuresView.Sources.DEFAULT_BORDER;
            if (s.name().startsWith(DataSource.LIPID.name())) return Colors.StructuresView.Sources.DEFAULT_BORDER;
            return label.values.length == 0 || s.URI() == null ? Colors.StructuresView.Sources.MAIN_DB_NO_LINK_BORDER : Colors.StructuresView.Sources.DEFAULT_BORDER;
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
                agreements.setForeground(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
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
                dbl.setForeground(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
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
                refLabel.setForeground(Colors.CellsAndRows.ALTERNATING_CELL_ROW_TEXT_COLOR);
                refLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                refLabel.setFont(headlineFont);
                innerBox.add(refLabel);
                innerBox.add(Box.createVerticalStrut(2));
                LabelPanel lp = new DatabaseLabelPanel(value.bestRefMatchLabel, false);
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
