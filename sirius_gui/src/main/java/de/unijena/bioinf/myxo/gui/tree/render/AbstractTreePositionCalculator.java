package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.util.List;

public abstract class AbstractTreePositionCalculator implements TreePositionCalculator {

	@Override
	public void computeRelativePositions(TreeNode root) {
		
		computeTreeSizeRequirement(root);
		setNodeDepth(root, 0);
		
		int sizeY = root.getVerticalRequirement();

		computeVerticalPositions(root, 0);
		
		computeHorizontalPositions(root);
	}

	protected void computeTreeSizeRequirement(TreeNode root){
		calculateSize(root);
	}
	
	protected static void calculateSize(TreeNode node){
		
		if(node.getOutEdgeNumber()==0){
			node.setHorizontalRequirement(1);
			node.setVerticalRequirement(1);
		}else{
			List<TreeEdge> outEdges = node.getOutEdges();
			int horSize = 0;
			int vertSize = 0;
			for(TreeEdge edge : outEdges){
				TreeNode child = edge.getTarget();
				calculateSize(child);
				vertSize = Math.max(child.getVerticalRequirement(), vertSize);
				horSize += child.getHorizontalRequirement();
			}
			node.setHorizontalRequirement(horSize);
			node.setVerticalRequirement(vertSize+1);
		}
		
	}
	
	protected static void setNodeDepth(TreeNode node, int depth){
		
		node.setNodeDepth(depth);
		
		for(TreeEdge edge : node.getOutEdges()){
			setNodeDepth(edge.getTarget(), depth+1);
		}
		
	}
	
	protected static void computeVerticalPositions(TreeNode node, int depth){
//		double value = node.getNodeDepth() * stepSize;
		node.setVerticalPosition(depth);
		for(TreeEdge edge : node.getOutEdges()){
			TreeNode child = edge.getTarget();
			computeVerticalPositions(child,depth+1);
		}
	}
	
	protected abstract void computeHorizontalPositions(TreeNode root);
	
}
