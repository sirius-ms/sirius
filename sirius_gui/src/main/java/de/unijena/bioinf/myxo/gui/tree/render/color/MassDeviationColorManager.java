package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.min;

public class MassDeviationColorManager extends RWBNodeColorManager {

	public MassDeviationColorManager(TreeNode root) {
		super(root);
		setSwitchBlueAndRed(true);
	}

	@Override
	public double getValue(TreeNode node) {
		return min(abs(node.getDeviationMass()), 10d);
	}

    @Override
    public String getLegendLowText() {
        return "0";
    }

    @Override
    public String getLegendMiddelText() {
        return "±5ppm";
    }

    @Override
    public String getLegendHighText() {
        return "±10ppm";
    }

    @Override
    public String getLegendName() {
        return "mass deviation";
    }
}
