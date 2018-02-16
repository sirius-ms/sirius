package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.*;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class IdentificationCellRenderer extends JPanel implements ListCellRenderer<ExperimentContainer> {

    private ExperimentContainer ec;

    private Color backColor, foreColor;

    private Font valueFont, compoundFont, propertyFont;

    private static Image icon;

//	private int index;

    private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
    private Color activatedForeground, deactivatedForeground, disableBackground;

    protected AtomContainerRenderer renderer;

//	private FormulaDisassambler disamb;

//	private DecimalFormat massFormat, ppmFormat, intFormat;

    private DecimalFormat numberFormat, scoreFormat;
    private ImageIcon loadingGif;

    public IdentificationCellRenderer(){
        this.setPreferredSize(new Dimension(200,86));
        initColorsAndFonts();
        this.numberFormat = new DecimalFormat("#0.00");
        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new BasicBondGenerator());
        generators.add(new RingGenerator());
        generators.add(new BasicAtomGenerator());
        generators.add(new HighlightGenerator());
        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());
    }

    public void initColorsAndFonts(){
        try{
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            compoundFont = tempFont.deriveFont(13f);

            propertyFont = tempFont.deriveFont(12f);
        }catch(Exception e){
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
        }

        try{
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            valueFont = tempFont.deriveFont(12f);

        }catch(Exception e){
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
        }

        selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
        selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
        evenBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\".background");
        disableBackground = UIManager.getColor("ComboBox.background");
        unevenBackground = new Color(213,227,238);
        activatedForeground = UIManager.getColor("List.foreground");
        deactivatedForeground = Color.GRAY;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ExperimentContainer> list, ExperimentContainer value,
            int index, boolean isSelected, boolean cellHasFocus) {
        this.ec = value;
        if(isSelected){
            this.backColor = this.selectedBackground;
            this.foreColor = this.selectedForeground;
        }else{
            if(index%2==0) this.backColor = this.evenBackground;
            else this.backColor = this.unevenBackground;
            this.foreColor = this.activatedForeground;
//			if(value.formulaUsed()) this.foreColor = this.activatedForeground;
//			else this.foreColor = this.foreColor = this.deactivatedForeground;
        }

        this.setToolTipText(ec.getGUIName());

        return this;
    }



    @Override
    public void paint(Graphics g){

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(this.backColor);

        g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());

        /*
            Draw identified compound
         */
        Compound c = ec.getBestHit().getFingerIdData().compounds[0];
        /*
        if (c.molecule != null) {
            final Graphics2D gg = (Graphics2D) g;
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(c.getMolecule(), false);
            try {
                sdg.generateCoordinates();
            } catch (CDKException e) {
                e.printStackTrace();
            }
            renderer.getRenderer2DModel().set(BasicSceneGenerator.BackgroundColor.class, backColor);
            synchronized (c) {
                renderer.paint(c.getMolecule(), new AWTDrawVisitor(gg),
                        new Rectangle2D.Double(100, 3, 100, 80), true);
            }
        }
        */
        /////


        FontMetrics compoundFm = g2.getFontMetrics(this.compoundFont);
        FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
        FontMetrics valueFm = g2.getFontMetrics(this.valueFont);

        g2.setColor(this.foreColor);

        int compoundLength = compoundFm.stringWidth(ec.getGUIName())+4;

        boolean trigger = compoundLength + 2 > 198;

        Paint p = g2.getPaint();

        if(trigger){
            g2.setPaint(new GradientPaint(180, 0, foreColor,199, 0, backColor));
        }

        g2.drawLine(2, 17, Math.min(197,2+compoundLength), 17);

        g2.setFont(compoundFont);
        g2.drawString(ec.getGUIName(), 4, 13);

        if(trigger) g2.setPaint(p);

        int ms1No = ec.getMs1Spectra().size();
        int ms2No = ec.getMs2Spectra().size();

        String focMassProp = "ion mass";
        String name = "name";
        int ionLength = propertyFm.stringWidth(name);
        int focLength = propertyFm.stringWidth(focMassProp);

        g2.setFont(propertyFont);
        g2.drawString(focMassProp,4,32);
        g2.drawString(name,4,48);

        int xPos = Math.max(propertyFm.stringWidth("confidence"),Math.max(ionLength,focLength))+10;

        double focD = ec.getIonMass();
        String focMass = focD>0 ? numberFormat.format(focD)+" Da" : "unknown";

        String nameOrInchi = c.name;
        if (c.name==null || c.name.isEmpty()) nameOrInchi = c.smiles.smiles;
        if (nameOrInchi==null) nameOrInchi = "";
        if (nameOrInchi.length() > 13) {
            nameOrInchi = nameOrInchi.substring(0, 10) + "...";
        }

        g2.setFont(valueFont);
        g2.drawString(focMass,xPos,32);
        g2.drawString(nameOrInchi,xPos,48);

        int yPos = 64;

        g2.setFont(propertyFont);
        String confidence = "confidence";
        int confidenceLength = propertyFm.stringWidth(confidence);

        g2.drawString(confidence, 4, yPos);
        g2.setFont(valueFont);
        g2.drawString(numberFormat.format(ec.getBestHit().getFingerIdData().getConfidence()), xPos, yPos);
    }

}