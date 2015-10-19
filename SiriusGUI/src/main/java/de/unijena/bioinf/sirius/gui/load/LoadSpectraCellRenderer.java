package de.unijena.bioinf.sirius.gui.load;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.gui.msviewer.data.MolecularFormulaInformation;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

public class LoadSpectraCellRenderer extends JLabel implements ListCellRenderer<SpectrumContainer>{
	
	private SpectrumContainer sp;

	private Color backColor, foreColor;
	
	private Font msLevelFont, propertyFont, valueFont;
	
//	private int index;
	
	private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
	private Color activatedForeground, deactivatedForeground, disableBackground;
	
//	private FormulaDisassambler disamb;
	
//	private DecimalFormat massFormat, ppmFormat, intFormat;
	
	private DecimalFormat cEFormat;
	
	public LoadSpectraCellRenderer(){
		this.setPreferredSize(new Dimension(150,32));
		initColorsAndFonts();
		this.cEFormat = new DecimalFormat("#0.0");
//		this.disamb = new FormulaDisassambler();
//		this.ppmFormat = new DecimalFormat("#0.00");
//		this.massFormat = new DecimalFormat("#0.00");
	}
	
	public void initColorsAndFonts(){
		try{
			File fontFile = new File("ttf/DejaVuSans-Bold.ttf");
			Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
			msLevelFont = tempFont.deriveFont(12f);
			
			propertyFont = tempFont.deriveFont(12f);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try{
			File fontFile = new File("ttf/DejaVuSans.ttf");
			Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
			valueFont = tempFont.deriveFont(12f);
			
		}catch(Exception e){
			e.printStackTrace();
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
			JList<? extends SpectrumContainer> list, SpectrumContainer value,
			int index, boolean isSelected, boolean cellHasFocus) {
		this.sp = value;
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
		
		return this;
	}
	
	
	
	@Override
	public void paint(Graphics g){
		
		Graphics2D g2 = (Graphics2D) g; 
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(this.backColor);
		
		g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());
		
		FontMetrics msLevelFm = g2.getFontMetrics(this.msLevelFont);
		FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
		FontMetrics valueFm = g2.getFontMetrics(this.valueFont);
		
		g2.setColor(this.foreColor);
		
		int mslevel = sp.getSpectrum().getMSLevel();
		
		String peakProp = "peaks:";
		String cEProp = "cE:";
		
		int peakPropLength = valueFm.stringWidth(peakProp);
		int cEPropLength = valueFm.stringWidth(cEProp);
		
		g2.setFont(propertyFont);
		g2.drawString(peakProp, 5, 12);
		if(mslevel>1) g2.drawString(cEProp, 5, 28);
		
		String peakVal = ""+sp.getSize();
		
		CollisionEnergy ce = sp.getSpectrum().getCollisionEnergy();
		String cEVal = "";
		if(ce!=null){
			double minE = sp.getSpectrum().getCollisionEnergy().getMinEnergy();
			double maxE = sp.getSpectrum().getCollisionEnergy().getMaxEnergy();
			
			
			if(minE==maxE){
				cEVal = cEFormat.format(minE)+" eV";
			}else{
				cEVal = cEFormat.format(minE)+" - "+cEFormat.format(maxE)+" eV";
			}
		}
		
		int peakValLength = valueFm.stringWidth(peakVal);
		int cEValLength = valueFm.stringWidth(cEVal);
		
		int valX = Math.max(peakPropLength, cEPropLength) + 15;
		
		g2.setFont(valueFont);
		g2.drawString(peakVal, valX, 12);
		if(mslevel>1) g2.drawString(cEVal, valX, 28);
		
		g2.setFont(msLevelFont);
		
		String msLevel = "MS "+sp.getSpectrum().getMSLevel();
		
		int msLevelLength = msLevelFm.stringWidth(msLevel);
		
		g2.drawString(msLevel, ((int)this.getSize().getWidth())-msLevelLength-2, 12);
		
		
	}

}
