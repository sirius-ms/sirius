package de.unijena.bioinf.ms.gui.mainframe.molecular_formular;

import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class FormulaListThumbnailCellRenderers implements ListCellRenderer<FormulaResultBean> {
	
	private HashMap<FormulaResultBean,TreeRenderPanel> renderPanels;
	
	private Color unevenColor, selectedColor;
	
	public FormulaListThumbnailCellRenderers(List<FormulaResultBean> results){
		renderPanels = new HashMap<>();
		unevenColor = new Color(230,239,255);
		selectedColor = new Color(187,210,255);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends FormulaResultBean> list, FormulaResultBean value, int index,
												  boolean isSelected, boolean callHasFocus) {
		TreeRenderPanel panel = renderPanels.get(value);
		
		Color currColor = index%2==1 ? unevenColor : Color.white;
		if(isSelected) currColor = selectedColor;
		
		Color oldColor = panel.getBackgroundColor();
		
		if(currColor!=oldColor) panel.changeBackgroundColor(currColor);
		
		return panel;
	}
	
}
