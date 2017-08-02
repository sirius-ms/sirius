package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MinimalWidthGreedyTreePositionCalculator extends AbstractTreePositionCalculator {
	
	private HashMap<TreeNode,Integer> horizontalPosition;
	private HashMap<TreeNode,Integer> treeDepth;
	private HashMap<TreeNode,Integer> maxHorIndex;

	private ArrayDeque<TreeNode> tempDeque;
	
	public MinimalWidthGreedyTreePositionCalculator(){
		tempDeque = new ArrayDeque<TreeNode>();
		treeDepth = new HashMap<TreeNode, Integer>();
		horizontalPosition = new HashMap<TreeNode, Integer>();
		maxHorIndex = new HashMap<TreeNode, Integer>();
	}
	
	@Override
	protected void computeHorizontalPositions(TreeNode root) {
		horizontalPosition.clear();
		treeDepth.clear();
		tempDeque.clear();
		maxHorIndex.clear();
		calculatePosition(root);
		
		int minVal = Integer.MAX_VALUE;
		for(TreeNode node : horizontalPosition.keySet()) if(minVal > horizontalPosition.get(node)) minVal = horizontalPosition.get(node);
		
		for(TreeNode node : horizontalPosition.keySet()) node.setHorizontalPosition(horizontalPosition.get(node)-minVal);
		
	}
	
	private void calculatePosition(TreeNode node){
		if(node.getOutEdgeNumber()==0){
			horizontalPosition.put(node, 0);
			treeDepth.put(node,1);
			maxHorIndex.put(node, 0);
			return;
		}else{
			List<TreeEdge> edges = node.getOutEdges();
			for(TreeEdge edge : edges){
				calculatePosition(edge.getTarget());
			}
			
//			HashSet<TreeNode> nodes = new HashSet<TreeNode>();
//			for(TreeEdge edge : edges) nodes.add(edge.getTarget());
			
			if(edges.size()==1){
				TreeNode child = edges.get(0).getTarget();
				horizontalPosition.put(node, horizontalPosition.get(child));
				treeDepth.put(node, treeDepth.get(child)+1);
				maxHorIndex.put(node,maxHorIndex.get(child));
				return;
			}else if(edges.size()==2){ //einfach darüber anordnen
				
				TreeNode node0 = edges.get(0).getTarget();
				TreeNode node1 = edges.get(1).getTarget();
				
				int offset0 = computeOffset(node0,node1); //Ordnung beibehalten
				int offset1 = computeOffset(node1,node0); //Ordnung umkehren
	
				int maxRightSide0 = maxHorIndex.get(node1)+offset0;
				int maxRightSide1 = maxHorIndex.get(node0)+offset1;
				int maxLeftSide0 = maxHorIndex.get(node0);
				int maxLeftSide1 = maxHorIndex.get(node1);
				
				int diff0 = (horizontalPosition.get(node1)+offset0) - horizontalPosition.get(node0);
				int diff1 = (horizontalPosition.get(node0)+offset1) - horizontalPosition.get(node1);
				
				if(diff0==1)maxRightSide0++;
				if(diff1==1)maxRightSide1++;
				
				int maxIndex0 = Math.max(maxLeftSide0, maxRightSide0);
				int maxIndex1 = Math.max(maxLeftSide1, maxRightSide1);
				
				int newHorPosition;
				int newOffset;
				
				TreeNode leftNode = null;
				TreeNode rightNode = null;
				
				if(maxIndex0<=maxIndex1){
					leftNode  = node0;
					rightNode = node1;
					newOffset = offset0;
				}else{
					leftNode  = node1;
					rightNode = node0;
					newOffset = offset1;
				}
				
				int distance = (horizontalPosition.get(rightNode)+newOffset)-horizontalPosition.get(leftNode);
				if(distance==1) newOffset = newOffset+1;
				
				addOffset(rightNode, newOffset);
				
				newHorPosition = (horizontalPosition.get(leftNode)+horizontalPosition.get(rightNode))/2;
				
				horizontalPosition.put(node, newHorPosition);
				treeDepth.put(node,Math.max(treeDepth.get(node0),treeDepth.get(node1))+1);
				maxHorIndex.put(node, Math.max(maxHorIndex.get(leftNode), maxHorIndex.get(rightNode)));
				
			}else{
				
				LinkedHashSet<TreeNode> unclusteredNodes = new LinkedHashSet<TreeNode>();
				for(TreeEdge edge : edges) unclusteredNodes.add(edge.getTarget());
				
				int minHorSize = Integer.MAX_VALUE;
				int minOffset = Integer.MAX_VALUE;
				TreeNode leftNode = null, rightNode = null;
				
				LinkedList<TreeNode> newOrder = new LinkedList<TreeNode>();
				
				//bestimme die ersten zwei Nodes
				
				for(TreeNode leftNodeCand : unclusteredNodes){
					for(TreeNode rightNodeCand : unclusteredNodes){
						if(leftNodeCand==rightNodeCand) continue;
						int tempOffset = computeOffset(leftNodeCand, rightNodeCand);
						
						int tempSize = calculateUpperSize(unclusteredNodes, leftNodeCand, rightNodeCand, tempOffset);
						
						if(tempSize<minHorSize){
							minHorSize = tempSize;
							leftNode = leftNodeCand;
							rightNode = rightNodeCand;
							minOffset = tempOffset;
						}
						
//						int tempSize = tempOffset + maxHorIndex.get(rightNodeCand);
//						if(tempSize<minHorSize){
//							minHorSize = tempSize;
//							minOffset = tempOffset;
//							leftNode = leftNodeCand;
//							rightNode = rightNodeCand;
//						}
												
					}
				}
				
				newOrder.addFirst(leftNode);
				newOrder.addLast(rightNode);
				addOffset(rightNode, minOffset);
				unclusteredNodes.remove(leftNode);
				unclusteredNodes.remove(rightNode);
				
				while(!unclusteredNodes.isEmpty()){
					
//					int maxSize = calculateMaximalSize(unclusteredNodes,newOrder);
					
					TreeNode nextNode=null;
					int minOff = Integer.MAX_VALUE;
//					int minSize = Integer.MAX_VALUE;
					int minTreeSize = Integer.MAX_VALUE;
					
					for(TreeNode rightCand : unclusteredNodes){
						int tempOff = computeOffset(newOrder, rightCand);
						
						int newSize = calculateUpperSize(unclusteredNodes, newOrder, rightCand, tempOff);
						if(newSize<minTreeSize){
							minTreeSize = newSize;
							minOff = tempOff;
							nextNode = rightCand;
						}
						
//						int tempSize = tempOff + maxHorIndex.get(rightCand);
//						if(tempSize<minSize){
//							minOff = tempOff;
//							minSize = tempSize;
//							nextNode = rightCand;
//						}
					}
					
					newOrder.addLast(nextNode);
					addOffset(nextNode, minOff);
					unclusteredNodes.remove(nextNode);
				}
				
				int tD = 0;
				for(TreeNode childTree : newOrder) tD = Math.max(tD, treeDepth.get(childTree));
				 
				int newMiddle = (int) Math.round((horizontalPosition.get(newOrder.getFirst()) + horizontalPosition.get(newOrder.getLast())) / 2.0);
				horizontalPosition.put(node, newMiddle);
				treeDepth.put(node,tD+1);
				
				int maxHor = 0;
				for(TreeNode child : newOrder){
					maxHor = Math.max(maxHor, maxHorIndex.get(child));
				}
				
				maxHorIndex.put(node, maxHor);
				ArrayDeque<TreeNode> nodeStor = new ArrayDeque<TreeNode>();
				nodeStor.addFirst(node);
				while(!nodeStor.isEmpty()){
					TreeNode actNode = nodeStor.removeFirst();
					for(TreeEdge edge: actNode.getOutEdges()) nodeStor.addFirst(edge.getTarget());
				}
				
			}
			
		}
	}
	
//	private int calculateMaximalSize(Set<TreeNode> unclusteredNodes){
//		int sum = 0;
//		for(TreeNode node : unclusteredNodes){
//			sum += maxHorIndex.get(node)+1;
//		}
//		return sum;
//	}
	
	private int calculateUpperSize(Set<TreeNode> unclusteredNodes, List<TreeNode> newOrder, TreeNode newRight, int offset){
		int sum = 0;
		for(TreeNode node : unclusteredNodes){
			if(node==newRight)continue;
			sum += maxHorIndex.get(node)+1;
		}
		
		int rightVal = maxHorIndex.get(newRight)+1+offset;
		
		int temp = 0;
		for(TreeNode node : newOrder){
			temp = Math.max(temp,maxHorIndex.get(node)+1);
		}
		
		int sum2 = Math.max(temp, rightVal);
		
		return sum + sum2;
	}
	
	private int calculateUpperSize(Set<TreeNode> unclusteredNodes, TreeNode leftNode, TreeNode rightNode, int offset){
		int sum = 0;
		for(TreeNode node : unclusteredNodes){
			if(node==leftNode || node==rightNode)continue;
			sum += maxHorIndex.get(node)+1;
		}
		int sum2 = Math.max(maxHorIndex.get(leftNode),maxHorIndex.get(rightNode)+offset)+1;
		return sum + sum2;
	}
	
	private void addOffset(TreeNode node, int offset){
		tempDeque.clear();
		tempDeque.addFirst(node);
		while(!tempDeque.isEmpty()){
			TreeNode currNode = tempDeque.removeFirst();
			horizontalPosition.put(currNode,horizontalPosition.get(currNode)+offset);
			maxHorIndex.put(currNode,maxHorIndex.get(currNode)+offset);
			
			for(TreeEdge edge : currNode.getOutEdges())tempDeque.addFirst(edge.getTarget());
		}
	}
	
//	private int calculateWidth(TreeNode leftTree, TreeNode rightTree, int distVal){
//		return maxHorIndex.get(leftTree) + maxHorIndex(rightTree) - distVal;
//	}
	
	/**
	 * Berechne den Abstand zwischen zwei Baeumen
	 * @param leftNode
	 * @param rightNode
	 * @return
	 */
	private int computeOffset(TreeNode leftNode, TreeNode rightNode){
		
		int rightOffset = maxHorIndex.get(leftNode) + 1;
		
		int depth = Math.min(treeDepth.get(leftNode),treeDepth.get(rightNode));
		
		List<TreeNode> leftTree  = new ArrayList<TreeNode>();
		List<TreeNode> rightTree = new ArrayList<TreeNode>();
		
		int minDistance = Integer.MAX_VALUE;
		
		leftTree.add(leftNode);
		rightTree.add(rightNode);
		
		for(int i=0;i<depth;i++){
			
			int biggestLeftValue = 0;
			for(TreeNode node : leftTree) if(horizontalPosition.get(node)>biggestLeftValue) biggestLeftValue = horizontalPosition.get(node);
//			for(TreeNode node : leftTree) if(node.getHorizontalPosition()>biggestLeftValue) biggestLeftValue = node.getHorizontalPosition();
			
			int smallestRightValue = Integer.MAX_VALUE;
			for(TreeNode node : rightTree) if(horizontalPosition.get(node)<smallestRightValue) smallestRightValue = horizontalPosition.get(node);
//			for(TreeNode node : rightTree) if(node.getHorizontalPosition()<smallestRightValue) smallestRightValue = node.getHorizontalPosition();
			
			int diff = (smallestRightValue+rightOffset) - biggestLeftValue;
			if(diff<minDistance) minDistance = diff;
			
			List<TreeNode> newLeft = new ArrayList<TreeNode>();
			List<TreeNode> newRight = new ArrayList<TreeNode>();
			for(TreeNode node : leftTree) for(TreeEdge edge : node.getOutEdges()) newLeft.add(edge.getTarget());
			for(TreeNode node : rightTree) for(TreeEdge edge : node.getOutEdges()) newRight.add(edge.getTarget());
			leftTree = newLeft;
			rightTree = newRight;
			
		}
//		System.out.println(rightOffset +" "+ minDistance);
		
//		System.out.println(leftNode.getMolecularFormula()+" "+rightNode.getMolecularFormula()+" "+rightOffset+" "+(rightOffset - (minDistance-1)));
		
		return rightOffset - (minDistance-1);
		
	}
	
	private int computeOffset(LinkedList<TreeNode> leftNodes, TreeNode rightNode){
		
		int rightOffset = 0;
		for(TreeNode leftNode : leftNodes){
			rightOffset = Math.max(rightOffset, maxHorIndex.get(leftNode));
		}
		rightOffset++;
		
//		int rightOffset = maxHorIndex.get(leftNodes.getLast()) + 1;
		
		int leftDepth = 0;
		for(TreeNode node : leftNodes){
			leftDepth = Math.max(leftDepth, treeDepth.get(node));
		}
		
		int depth = Math.min(leftDepth,treeDepth.get(rightNode));
		
		List<TreeNode> leftTree  = new ArrayList<TreeNode>();
		List<TreeNode> rightTree = new ArrayList<TreeNode>();
		
		int minDistance = Integer.MAX_VALUE;
		
		leftTree.addAll(leftNodes);
		rightTree.add(rightNode);
		
		for(int i=0;i<depth;i++){
			
			int biggestLeftValue = 0;
			for(TreeNode node : leftTree) if(horizontalPosition.get(node)>biggestLeftValue) biggestLeftValue = horizontalPosition.get(node);
			
			int smallestRightValue = Integer.MAX_VALUE;
			for(TreeNode node : rightTree) if(horizontalPosition.get(node)<smallestRightValue) smallestRightValue = horizontalPosition.get(node);
			
			int diff = (smallestRightValue+rightOffset) - biggestLeftValue;
			if(diff<minDistance) minDistance = diff;
			
			List<TreeNode> newLeft = new ArrayList<TreeNode>();
			List<TreeNode> newRight = new ArrayList<TreeNode>();
			for(TreeNode node : leftTree) for(TreeEdge edge : node.getOutEdges()) newLeft.add(edge.getTarget());
			for(TreeNode node : rightTree) for(TreeEdge edge : node.getOutEdges()) newRight.add(edge.getTarget());
			leftTree = newLeft;
			rightTree = newRight;
			
		}
		
		return rightOffset - (minDistance-1);
		
	}
	
	@SuppressWarnings("unused")
	private static void printNodeStats(TreeNode node){
		System.out.println(node.getMolecularFormula()+" "+node.getHorizontalPosition()+" "+node.getVerticalPosition());
		for(TreeEdge edge : node.getOutEdges()){
			printNodeStats(edge.getTarget());
		}
	}
}
