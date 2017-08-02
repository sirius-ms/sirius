package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class BGRRelativeIntensityNodeColorManager extends BGRNodeColorManager {

	@SuppressWarnings("unused")
	public BGRRelativeIntensityNodeColorManager(TreeNode root) {
		super(root);
	}

	@Override
	public double getValue(TreeNode node) {
		return node.getPeakRelativeIntensity();
	}

}
