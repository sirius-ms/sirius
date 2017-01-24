package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.configs.Style;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.Colors;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;

public class ResultTreeListTextCellRenderer extends JLabel implements ListCellRenderer<SiriusResultElement> {

	private Color backColor, foreColor;
	
	private Font valueFont, mfFont, propertyFont, rankFont,statusFont;
	
	private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
	private Color activatedForeground, deactivatedForeground, disableBackground;
	
	private SiriusResultElement sre;
    protected ExperimentContainer ec;
	
//	private FormulaDisassambler disamb;
	
//	private DecimalFormat massFormat, ppmFormat, intFormat;
	
	private DecimalFormat numberFormat;

	public ResultTreeListTextCellRenderer(){
		this.setPreferredSize(new Dimension(200,45));
		initColorsAndFonts();
		sre = null;
		this.numberFormat = new DecimalFormat("#0.000000");
//		this.disamb = new FormulaDisassambler();
//		this.ppmFormat = new DecimalFormat("#0.00");
//		this.massFormat = new DecimalFormat("#0.00");
	}
	
	public void initColorsAndFonts(){
		
		try{
			InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
			Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
			mfFont = tempFont.deriveFont(13f);
			propertyFont = tempFont.deriveFont(12f);
			statusFont = tempFont.deriveFont(24f);
			rankFont = tempFont.deriveFont(16f);
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
			JList<? extends SiriusResultElement> list, SiriusResultElement value,
			int index, boolean isSelected, boolean cellHasFocus) {
//		list.setPreferredSize(new Dimension(list.getModel().getSize()*200,45));
		this.sre = value;
		if(isSelected){
			if (isTopHit(value)) {
			    this.backColor = Style.SELECTED_GREEN;
            } else {
                this.backColor = this.selectedBackground;
            }
			this.foreColor = this.selectedForeground;
		}else{
            if (isTopHit(value)) {
                this.backColor = Style.LIGHT_GREEN;
            } else {
                if(index%2==0) this.backColor = this.evenBackground;
                else this.backColor = this.unevenBackground;
            }
			this.foreColor = this.activatedForeground;
//			if(value.formulaUsed()) this.foreColor = this.activatedForeground;
//			else this.foreColor = this.foreColor = this.deactivatedForeground;
		}
		
		return this;
	}

    private boolean isTopHit(SiriusResultElement value) {
        if (ec==null) return false;
        return (ec.getBestHit()==value);
    }


    @Override
	public void paint(Graphics g){
		
		Graphics2D g2 = (Graphics2D) g; 
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(this.backColor);
		
		g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());
		
		FontMetrics mfFm = g2.getFontMetrics(this.mfFont);
		FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
//		FontMetrics valueFm = g2.getFontMetrics(this.valueFont);
		FontMetrics rankFm = g2.getFontMetrics(this.rankFont);
		
		g2.setColor(this.foreColor);

		final String formulaText = sre.getFormulaAndIonText();
		final int charge = sre.getCharge();


		int mfLength = mfFm.stringWidth(formulaText)+4;
		int rankLength = rankFm.stringWidth(Integer.toString(sre.getRank()));
		
		g2.drawLine(13+rankLength, 17, 15+mfLength+rankLength, 17);
		
		g2.setFont(mfFont);
		g2.drawString(formulaText, 15+rankLength, 13);
		g2.drawString(charge>0 ? "+" : "-", 15+mfLength+rankLength-4, 13-4);
		
		g2.setFont(rankFont);
		g2.drawString(Integer.toString(sre.getRank()),2,15);
		
		int scoreLength = propertyFm.stringWidth("Score:");
		g2.setFont(propertyFont);
		g2.drawString("Score:",10,35);
		g2.setFont(valueFont);
		g2.drawString(numberFormat.format(sre.getScore()), 15+scoreLength,35);
//
        if (sre!=null && sre.fingerIdComputeState!=null) {
            g.setFont(statusFont);
			final ComputingStatus ec = sre.fingerIdComputeState;
            if (ec==ComputingStatus.COMPUTED) {
				g2.setColor(Colors.ICON_GREEN);
				g2.drawString("\u2713", getWidth()-24, getHeight()-8);
            } else if (ec==ComputingStatus.COMPUTING) {
                g2.drawString("\u2699", getWidth()-24, getHeight()-8);
            } else if (ec==ComputingStatus.FAILED){
                final Color prevCol = g2.getColor();
                g2.setColor(Colors.ICON_RED);
                g2.drawString("\u2718", getWidth()-24, getHeight()-8);
                g2.setColor(prevCol);
            } else if (ec==ComputingStatus.QUEUED) {
                g2.drawString("...", getWidth()-24, getHeight()-8);
            }
        }



//		int ms1No = ec.getMs1Spectra().size();
//		int ms2No = ec.getMs2Spectra().size();
//		
//		System.out.println(ms1No+" "+ms2No);
//		
//		String ionizationProp = "ionization";
//		String focMassProp = "focused mass";
//		int ionLength = propertyFm.stringWidth(ionizationProp);
//		int focLength = propertyFm.stringWidth(focMassProp);
//		
//		g2.setFont(propertyFont);
//		g2.drawString(ionizationProp,4,32);
//		g2.drawString(focMassProp,4,48);
//		
//		int xPos = Math.max(ionLength,focLength)+15;
//		
//		String ionValue = null;
//		Ionization ioni = ec.getIonization();
//		if(ioni == Ionization.M) ionValue = "M+";
//		else if(ioni == Ionization.MMinusH) ionValue = "[M-H]-";
//		else if(ioni == Ionization.MPlusH) ionValue = "[M+H]+";
//		else if(ioni == Ionization.MPlusNa) ionValue = "[M+Na]+";
//		if(ionValue==null||ionValue.isEmpty()) ionValue = "unknown";
//		double focD = ec.getSelectedFocusedMass();
//		String focMass = focD>0 ? numberFormat.format(focD) : "unknown";
//		
//		g2.setFont(valueFont);
//		g2.drawString(ionValue,xPos,32);
//		g2.drawString(focMass,xPos,48);
//		
//		int yPos = 64;
//		
//		g2.setFont(valueFont);
//		
//		if(ms1No>0){
//			String ms1String = ms1No==1 ? "spectrum " : "spectra";
//			ms1String = ms1No+" ms1 "+ms1String;
//			g2.drawString(ms1String, 4, yPos);
//			yPos+=16;
//		}
//		
//		if(ms2No>0){
//			String ms2String = ms2No==1 ? "spectrum " : "spectra";
//			ms2String = ms2No+" ms2 "+ms2String;
//			g2.drawString(ms2String, 4, yPos);
//		}
		

	}

}
