package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fleisch on 16.05.17.
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

    protected final CandidateListDetailView candidateJList; //todo remove me to make conversion complete

    public CandidateCellRenderer(DoubleListStats stats, CandidateListDetailView candidateJList) {
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
                    g.setColor(Color.BLACK);
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
        private final Font dbPanelFont = Fonts.FONT_BOLD.deriveFont(12f);

        public DatabasePanel() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(5, 2, 2, 2));
        }

        public void setCompound(FingerprintCandidateBean candidate) {
            removeAll();
            if (candidate == null || candidate.candidate == null) return;

            for (DatabaseLabel label : candidate.labels) {
                final TextLayout tlayout = new TextLayout(label.name, dbPanelFont, new FontRenderContext(null, false, false));
                final Rectangle2D r = tlayout.getBounds();
                final int X = (int) r.getWidth() + 2 * DB_LABEL_PADDING + 6;
                final int Y = (int) r.getHeight() + 2 * DB_LABEL_PADDING + 6;

                add(new DatabaseLabelPanel(label, X, Y, dbPanelFont));
            }
        }
    }

    private static class DatabaseLabelPanel extends JPanel {
        private final Color color;
        private final DatabaseLabel label;

        public DatabaseLabelPanel(DatabaseLabel label, int width, int height, Font font) {
            this.label = label;
            this.color = color();
            setFont(font);
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));
        }

        private Color color() {
            CustomDataSources.Source s = CustomDataSources.getSourceFromName(label.name);
            if (s == null) return Colors.DB_UNKNOWN;
            if (s.isCustomSource()) return Colors.DB_CUSTOM;
            if (s.name().equals(DataSource.TRAIN.realName)) return Colors.DB_TRAINING;
            return label.values.length == 0 ? Colors.DB_UNLINKED : Colors.DB_LINKED;
        }


        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            final FontMetrics m = getFontMetrics(getFont());
            final int tw = m.stringWidth(label.name);
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
            g.setColor(color);
            g.fillRoundRect(2, 2, w, h, 4, 4);
            g.setColor(Color.BLACK);
            g.drawRoundRect(2, 2, w, h, 4, 4);
            g.setColor(color.equals(Colors.DB_CUSTOM) ? Color.BLACK : Color.WHITE);
            g.drawString(label.name, 2 + (w - tw) / 2, h - (h - th) / 2);
        }
    }

    public class XLogPLabel extends JPanel {

        private double logP;
        private final DecimalFormat format = new DecimalFormat("#0.000");
        private Font font;

        public XLogPLabel() {
            this.logP = Double.NaN;
            setPreferredSize(new Dimension(128, 20));
            Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            font = nameFont.deriveFont(map);
        }

        @Override
        public void paintComponent(Graphics g) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (Double.isNaN(logP)) return;
            g.setFont(font);
            int widthB = g.getFontMetrics().stringWidth("XLogP: ");
            g.drawString("XLogP:", 0, 14);
            g.setFont(nameFont);
            g.drawString(format.format(logP), widthB, 14);
        }

        public void setLogP(double logP) {
            this.logP = logP;
            repaint();
        }
    }

    public class ScoreLabel extends JPanel {

        private double score;
        private final DecimalFormat format = new DecimalFormat("#0.000");
        private Font scoreSuperscriptFont;

        public ScoreLabel() {
            this.score = Double.NaN;
            setPreferredSize(new Dimension(128, 20));
            final HashMap<AttributedCharacterIterator.Attribute, Object> attrs = new HashMap<>();
            attrs.put(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
            attrs.put(TextAttribute.SIZE, 15f);
            scoreSuperscriptFont = nameFont.deriveFont(attrs);
        }

        @Override
        public void paintComponent(Graphics g) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (Double.isNaN(score)) return;
            g.setFont(nameFont);
            final String t1 = "Score: ";
            int widthB = g.getFontMetrics().stringWidth(t1);
            g.drawString(t1, 0, 14);
            //g.setFont(scoreSuperscriptFont);
            g.drawString(format.format(score), widthB, 14);
        }

        public void setScore(double score) {
            this.score = score;
            repaint();
        }
    }

    public class DescriptionPanel extends JPanel {

        protected JLabel inchi, agreements;
        protected XLogPLabel xlogP;
        protected ScoreLabel scoreL;
        protected FingerprintView ag;
        protected JPanel agpanel;
        protected DatabasePanel databasePanel;

        public DescriptionPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(5, 2, 2, 2));

            final JPanel namePanel = new JPanel(new BorderLayout());
            inchi = new JLabel("", SwingConstants.LEFT);
            inchi.setFont(nameFont);
            xlogP = new XLogPLabel();
            namePanel.setOpaque(false);
            namePanel.add(inchi, BorderLayout.WEST);
            namePanel.add(xlogP, BorderLayout.EAST);
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

            ag = new FingerprintView(120);
            add(ag);


            final JPanel b1 = new JPanel(new BorderLayout());

            final JLabel dbl = new JLabel("Databases");
            dbl.setFont(nameFont.deriveFont(map));
            scoreL = new ScoreLabel();
            b1.setOpaque(false);
            b1.add(dbl, BorderLayout.WEST);
            b1.add(scoreL, BorderLayout.EAST);
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
            xlogP.setLogP(value.candidate.getXlogp());
            scoreL.setScore(value.getScore());
            ag.agreement = null;

            if (value.fp == null)
                return;

            if (value.adduct.isNegative()) {
                LoggerFactory.getLogger(getClass()).error("Negative data is currently not supported");
                return;
            }

            MainFrame.MF.ps().getProjectSpaceProperty(FingerIdData.class).ifPresent(f ->
                    ag.setAgreement(value.getSubstructures(value.getPlatts(), f.getPerformances())));

        }
    }
}
