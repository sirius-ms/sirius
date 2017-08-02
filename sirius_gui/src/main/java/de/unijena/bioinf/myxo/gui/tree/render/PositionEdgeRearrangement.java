package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PositionEdgeRearrangement implements EdgeRearrangement {
	
	private EdgePositionComparator comp;
	
	public PositionEdgeRearrangement() {
		comp = new EdgePositionComparator();
	}

	@Override
	public void rearrangeTreeNodes(TreeNode root) {
		rearrange(root);
	}
	
	private void rearrange(TreeNode node){
		if(node.getOutEdgeNumber()==0) return;
		
		if(node.getOutEdgeNumber()==1){
			rearrange(node.getOutEdge(0).getTarget());
			return;
		}
		
		List<TreeEdge> edges = new ArrayList<>(node.getOutEdges());
		Collections.sort(edges,comp);
		node.setOutEdges(edges);
		
		for(TreeEdge edge : edges) rearrange(edge.getTarget());
	}

}

class EdgePositionComparator implements Comparator<TreeEdge>{

	@Override
	public int compare(TreeEdge o1, TreeEdge o2) {
		int pos1 = o1.getTarget().getHorizontalPosition();
		int pos2 = o2.getTarget().getHorizontalPosition();
		if(pos1<pos2)return -1;
		else if(pos1>pos2) return 1;
		return 0;
	}
	
}