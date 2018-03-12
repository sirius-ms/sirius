package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.myxo.gui.tree.render.NodeColorManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;

public class ScoreVisualizationPanel extends JPanel {
	
	private NodeColorManager ncm;
	private Font valueFont;

	public ScoreVisualizationPanel() {
		ncm = null;
		this.valueFont = null;
		initColorsAndFonts();
		this.setMinimumSize(new Dimension(200,25));
		this.setPreferredSize(new Dimension(200,25));
	}
	
	public void initColorsAndFonts(){

		try{
			InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
			Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
			valueFont = tempFont.deriveFont(10f);
			
		}catch(Exception e){
			LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
		}
		
	}
	
	public void setNodeColorManager(NodeColorManager ncm){
		this.ncm = ncm;
	}
	
	@Override
	public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(1f, 1f, 1f, 0f));
        g2.fillRect(0, 0, 200, 25);
        if (ncm == null) {
            return;
        }
        double diff = ncm.getMaximalValue() - ncm.getMinimalValue();
        double stepSize = diff / 200;
//		double value = ncm.getMinimalValue();
        for (int i = 0; i < 199; i++) {
            Color c = ncm.getColor(ncm.getMinimalValue() + (i * stepSize));
            g2.setColor(c);
            g2.drawLine(i, 0, i, 12);
//			value += stepsize;
        }
        Color c = ncm.getColor(ncm.getMaximalValue());
        g2.setColor(c);
        g2.drawLine(199, 0, 199, 12);

        g2.setFont(valueFont);
        g2.setColor(c);

        g2.setColor(Color.black);
        FontMetrics fm = g2.getFontMetrics(valueFont);
        String text = ncm.getLegendLowText();
        g2.drawString(text, 0, 22);

        text = ncm.getLegendMiddelText();
        int textLength = fm.stringWidth(text);
        g2.drawString(text, 100 - (textLength / 2), 22);

        text = ncm.getLegendHighText();
        textLength = fm.stringWidth(text);
        g2.drawString(text, 200 - textLength, 22);
    }

}
