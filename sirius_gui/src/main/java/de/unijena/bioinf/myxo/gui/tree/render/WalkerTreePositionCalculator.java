/*
 * Code basiert auf abego treelayout. BSD ... Ist wiederum eine Implementierung für den verbesserten Walker...
 * 
 */


package de.unijena.bioinf.myxo.gui.tree.render;

import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WalkerTreePositionCalculator extends AbstractTreePositionCalculator {
	
	private HashMap<TreeNode, Double> mod;
	private HashMap<TreeNode, Double> prelim;
	private HashMap<TreeNode, Double> change;
	private HashMap<TreeNode, Double> shift;
	private HashMap<TreeNode, TreeNode> thread;
	private HashMap<TreeNode, TreeNode> ancestor;
	private HashMap<TreeNode, Integer> number;
	private HashMap<TreeNode, Double> relHorPositions;
	
	public WalkerTreePositionCalculator(){
		mod = new HashMap<TreeNode, Double>();
		prelim = new HashMap<TreeNode, Double>();
		change = new HashMap<TreeNode, Double>();
		shift = new HashMap<TreeNode, Double>();
		thread = new HashMap<TreeNode, TreeNode>();
		ancestor = new HashMap<TreeNode, TreeNode>();
		number = new HashMap<TreeNode, Integer>();
		relHorPositions = new HashMap<TreeNode, Double>();
	}
	
	private double getMod(TreeNode node) {
		Double d = mod.get(node);
		return d != null ? d.doubleValue() : 0;
	}

	private void setMod(TreeNode node, double d) {
		mod.put(node, d);
	}
	
	private double getPrelim(TreeNode node) {
		Double d = prelim.get(node);
		return d != null ? d.doubleValue() : 0;
	}

	private void setPrelim(TreeNode node, double d) {
		prelim.put(node, d);
	}
	
	private double getChange(TreeNode node) {
		Double d = change.get(node);
		return d != null ? d.doubleValue() : 0;
	}

	private void setChange(TreeNode node, double d) {
		change.put(node, d);
	}

	private double getShift(TreeNode node) {
		Double d = shift.get(node);
		return d != null ? d.doubleValue() : 0;
	}
	
	private void setAncestor(TreeNode node, TreeNode ancestor) {
		this.ancestor.put(node, ancestor);
	}
	
	private TreeNode getAncestor(TreeNode node) {
		TreeNode n = ancestor.get(node);
		return n != null ? n : node;
	}

	private void setShift(TreeNode node, double d) {
		shift.put(node, d);
	}
	
	private void setThread(TreeNode node, TreeNode thread) {
		this.thread.put(node, thread);
	}
	
	private TreeNode getThread(TreeNode node) {
		TreeNode n = thread.get(node);
		return n != null ? n : null;
	}
	
	private TreeNode nextLeft(TreeNode v) {
		return v.getOutEdgeNumber()==0 ? getThread(v) : v.getOutEdge(0).getTarget();
	}

	private TreeNode nextRight(TreeNode v) {
		return v.getOutEdgeNumber()==0 ? getThread(v) : v.getOutEdge(v.getOutEdgeNumber()-1).getTarget();
	}
	
	private void moveSubtree(TreeNode wMinus, TreeNode wPlus, TreeNode parent, double shift) {

		int subtrees = getNumber(wPlus, parent) - getNumber(wMinus, parent);
		setChange(wPlus, getChange(wPlus) - shift / subtrees);
		setShift(wPlus, getShift(wPlus) + shift);
		setChange(wMinus, getChange(wMinus) + shift / subtrees);
		setPrelim(wPlus, getPrelim(wPlus) + shift);
		setMod(wPlus, getMod(wPlus) + shift);
	}
	
	private int getNumber(TreeNode node, TreeNode parentNode) {
		Integer n = number.get(node);
		if (n == null) {
			int i = 1;
			for(TreeEdge edge : parentNode.getOutEdges()){
				TreeNode child = edge.getTarget();
				number.put(child, i++);
			}
			n = number.get(node);
		}

		return n.intValue();
	}

	@Override
	protected void computeHorizontalPositions(TreeNode root) {
		mod.clear();
		prelim.clear();
		change.clear();
		shift.clear();
		thread.clear();
		ancestor.clear();
		number.clear();
		relHorPositions.clear();
		
		FirstEdgeRearrangement er = new FirstEdgeRearrangement();
		er.rearrangeTreeNodes(root);
		
		firstWalk(root, null);
		secondWalk(root, -getPrelim(root));
		
		setIntegerPositions();
		
//		shiftValues(root);
//		makeDiscrete(root);
////		
//		reduceEdgeLength(root);
//		shiftValues(root);
		
//		normalizeHorizontalPositions(root);
	}
	
	private void setIntegerPositions(){
		
		List<Double> positions = new ArrayList<Double>(relHorPositions.size());
		double minPos = Double.POSITIVE_INFINITY;
		
		for(TreeNode node : relHorPositions.keySet()){
			Double d = relHorPositions.get(node);
			if(d<minPos)minPos = d;
		}
		
		System.out.println("minPos: "+minPos);
		
		for(TreeNode node : relHorPositions.keySet()){
			Double d = relHorPositions.get(node);
			positions.add(d-minPos);
		}
		
		Collections.sort(positions);
		
		double minDiff = Double.POSITIVE_INFINITY;
		
		for(int i=0;i<positions.size()-1;i++){
			double val1 = positions.get(i);
			double val2 = positions.get(i+1);
			if(val1==val2)continue;
			double diff = val2 - val1;
			if(minDiff>diff) minDiff = diff;
		}
		
		System.out.println("minDiff: "+minDiff);
		
		for(TreeNode node : relHorPositions.keySet()){
			double doubleVal = relHorPositions.get(node);
			int intVal = (int) Math.round((doubleVal-minPos) / minDiff);
			node.setHorizontalPosition(intVal);
			System.out.println(node.getMolecularFormula()+" "+node.getHorizontalPosition()+" "+node.getVerticalPosition());
//			System.out.println(node.getMolecularFormula()+" "+((doubleVal-minPos)/minDiff) +" "+intVal);
//			System.out.println("b "+node.getMolecularFormula()+" "+doubleVal);
		}
		
	}
	
//	private void shiftValues(TreeNode root){
//		
//		double minVal = Double.POSITIVE_INFINITY;
//		
//		ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode n = nodes.removeFirst();
//
//			if(n.getHorizontalPosition()<minVal) minVal = n.getHorizontalPosition();
//			
//			if(n.getOutEdgeNumber()>0) for(TreeEdge edge : n.getOutEdges()) nodes.addFirst(edge.getTarget());
//		}
//		
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode n = nodes.removeFirst();
//			
//			n.setHorizontalPosition(n.getHorizontalPosition()-minVal);
//			
//			if(n.getOutEdgeNumber()>0) for(TreeEdge edge : n.getOutEdges()) nodes.addFirst(edge.getTarget());
//		}
//		
//	}
//	
//	private void makeDiscrete(TreeNode root){
//		
//		List<Double> vals = new ArrayList<Double>();
//		
//		ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode n = nodes.removeFirst();
//
//			vals.add(n.getHorizontalPosition());
//			
//			if(n.getOutEdgeNumber()>0) for(TreeEdge edge : n.getOutEdges()) nodes.addFirst(edge.getTarget());
//		}
//		
//		Collections.sort(vals);
//		
//		double minDiff = Double.POSITIVE_INFINITY;
//		for(int i=0;i<vals.size()-1;i++){
//			double val1 = vals.get(i);
//			double val2 = vals.get(i+1);
//			if(val1==val2) continue;
//			double diff = val2 - val1;
//			if(diff<minDiff) minDiff = diff;
//		}
//		
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode n = nodes.removeFirst();
//			n.setHorizontalPosition(Math.round(n.getHorizontalPosition()/minDiff));
//			
//			if(n.getOutEdgeNumber()>0) for(TreeEdge edge : n.getOutEdges()) nodes.addFirst(edge.getTarget());
//		}
//		
//	}
//	
//	private void reduceEdgeLength(TreeNode node){
//		
//		if(node.getOutEdgeNumber()==0) return;
//		
//		for(TreeEdge edge : node.getOutEdges()){
//			reduceEdgeLength(edge.getTarget());
//		}
//		
//		List<TreeEdge> edges = node.getOutEdges();
//		
//		if(edges.size()==1) return;
//		
//		for(int i=edges.size()-1;i>0;i--){
//			
//			double minDistance = Double.POSITIVE_INFINITY;
//			
//			for(int j=0;j<i;j++){  //jeder der linken Nachbarn kann kollidieren
//				
//				for(int k=i;k<edges.size();k++){
//					TreeNode leftSubTree   = edges.get(j).getTarget();
//					TreeNode rightSubTree  = edges.get(k).getTarget();
//					
//					while(true){
//						double leftHorPos  = leftSubTree.getHorizontalPosition();
//						double rightHorPos = rightSubTree.getHorizontalPosition();
//						double diff = rightHorPos-leftHorPos;
//						if(diff<minDistance) minDistance=diff;
//						
//						if(leftSubTree.getOutEdgeNumber()==0 || rightSubTree.getOutEdgeNumber()==0) break;
//						
//						leftSubTree  = leftSubTree.getOutEdge(leftSubTree.getOutEdgeNumber()-1).getTarget();
//						rightSubTree = rightSubTree.getOutEdge(0).getTarget();
//						
//					}
//					
//				}
//				
//			}
//			
//			System.out.println(node.getMolecularFormula()+" - "+minDistance);
//			
//			if(minDistance>1){ // Verschiebung ist möglich
//				
//				double shift = - (minDistance-1);
//				ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
//				
//				for(int j=i;j<edges.size();j++){
//					
//					nodes.addFirst(edges.get(i).getTarget());
//					while(!nodes.isEmpty()){
//						TreeNode n = nodes.removeFirst();
//						n.setHorizontalPosition(n.getHorizontalPosition() + shift);
//						if(n.getOutEdgeNumber()>0){
//							for(TreeEdge edge : n.getOutEdges()) nodes.addFirst(edge.getTarget());
//						}
//					}
//					
//				}
//			}
//			
//			
//		}
//		
//		
//	}
	
	
	
//	private void normalizeHorizontalPositions(TreeNode root){
//		ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
//		double minPosition = Double.MAX_VALUE;
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode node = nodes.remove();
//			if(node.getHorizontalPosition()<minPosition) minPosition=node.getHorizontalPosition();
//			for(TreeEdge edge : node.getOutEdges()){
//				nodes.addFirst(edge.getTarget());
//			}
//		}
//		
////		System.out.println("minimalPosition: "+minPosition);
//		
//		nodes.addFirst(root);
//		double maxValue = Double.NEGATIVE_INFINITY;
//		while(!nodes.isEmpty()){
//			TreeNode node = nodes.remove();
//			
//			double newValue = node.getHorizontalPosition() - minPosition;
//			if(newValue>maxValue) maxValue = newValue;
//			node.setHorizontalPosition(newValue);
//			
//			for(TreeEdge edge : node.getOutEdges()){
//				nodes.addFirst(edge.getTarget());
//			}
//		}
//		
////		System.out.println("neuer Maximalwert: "+maxValue);
//		
//		nodes.addFirst(root);
//		while(!nodes.isEmpty()){
//			TreeNode node = nodes.remove();
////			System.out.println("Wert vor:  "+node.getHorizontalPosition());
//			node.setHorizontalPosition(Math.min(Math.max(node.getHorizontalPosition()/maxValue,0),1) ); //Rundungsfehler...
////			System.out.println("Wert nach: "+node.getHorizontalPosition());
//			
//			for(TreeEdge edge : node.getOutEdges()){
//				nodes.addFirst(edge.getTarget());
//			}
//		}
//	}
	
	/**
	 * 
	 * @param vIMinus
	 * @param v
	 * @param parentOfV
	 * @param defaultAncestor
	 * @return the greatest distinct ancestor of vIMinus and its right neighbor
	 *         v
	 */
	private TreeNode ancestor(TreeNode vIMinus, TreeNode parentOfV, TreeNode defaultAncestor) {
		TreeNode ancestor = getAncestor(vIMinus);

		// when the ancestor of vIMinus is a sibling of v (i.e. has the same
		// parent as v) it is also the greatest distinct ancestor vIMinus and
		// v. Otherwise it is the defaultAncestor

		for(TreeEdge edge : parentOfV.getOutEdges()){
			if(edge.getTarget()==ancestor) return ancestor;
		}
		return defaultAncestor;
		
//		return tree.isChildOfParent(ancestor, parentOfV) ? ancestor
//				: defaultAncestor;
	}
	
	private TreeNode apportion(TreeNode v, TreeNode defaultAncestor, TreeNode leftSibling, TreeNode parentOfV) {
		
		TreeNode w = leftSibling;
		if (w == null) {
			// v has no left sibling
			return defaultAncestor;
		}
		// v has left sibling w

		// The following variables "v..." are used to traverse the contours to
		// the subtrees. "Minus" refers to the left, "Plus" to the right
		// subtree. "I" refers to the "inside" and "O" to the outside contour.
		TreeNode vOPlus = v;
		TreeNode vIPlus = v;
		TreeNode vIMinus = w;
		// get leftmost sibling of vIPlus, i.e. get the leftmost sibling of
		// v, i.e. the leftmost child of the parent of v (which is passed
		// in)
		TreeNode vOMinus = parentOfV.getOutEdge(0).getTarget();

		Double sIPlus = getMod(vIPlus);
		Double sOPlus = getMod(vOPlus);
		Double sIMinus = getMod(vIMinus);
		Double sOMinus = getMod(vOMinus);

		TreeNode nextRightVIMinus = nextRight(vIMinus);
		TreeNode nextLeftVIPlus = nextLeft(vIPlus);

		while (nextRightVIMinus != null && nextLeftVIPlus != null) {
			vIMinus = nextRightVIMinus;
			vIPlus = nextLeftVIPlus;
			vOMinus = nextLeft(vOMinus);
			vOPlus = nextRight(vOPlus);
			setAncestor(vOPlus, v);
			double shift = (getPrelim(vIMinus) + sIMinus) - (getPrelim(vIPlus) + sIPlus) + 1;

			if (shift > 0) {
				moveSubtree(ancestor(vIMinus, parentOfV, defaultAncestor),
						v, parentOfV, shift);
				sIPlus = sIPlus + shift;
				sOPlus = sOPlus + shift;
			}
			sIMinus = sIMinus + getMod(vIMinus);
			sIPlus = sIPlus + getMod(vIPlus);
			sOMinus = sOMinus + getMod(vOMinus);
			sOPlus = sOPlus + getMod(vOPlus);

			nextRightVIMinus = nextRight(vIMinus);
			nextLeftVIPlus = nextLeft(vIPlus);
		}

		if (nextRightVIMinus != null && nextRight(vOPlus) == null) {
			setThread(vOPlus, nextRightVIMinus);
			setMod(vOPlus, getMod(vOPlus) + sIMinus - sOPlus);
		}

		if (nextLeftVIPlus != null && nextLeft(vOMinus) == null) {
			setThread(vOMinus, nextLeftVIPlus);
			setMod(vOMinus, getMod(vOMinus) + sIPlus - sOMinus);
			defaultAncestor = v;
		}
		return defaultAncestor;
	}	
	
	private void firstWalk(TreeNode v, TreeNode leftSibling) {
		if (v.getOutEdgeNumber()==0) {
			// No need to set prelim(v) to 0 as the getter takes care of this.

			TreeNode w = leftSibling;
			if (w != null) {
				// v has left sibling

				setPrelim(v, getPrelim(w) + 1);
			}

		} else {
			// v is not a leaf

			TreeNode defaultAncestor = v.getOutEdge(0).getTarget();
			TreeNode previousChild = null;
			for(int i=0;i<v.getOutEdgeNumber();i++){
				TreeNode w = v.getOutEdge(i).getTarget();
				firstWalk(w, previousChild);
				defaultAncestor = apportion(w, defaultAncestor, previousChild,v);
				previousChild = w;
			}
			executeShifts(v);
			
			List<TreeEdge> edges = v.getOutEdges();
			TreeNode firstChild = edges.get(0).getTarget();
			TreeNode lastChild  = edges.get(edges.size()-1).getTarget();
			
			double midpoint = (getPrelim(firstChild) + getPrelim(lastChild)) / 2.0;
			if(midpoint!=(int)midpoint){
				
				if(edges.size()==2 && 1 == prelim.get(lastChild)-prelim.get(firstChild)){
					double prelim = getPrelim(lastChild);
					prelim++;
					setPrelim(lastChild, prelim);
					
					midpoint = (getPrelim(firstChild) + prelim) / 2.0;
					
					ArrayDeque<TreeNode> nodes = new ArrayDeque<TreeNode>();
					for(TreeEdge edge : lastChild.getOutEdges()){
						nodes.addFirst(edge.getTarget());
					}
					while(!nodes.isEmpty()){
						TreeNode node = nodes.removeFirst();
						for(TreeEdge edge : node.getOutEdges()){
							nodes.addFirst(edge.getTarget());
						}
						setPrelim(node, getPrelim(node)+1);
					}
				}else{
					midpoint = Math.round(midpoint);
				}
				
//				double prelim = getPrelim(v.getOutEdge(v.getOutEdgeNumber()-1).getTarget());
//				prelim++;
//				setPrelim(v.getOutEdge(v.getOutEdgeNumber()-1).getTarget(), prelim);
				
				
			}
			
			TreeNode w = leftSibling;
			if (w != null) {
				// v has left sibling

				setPrelim(v, getPrelim(w) + 1);
				setMod(v, getPrelim(v) - midpoint);

			} else {
				// v has no left sibling

				setPrelim(v, midpoint);
			}
		}
	}	
	
	private void executeShifts(TreeNode v) {
		double shift = 0;
		double change = 0;
		
		for(int i=v.getOutEdgeNumber()-1;i>=0;i--){
			TreeNode w = v.getOutEdge(i).getTarget();
			
			change = change + getChange(w);
			setPrelim(w, getPrelim(w) + shift);
			setMod(w, getMod(w) + shift);
			shift = shift + getShift(w) + change;
		}
	}
	
	
	private void secondWalk(TreeNode v, double m) {
		// construct the position from the prelim and the level information

		double x = getPrelim(v) + m;
		
		relHorPositions.put(v, x);
//		v.setHorizontalPosition(x);

		// recurse
		if(v.getOutEdgeNumber()>0) {
			for(int i=0;i<v.getOutEdgeNumber();i++){
				TreeNode w = v.getOutEdge(i).getTarget();
				secondWalk(w, m + getMod(v));
			}
		}
	}
	
	private static void printNodeStats(TreeNode node){
		System.out.println(node.getMolecularFormula()+" "+node.getHorizontalPosition()+" "+node.getVerticalPosition());
		for(TreeEdge edge : node.getOutEdges()){
			printNodeStats(edge.getTarget());
		}
	}
}
