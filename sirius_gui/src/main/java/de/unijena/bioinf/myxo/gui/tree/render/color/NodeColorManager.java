package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;
import java.util.ArrayDeque;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.StrictMath.abs;

public abstract class NodeColorManager{

	protected double halfRange;
	protected double posM, negM;
	private boolean switchBlueAndRed = false;

    protected double minValue;
	protected double maxValue;
	protected double range;

	public NodeColorManager() {

	}

	private double getRedValue(double value) {
		if (value <= halfRange) {
			return 1;
		} else {
			return negM * value + 2;
		}
	}

	private double getGreenValue(double value) {
		if (value < halfRange) {
			return posM * value;
		} else if (value == halfRange) {
			return 1;
		} else {
			return negM * value + 2;
		}
	}

	private double getBlueValue(double value) {
		if (value <= halfRange) {
			return posM * value;
		} else {
			return 1;
		}
	}

	public Color getColor(double value) {

		value = value - minValue;
		value = min(value, range);
		value = max(value, 0d);

		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		double bTemp = getBlueValue(value);

		if (rTemp > 1 || rTemp < 0 || gTemp > 1 || gTemp < 0 || bTemp > 1 || bTemp < 0)
			throw new RuntimeException("v " + value + " p0 " + 0 + " halfRange " + halfRange + " p2 " + range + " rT " + rTemp + " gT " + gTemp + " bT " + bTemp);

		double maxValue = 255;
		double minValue = 175;
		double maxMinDiff = maxValue - minValue;

		int rVal = (int) (minValue + rTemp * maxMinDiff);
		int gVal = (int) (minValue + gTemp * maxMinDiff);
		int bVal = (int) (minValue + bTemp * maxMinDiff);

		if(switchBlueAndRed)
			return new Color(bVal, gVal, rVal);
		else
			return new Color(rVal, gVal, bVal);
	}

	public void setSwitchBlueAndRed(boolean switchBlueAndRed)
	{
		this.switchBlueAndRed = switchBlueAndRed;
	}


	public abstract double getValue(TreeNode node);

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

	public double getMaximalValue()
	{
		return maxValue;
	}

	public double getMinimalValue()
	{
		return minValue;
	}

	public double getRange()
    {
        return range;
    }

	public Color getColor(TreeNode node) {
		return getColor(getValue(node));
	}
}
