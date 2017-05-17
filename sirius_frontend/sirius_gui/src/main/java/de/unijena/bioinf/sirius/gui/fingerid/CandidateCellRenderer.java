package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.configs.Colors;
import de.unijena.bioinf.sirius.gui.configs.Fonts;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
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
import java.util.*;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateCellRenderer extends JPanel implements ListCellRenderer<CompoundCandidate> {
    public static final int MIN_CELL_SIZE = 5;
    public final static int CELL_SIZE = 15;
    private static Color LOW = Color.RED, MED = Color.WHITE, HIGH = new Color(100, 149, 237);
    private final static Color EVEN = Colors.LIST_EVEN_BACKGROUND;
    private final static Color ODD = Colors.LIST_UNEVEN_BACKGROUND;


    protected final Font nameFont, propertyFont, rankFont,  matchFont;

//    private CandidateJList candidateJList;
    private CompoundImage image;
    private DescriptionPanel descriptionPanel;
    private CompoundCandidate currentCandidate;

    private final CSIFingerIdComputation computation;
    private final DoubleListStats stats;

    protected final CandidateJList candidateJList; //todo remove me to make conversion complete
    public CandidateCellRenderer(CSIFingerIdComputation computation, DoubleListStats stats, CandidateJList candidateJList) {
        this.candidateJList = candidateJList;
        this.computation = computation;
        this.stats = stats;
        setLayout(new BorderLayout());

        //init fonts
        final Font tempFont = Fonts.FONT_BOLD;
        if (tempFont != null){
            nameFont = tempFont.deriveFont(13f);
            propertyFont = tempFont.deriveFont(16f);
            matchFont = tempFont.deriveFont(18f);
            rankFont = tempFont.deriveFont(32f);
        }else {
            nameFont = propertyFont = matchFont = rankFont = Font.getFont(Font.SANS_SERIF);

        }

        image = new CompoundImage();
        descriptionPanel = new DescriptionPanel();
        add(image, BorderLayout.WEST);
        add(descriptionPanel, BorderLayout.CENTER);

    }

    @Override
    public Component getListCellRendererComponent(JList<? extends CompoundCandidate> list, CompoundCandidate value, int index, boolean isSelected, boolean cellHasFocus) {

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
        ra.setLocation(ra.x + descriptionPanel.agpanel.getX(), ra.y + descriptionPanel.agpanel.getY());

        currentCandidate.substructures.setBounds(ra.x, ra.y, ra.width, ra.height);
    }


















    private class CompoundImage extends JPanel {

        protected CompoundCandidate molecule;
        protected AtomContainerRenderer renderer;
        protected Color backgroundColor;

        public CompoundImage() {
            setOpaque(false);
            setPreferredSize(new Dimension(374, 215));
            // make generators
            java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
            generators.add(new BasicSceneGenerator());
            generators.add(new StandardGenerator(nameFont));
            /*
            generators.add(new BasicBondGenerator());
            generators.add(new RingGenerator());
            generators.add(new BasicAtomGenerator());
            generators.add(new HighlightGenerator());
            */
            // setup the renderer
            this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());

            renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                    StandardGenerator.HighlightStyle.OuterGlow);
            renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class,
                    new CDK2DAtomColors());
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (molecule.compound.molecule != null) {
                final Graphics2D gg = (Graphics2D) g;
                StructureDiagramGenerator sdg = new StructureDiagramGenerator();
                sdg.setMolecule(molecule.compound.getMolecule(), false);
                try {
                    sdg.generateCoordinates();
                } catch (CDKException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                }
                renderer.getRenderer2DModel().set(BasicSceneGenerator.BackgroundColor.class, backgroundColor);
                synchronized (molecule.compound) {
                    renderer.paint(molecule.compound.getMolecule(), new AWTDrawVisitor(gg),
                            new Rectangle2D.Double(7, 14, 360, 185), true);
                }
                if (molecule.compound.name != null) {
                    gg.setFont(nameFont);
                    gg.drawString(molecule.compound.name, 3, 16);
                }
                gg.setFont(rankFont);
                final String rankString = String.valueOf(molecule.rank);
                final Rectangle2D bound = gg.getFontMetrics().getStringBounds(rankString, gg);
                {
                    final int x = 3;
                    final int y = getHeight() - (int) (bound.getHeight());
                    final int h = (int) (y + bound.getHeight());
                    gg.drawString(rankString, x, h - 2);
                }
//                gg.setFont(nameFont);
//                final String scoreText1 = "(score: e";
//                final String scoreText2 = String.format(Locale.US, "%d", (long) Math.round(molecule.score));
//                final String scoreText3 = ")";
//
//                double w = gg.getFontMetrics(nameFont).getStringBounds(scoreText1, gg).getWidth();
//                double w2 = gg.getFontMetrics(scoreSuperscriptFont).getStringBounds(scoreText2, gg).getWidth();
//                double w3 = gg.getFontMetrics(nameFont).getStringBounds(scoreText3, gg).getWidth();
//                double h2 = gg.getFontMetrics(scoreSuperscriptFont).getStringBounds(scoreText2, gg).getHeight();

                final String tanimotoText = String.format(Locale.US, "%.2f", molecule.getTanimotoScore() * 100d) + "%";
                double tw = gg.getFontMetrics(matchFont).getStringBounds(tanimotoText, gg).getWidth();
//                double th = gg.getFontMetrics(matchFont).getStringBounds(tanimotoText, gg).getHeight();

                /*{
                    Color from = new Color(backgroundColor.getRed(),backgroundColor.getGreen(),backgroundColor.getBlue(),0);
                    Color to = new Color(backgroundColor.getRed(),backgroundColor.getGreen(),backgroundColor.getBlue(),255);

                    int xx = (int)(getWidth()-(w + w2)), yy = (int)(getHeight()-30);
                    int mid = xx + (getWidth()-xx)/2;
                    GradientPaint paint = new GradientPaint(mid, yy, from,
                            mid, yy+15, to, false);
                    Paint oldPaint = gg.getPaint();
                    gg.setPaint(paint);
                    gg.fillRect(xx, yy, getWidth()-xx, getHeight()-yy);
                    gg.setPaint(oldPaint);
                }*/

//                gg.setFont(nameFont);
//                gg.drawString(scoreText1, (int) (getWidth() - (tw + w + w2 + w3 + 8)), getHeight() - 4);
//                gg.setFont(scoreSuperscriptFont);
//                gg.drawString(scoreText2, (int) (getWidth() - (tw + w2 + w3 + 8)), (int) (getHeight() - 4));
//                gg.setFont(nameFont);
//                gg.drawString(scoreText3, (int) (getWidth() - (tw + w3 + 8)), (int) (getHeight() - 4));

                gg.setFont(matchFont);
                gg.drawString(tanimotoText, (int) (getWidth() - (tw + 4)), (int) (getHeight() - 4));


            }
        }
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
            //setPreferredSize(new Dimension(Integer.MAX_VALUE, H + 8));
            //revalidate();
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            if (agreement == null || agreement.indizes.length == 0) return;
            final Graphics2D g = (Graphics2D) graphics;
            final int numberOfCols = Math.min(agreement.indizes.length, (getWidth() - 2) / CELL_SIZE);
            final int numberOfRows = ((agreement.indizes.length + numberOfCols - 1) / numberOfCols);
            agreement.setNumberOfCols(numberOfCols);
            final int W = numberOfCols * CELL_SIZE;
            final int H = numberOfRows * CELL_SIZE;
            final int sizeOfLastRow = agreement.indizes.length % numberOfCols;
            /*
            g.setColor(Color.BLACK);
            if (sizeOfLastRow==0) {
                g.fillRect(0, 0, W, H);
            } else {
                g.fillRect(0, 0, W, H- CELL_SIZE);
                g.fillRect(0, CELL_SIZE *(numberOfRows-1), CELL_SIZE *sizeOfLastRow, CELL_SIZE);
            }
            */
            final float[] components = new float[3];
            //Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), components);

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

//                g.setColor(Color.getHSBColor(components[0], components[1], weight));
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

    public class DatabasePanel extends JPanel {
        private CompoundCandidate candidate;
        private Font ownFont;
        private Color bgColor = new Color(155, 166, 219);

        public DatabasePanel() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(5, 2, 2, 2));
            ownFont = getFont().deriveFont(Font.BOLD, 12);
        }

        public void setCompound(CompoundCandidate candidate) {
            removeAll();
            if (candidate == null || candidate.compound == null || candidate.compound.databases == null) return;
            final ArrayList<String> dbNames = new ArrayList<>(candidate.compound.databases.keySet());
            Collections.sort(dbNames);
            final FontMetrics m = getFontMetrics(ownFont);
            java.util.List<de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel> labels = new ArrayList<>();

            final Rectangle2D boundary = getBounds();

            for (de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel label : candidate.labels) {
                final TextLayout tlayout = new TextLayout(label.name, ownFont, new FontRenderContext(null, false, false));
                final Rectangle2D r = tlayout.getBounds();
                final int X = (int) r.getWidth() + 2 * DB_LABEL_PADDING + 6;
                final int Y = (int) r.getHeight() + 2 * DB_LABEL_PADDING + 6;
                add(new DatabaseLabel(label, X, Y, bgColor, ownFont));

            }
        }
/*
        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D)graphics;
            if (candidate!=null) {
                final ArrayList<String> dbNames = new ArrayList<>(candidate.compound.databases.keySet());
                Collections.sort(dbNames);
                g.setFont(ownFont);
                final FontMetrics fm = g.getFontMetrics();
                int x=0, y=0;
                for (String dbname : dbNames) {
                    final Rectangle2D r = fm.getStringBounds(dbname, g);
                    final int w = (int)r.getWidth();
                    final int h = (int)r.getHeight();
                    g.setColor(new Color(43,94,139));
                    g.fillRoundRect(x,y, w+4, h+4, w, h);
                    g.setColor(Color.WHITE);
                    g.drawString(dbname, x+2, h-2);
                    x += w + 5;

                }
            }
        }
        */
    }

    private static class DatabaseLabel extends JPanel {
        private String name;
        private Color color;
        private de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel label;

        public DatabaseLabel(de.unijena.bioinf.sirius.gui.fingerid.DatabaseLabel label, int width, int height, Color color, Font font) {
            this.name = label.name;
            this.color = color;
            setFont(font);
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));
            this.label = label;
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            final Graphics2D g = (Graphics2D) graphics;
            final FontMetrics m = getFontMetrics(getFont());
            final int tw = m.stringWidth(name);
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
            g.setColor(Color.WHITE);
            g.drawString(name, 2 + (w - tw) / 2, h - (h - th) / 2);
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
        public void paint(Graphics g) {
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
        public void paint(Graphics g) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (Double.isNaN(score)) return;
            g.setFont(nameFont);
            final String t1 = "Score: e";
            int widthB = g.getFontMetrics().stringWidth(t1);
            g.drawString(t1, 0, 14);
            g.setFont(scoreSuperscriptFont);
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

        public void setCompound(CompoundCandidate value) {
            setFont(propertyFont);
            inchi.setText(value.compound.inchi.key2D());
            databasePanel.setCompound(value);
            xlogP.setLogP(value.compound.xlogP);
            scoreL.setScore(value.getScore());
            if (value.data == null) {
                ag.agreement = null;
            } else {
                ag.setAgreement(value.getSubstructures(computation, value.getPlatts()));//todo should that be here??? protected :CSIFingerIdComputation computation;
            }
        }
    }
}
