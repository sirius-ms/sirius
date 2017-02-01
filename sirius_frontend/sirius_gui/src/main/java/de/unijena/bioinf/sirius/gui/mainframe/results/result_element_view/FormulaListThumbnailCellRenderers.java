package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;

import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class FormulaListThumbnailCellRenderers implements ListCellRenderer<SiriusResultElement> {
	
	private HashMap<SiriusResultElement,TreeRenderPanel> renderPanels;
	
	private Color unevenColor, selectedColor;
	
	public FormulaListThumbnailCellRenderers(List<SiriusResultElement> results){
		renderPanels = new HashMap<>();
		unevenColor = new Color(230,239,255);
		selectedColor = new Color(187,210,255);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends SiriusResultElement> list, SiriusResultElement value, int index,
			boolean isSelected, boolean callHasFocus) {
		TreeRenderPanel panel = renderPanels.get(value);
		
		Color currColor = index%2==1 ? unevenColor : Color.white;
		if(isSelected) currColor = selectedColor;
		
		Color oldColor = panel.getBackgroundColor();
		
		if(currColor!=oldColor) panel.changeBackgroundColor(currColor);
		
		return panel;
	}
	
}
