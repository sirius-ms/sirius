package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class RelativeIntensityNodeColorManager extends NodeColorManager {

	public RelativeIntensityNodeColorManager(TreeNode root) {
		super(root);
	}

	public double getValue(TreeNode node) {
		return java.lang.StrictMath.log10(node.getPeakRelativeIntensity());
	}

    public String getLegendLowText() {
        return "0%";
    }

    public String getLegendMiddelText() {
        return "10%";
    }

    public String getLegendHighText() {
        return "100%";
    }

    public String getLegendName() {
        return "Intensity";
    }
}
