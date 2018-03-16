package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;
import java.util.ArrayDeque;

public abstract class NodeColorManager{

	private double p0, p1, p2;
	private double posM, negM;
	private boolean switchBlueAndRed = false;

    private double minValue;
    private double maxValue;
    private double range;

	public NodeColorManager() {

	}

	public NodeColorManager(TreeNode root) {
		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;

		ArrayDeque<TreeNode> nodeStorage = new ArrayDeque<>();
		nodeStorage.addFirst(root);

		while(!nodeStorage.isEmpty()){
			TreeNode node = nodeStorage.removeLast();
			if(getValue(node)<minValue) minValue = getValue(node);
			if(getValue(node)>maxValue) maxValue = getValue(node);
			if(node.getOutEdgeNumber()>0){
				for(TreeEdge edge : node.getOutEdges()) nodeStorage.addFirst(edge.getTarget());
			}
		}

		range = maxValue - minValue;

		p0 = 0;
		p1 = range / 2;
		p2 = range;

		posM = 2 / range;
		negM = -posM;

	}

	private double getRedValue(double value) {
		if (value <= p1) {
			return 1;
		} else {
			return negM * value + 2;
		}
	}

	private double getGreenValue(double value) {
		if (value < p1) {
			return posM * value;
		} else if (value == p1) {
			return 1;
		} else {
			return negM * value + 2;
		}
	}

	private double getBlueValue(double value) {
		if (value <= p1) {
			return posM * value;
		} else {
			return 1;
		}
	}

	public Color getColor(double value) {
		value = value - minValue;
		double rTemp = getRedValue(value);
		double gTemp = getGreenValue(value);
		double bTemp = getBlueValue(value);

		if (rTemp > 1 || rTemp < 0 || gTemp > 1 || gTemp < 0 || bTemp > 1 || bTemp < 0)
			throw new RuntimeException("v " + value + " p0 " + p0 + " p1 " + p1 + " p2 " + p2 + " rT " + rTemp + " gT " + gTemp + " bT " + bTemp);

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
