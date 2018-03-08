package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import static java.lang.StrictMath.abs;

public class MassDeviationColorManager extends RWBNodeColorManager {

	public MassDeviationColorManager(TreeNode root) {
		super(root);
	}

	@Override
	public double getValue(TreeNode node) {
		return node.getDeviationMass();
	}

}
