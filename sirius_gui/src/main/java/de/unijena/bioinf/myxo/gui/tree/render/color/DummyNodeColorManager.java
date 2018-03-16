package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;

public class DummyNodeColorManager extends NodeColorManager {

	public Color getColor(TreeNode node) {
		return Color.WHITE;
	}

	public double getMinimalValue() {
		return 0;
	}

	public double getMaximalValue() {
		return 0;
	}

	public String getLegendLowText() {
		return "";
	}

	public String getLegendMiddelText() {
		return "";
	}

	public String getLegendHighText() {
		return "";
	}

	public String getLegendName() {
		return "";
	}

	public Color getColor(double value) {
		return new Color(1f,1f,1f,0f);
	}

	@Override
	public double getValue(TreeNode node) {
		return 0;
	}

}
