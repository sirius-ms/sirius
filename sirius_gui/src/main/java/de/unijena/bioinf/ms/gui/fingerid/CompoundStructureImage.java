package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.configs.Fonts;
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

    protected static final Font nameFont, rankFont, matchFont;

    static {
        //init fonts
        final Font tempFont = Fonts.FONT_BOLD;
        if (tempFont != null) {
            nameFont = tempFont.deriveFont(13f);
            matchFont = tempFont.deriveFont(18f);
            rankFont = tempFont.deriveFont(32f);
        } else {
            nameFont = matchFont = rankFont = Font.getFont(Font.SANS_SERIF);

        }
    }

    protected FingerprintCandidateBean molecule;
    protected AtomContainerRenderer renderer;
    protected Color backgroundColor;

    public CompoundStructureImage() {
        this(StandardGenerator.HighlightStyle.OuterGlow);
    }

    public CompoundStructureImage(StandardGenerator.HighlightStyle highlightStyle) {
        setOpaque(false);
        setPreferredSize(new Dimension(374, 215));
        // make generators
        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(nameFont));

        // setup the renderer
        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());

        renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                highlightStyle);
        renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class,
                new CDK2DAtomColors());
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (molecule.hasAtomContainer())
            renderImage((Graphics2D) g);
    }

    private void renderImage(final Graphics2D gg) {
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(molecule.getMolecule(), false);
        try {
            sdg.generateCoordinates();
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
        renderer.getRenderer2DModel().set(BasicSceneGenerator.BackgroundColor.class, backgroundColor);
        synchronized (molecule.candidate) {
            renderer.paint(molecule.getMolecule(), new AWTDrawVisitor(gg),
                    new Rectangle2D.Double(7, 14, 360, 185), true);
        }
        if (molecule.candidate.getName() != null) {
            gg.setFont(nameFont);
            gg.drawString(molecule.candidate.getName(), 3, 16);
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
//

        //todo change to gif
        final String tanimotoText = molecule.getTanimotoScore() == null  ? "loading..." : String.format(Locale.US, "%.2f", molecule.getTanimotoScore() * 100d) + "%";
        double tw = gg.getFontMetrics(matchFont).getStringBounds(tanimotoText, gg).getWidth();

        gg.setFont(matchFont);
        gg.drawString(tanimotoText, (int) (getWidth() - (tw + 4)), getHeight() - 4);
    }
}
