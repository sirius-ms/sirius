package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class RGRelativeIntensityNodeColorManager extends RGNodeColorManager {

	public RGRelativeIntensityNodeColorManager(TreeNode root) {
		super(root);
	}

	@Override
	public double getValue(TreeNode node) {
		return node.getPeakRelativeIntensity();
	}

}
