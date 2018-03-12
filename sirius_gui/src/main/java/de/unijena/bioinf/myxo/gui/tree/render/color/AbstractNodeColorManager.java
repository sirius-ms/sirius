package de.unijena.bioinf.myxo.gui.tree.render.color;

import de.unijena.bioinf.myxo.gui.tree.render.NodeColorManager;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.awt.*;
import java.util.ArrayDeque;

public abstract class AbstractNodeColorManager implements NodeColorManager {
	
	protected double minValue, maxValue, diff;
	
	public AbstractNodeColorManager(TreeNode root){
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
		
		diff = maxValue - minValue;
	}
	
	@Override
	public double getMinimalValue(){
		return this.minValue;
	}
	
	@Override
	public double getMaximalValue(){
		return this.maxValue;
	}
	
	@SuppressWarnings("unused")
	protected double getValueDifference(){
		return this.diff;
	}

	@Override
	public Color getColor(TreeNode node) {
		return getColor(getValue(node));
	}

	public abstract double getValue(TreeNode node);

}
