package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public interface NodeColorManager {
	
	Color getColor(TreeNode node);
	
	@SuppressWarnings("unused")
    double getMinimalValue();
	
	@SuppressWarnings("unused")
    double getMaximalValue();
	
	Color getColor(double value);

}
