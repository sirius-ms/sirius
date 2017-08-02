package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.render.NodeColorManager;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public class DummyNodeColorManager implements NodeColorManager {

	@Override
	public Color getColor(TreeNode node) {
		return Color.WHITE;
	}

	@Override
	public double getMinimalValue() {
		return 0;
	}

	@Override
	public double getMaximalValue() {
		return 0;
	}

	@Override
	public Color getColor(double value) {
		return Color.WHITE;
	}

}
