package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FirstEdgeRearrangement implements EdgeRearrangement {
	
	private static TreeEdgeComperator comp = new TreeEdgeComperator();
	
	public void rearrangeTreeNodes(TreeNode root){
		
		setNodeNumber(root);
		
		rearrangeNode(root,0);
		
		
	}
	
	private void rearrangeNode(TreeNode node, int depth){
		if(node.getOutEdgeNumber()==0) return;
		if(node.getOutEdgeNumber()==1){
			rearrangeNode(node.getOutEdge(0).getTarget(),depth+1);
		}else{
			
			List<TreeEdge> edges = new ArrayList<TreeEdge>(node.getOutEdges());
			Collections.sort(edges, Collections.reverseOrder(comp));
			
			int middlePos;
			double middleTemp = (edges.size()-1) / 2.0;
			if(depth%2 == 0) middlePos = (int) Math.floor(middleTemp);
			else middlePos = (int) Math.ceil(middleTemp);
			
//			int middlePos = edges.size()- / 2;
			TreeEdge[] temp = new TreeEdge[edges.size()];
			temp[middlePos] = edges.get(0);
			
			int leftEdgePos = 1;
			int rightEdgePos = edges.size()-1;
			int leftPos = middlePos-1;
			int rightPos = middlePos+1;
			
			while(leftPos>=0 || rightPos<temp.length){
				
				if(leftPos>=0){
					temp[leftPos] = edges.get(rightEdgePos);
					rightEdgePos--;
					leftPos--;
				}
				if(leftPos>=0){
					temp[leftPos] = edges.get(leftEdgePos);
					leftEdgePos++;
					leftPos--;
				}
				
				if(rightPos<temp.length){
					temp[rightPos] = edges.get(rightEdgePos);
					rightEdgePos--;
					rightPos++;
				}
				if(rightPos<temp.length){
					temp[rightPos] = edges.get(leftEdgePos);
					leftEdgePos++;
					rightPos++;
				}
				
			}
			
			List<TreeEdge> rearrangedNodes = new ArrayList<>(temp.length);
			Collections.addAll(rearrangedNodes,temp);
			node.setOutEdges(rearrangedNodes);
			
			for(TreeEdge edge : temp){
				rearrangeNode(edge.getTarget(),depth+1);
			}
		}
		
	}
	
	private void setNodeNumber(TreeNode node){
		if(node.getOutEdgeNumber()==0){
			node.setNodeNumber(1);
			return;
		}else{
			int sum = 1;
			for(TreeEdge edge : node.getOutEdges()){
				setNodeNumber(edge.getTarget());
				sum += edge.getTarget().getNodeNumber();
			}
			node.setNodeNumber(sum);
		}
	}

}

class TreeEdgeComperator implements Comparator<TreeEdge> {

	@Override
	public int compare(TreeEdge o1, TreeEdge o2) {
		int edgeNo1 = o1.getTarget().getNodeNumber();
		int edgeNo2 = o2.getTarget().getNodeNumber();
		if(edgeNo1<edgeNo2) return -1;
		else if(edgeNo1==edgeNo2) return 0;
		else return 1;
	}

	
}
