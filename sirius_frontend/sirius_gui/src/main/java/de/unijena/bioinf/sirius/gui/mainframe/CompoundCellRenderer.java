package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Colors;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;

public class CompoundCellRenderer extends JLabel implements ListCellRenderer<ExperimentContainer>{
	
	private ExperimentContainer ec;

	private Color backColor, foreColor;
	
	private Font valueFont, compoundFont, propertyFont, statusFont;

	private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
	private Color activatedForeground, deactivatedForeground, disableBackground;
	
	private DecimalFormat numberFormat;
	private ImageIcon loadingGif;

	public CompoundCellRenderer(){
		this.setPreferredSize(new Dimension(200,86));
		initColorsAndFonts();
		this.numberFormat = new DecimalFormat("#0.00");
	}
	
	public void initColorsAndFonts(){
		try{
			InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
			Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
			compoundFont = tempFont.deriveFont(13f);
			propertyFont = tempFont.deriveFont(12f);
			statusFont = tempFont.deriveFont(24f);
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
		
		String ionizationProp = "ionization";
		String focMassProp = "parent mass";
		int ionLength = propertyFm.stringWidth(ionizationProp);
		int focLength = propertyFm.stringWidth(focMassProp);
		
		g2.setFont(propertyFont);
		g2.drawString(ionizationProp,4,32);
		g2.drawString(focMassProp,4,48);
		
		int xPos = Math.max(ionLength,focLength)+15;
		
		String ionValue = ec.getIonization().toString();
		double focD = ec.getFocusedMass();
		String focMass = focD>0 ? numberFormat.format(focD)+" Da" : "unknown";
		
		g2.setFont(valueFont);
		g2.drawString(ionValue,xPos,32);
		g2.drawString(focMass,xPos,48);
		
		int yPos = 64;
		
		g2.setFont(valueFont);
		
		if(ms1No>0){
			String ms1String = ms1No==1 ? "spectrum " : "spectra";
			ms1String = ms1No+" ms1 "+ms1String;
			g2.drawString(ms1String, 4, yPos);
			yPos+=16;
		}
		
		if(ms2No>0){
			String ms2String = ms2No==1 ? "spectrum " : "spectra";
			ms2String = ms2No+" ms2 "+ms2String;
			g2.drawString(ms2String, 4, yPos);
		}



		g2.setFont(statusFont);
		SwingUtils.drawListStatusElement(ec.getComputeState(),g2,this);
	}

}