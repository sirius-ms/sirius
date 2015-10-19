package de.unijena.bioinf.sirius.gui.mainframe.results;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.render.NodeType;
import de.unijena.bioinf.myxo.gui.tree.render.color.RGBScoreNodeColorManager;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public class ResultTreeListThumbnailCellRenderers implements ListCellRenderer<SiriusResultElement> {
	
	private HashMap<SiriusResultElement,TreeRenderPanel> renderPanels;
	
	private Color unevenColor, selectedColor;
	
	public ResultTreeListThumbnailCellRenderers(List<SiriusResultElement> results){
		renderPanels = new HashMap<>();
		unevenColor = new Color(230,239,255);
		selectedColor = new Color(187,210,255);
		updateData(results);
	}
	
	public void updateData(List<SiriusResultElement> results){
		throw new RuntimeException("not implemented");
//		for(int i=0;i<results.size();i++){
//			SiriusResultElement sre = results.get(i);
//			TreeRenderPanel panel = new TreeRenderPanel(sre.getTree());
//			
//			Color color = Color.white;
//			if(i%2==1){
//				color = unevenColor;
//			}
//			
//			panel.changeRenderParameters(color, NodeType.thumbnail, new RGBScoreNodeColorManager(sre.getTree()));
//			
//			System.out.println("minSize: "+panel.getPreferredSize().getWidth()+" "+panel.getPreferredSize().getHeight());
//			
//			renderPanels.put(sre, panel);
//		}
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
