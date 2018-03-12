package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class RWBRelativeIntensityNodeColorManager extends RWBNodeColorManager {

	public RWBRelativeIntensityNodeColorManager(TreeNode root) {
		super(root);
	}

	@Override
	public double getValue(TreeNode node) {
		return java.lang.StrictMath.log10(node.getPeakRelativeIntensity());
	}

    @Override
    public String getLegendLowText() {
        return "0%";
    }

    @Override
    public String getLegendMiddelText() {
        return "10%";
    }

    @Override
    public String getLegendHighText() {
        return "100%";
    }

    @Override
    public String getLegendName() {
        return "Intensity";
    }
}
