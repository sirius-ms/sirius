package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;



public class RelativeIntensityNodeColorManager extends NodeColorManager {

    public RelativeIntensityNodeColorManager() {
        minValue = -2;
        maxValue = 0;

        range = maxValue - minValue;

        halfRange = range / 2;

        posM = 2 / range;
        negM = -posM;
    }

    public double getValue(TreeNode node) {
        double relInt = node.getPeakRelativeIntensity();
        double logRelInt = java.lang.StrictMath.log10(relInt);
		return logRelInt;
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