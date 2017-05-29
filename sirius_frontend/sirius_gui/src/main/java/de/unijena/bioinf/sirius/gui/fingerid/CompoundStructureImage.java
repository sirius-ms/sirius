package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.configs.Fonts;
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
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by fleisch on 24.05.17.
 */
class CompoundStructureImage extends JPanel {

    protected static final Font nameFont, rankFont,  matchFont;
    static {
        //init fonts
        final Font tempFont = Fonts.FONT_BOLD;
        if (tempFont != null){
            nameFont = tempFont.deriveFont(13f);
            matchFont = tempFont.deriveFont(18f);
            rankFont = tempFont.deriveFont(32f);
        }else {
            nameFont = matchFont = rankFont = Font.getFont(Font.SANS_SERIF);

        }
    }

    protected CompoundCandidate molecule;
    protected AtomContainerRenderer renderer;
    protected Color backgroundColor;



    public CompoundStructureImage() {
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
