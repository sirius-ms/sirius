/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.io;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.io.spectrum.SpectrumContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;

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
			msLevelFont = Fonts.FONT_BOLD.deriveFont(12f);
			propertyFont = Fonts.FONT_BOLD.deriveFont(12f);
		}catch(Exception e){
			LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
		}
		
		try{
			valueFont = Fonts.FONT.deriveFont(12f);
			
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
		FontMetrics valueFm = g2.getFontMetrics(this.valueFont);
		
		g2.setColor(this.foreColor);
		
		int mslevel = sp.getSpectrum().getMsLevel();
		
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
		
		int valX = Math.max(peakPropLength, cEPropLength) + 15;
		
		g2.setFont(valueFont);
		g2.drawString(peakVal, valX, 12);
		if(mslevel>1) g2.drawString(cEVal, valX, 28);
		
		g2.setFont(msLevelFont);
		
		String msLevel = "MS "+sp.getSpectrum().getMsLevel();
		
		int msLevelLength = msLevelFm.stringWidth(msLevel);
		
		g2.drawString(msLevel, ((int)this.getSize().getWidth())-msLevelLength-2, 12);
	}
}
