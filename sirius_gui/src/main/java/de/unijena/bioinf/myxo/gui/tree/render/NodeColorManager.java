package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public interface NodeColorManager {
	
	public Color getColor(TreeNode node);
	
	@SuppressWarnings("unused")
	public double getMinimalValue();
	
	@SuppressWarnings("unused")
	public double getMaximalValue();
	
	public Color getColor(double value);

}
