package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.min;

public class MassDeviationColorManager extends NodeColorManager {

	public MassDeviationColorManager(TreeNode root) {
		super(root);
		setSwitchBlueAndRed(true);
	}

	public double getValue(TreeNode node) {
		return min(abs(node.getDeviationMass()), 10d);
	}

    public String getLegendLowText() {
        return "0";
    }

    public String getLegendMiddelText() {
        return "±5ppm";
    }

    public String getLegendHighText() {
        return "±10ppm";
    }

    public String getLegendName() {
        return "mass deviation";
    }
}
