
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package fragtreealigner.ui;

import att.grappa.*;
import fragtreealigner.domainobjects.Alignment;
import fragtreealigner.domainobjects.chem.structure.MolecularStructureAtom;
import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("serial")
public class GraphPanelListener extends GrappaAdapter implements Serializable {
	private GrappaPanel relatedGraphPanel;
	private GrappaPanel structureGraphPanel;
	private Alignment alignment;
	private int treeId;
	
	public GraphPanelListener(int treeId) {
		this.treeId = treeId;
	}
	
	public GraphPanelListener(int treeId, GrappaPanel relatedGraphPanel, GrappaPanel structureGraphPanel) {
		this(treeId);
		this.relatedGraphPanel = relatedGraphPanel;
		this.structureGraphPanel = structureGraphPanel;
	}

	public void setAlignment(Alignment alignment) {
		this.alignment = alignment;
	}
	
	@Override
	public void grappaClicked(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, int clickCount, GrappaPanel panel) {
		if (elem == null) return;
		super.grappaClicked(subg, elem, pt, modifiers, clickCount, panel);
		selectRelatedNodes(subg);
	} 
	
	@Override
	public void grappaReleased(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, Element pressedElem, GrappaPoint pressedPt, int pressedModifiers, GrappaBox outline, GrappaPanel panel) {
		super.grappaReleased(subg, elem, pt, modifiers, pressedElem, pressedPt, pressedModifiers, outline, panel);
		selectRelatedNodes(subg);
	}
	
	@SuppressWarnings("unchecked")
	private void selectRelatedNodes(Subgraph subg) {
		if (subg.currentSelection == null) return;
		
		List<String> nodeIds;
		Vector<Element> relatedSelection = new Vector<Element>();
		Vector<Element> selection = new Vector<Element>();
		Element relatedElement;
		Subgraph relatedSubgraph = relatedGraphPanel.getSubgraph();
		
		if (relatedSubgraph.currentSelection != null) {
			if (relatedSubgraph.currentSelection instanceof Element) {
				((Element)(relatedSubgraph.currentSelection)).highlight &= ~HIGHLIGHT_MASK;
			} else {
				Vector<Element> vec = ((Vector<Element>)(relatedSubgraph.currentSelection));
				for (int i = 0; i < vec.size(); i++) {
					vec.elementAt(i).highlight &= ~HIGHLIGHT_MASK;
				}
			}
		}
		
		if (subg.currentSelection instanceof Element) {
			selection.add((Element)(subg.currentSelection));
		} else {
			selection = ((Vector<Element>)(subg.currentSelection));
		}

		for (Element elem : selection) {
			if (treeId == 1) {
				nodeIds = alignment.getRelatedNodesInTree2(elem.getName().substring(1));
				if (nodeIds != null) {
					for (String nodeId : nodeIds) {
						relatedElement = relatedGraphPanel.getSubgraph().findNodeByName("b" + nodeId);
						relatedSelection.add(relatedElement);
						relatedElement.highlight |= SELECTION_MASK;
					}
				}
			} else {
				nodeIds = alignment.getRelatedNodesInTree1(elem.getName().substring(1));
				if (nodeIds != null) {
					for (String nodeId : nodeIds) {
						relatedElement = relatedGraphPanel.getSubgraph().findNodeByName("a" + nodeId);
						relatedSelection.add(relatedElement);
						relatedElement.highlight |= SELECTION_MASK;
					}
				}
			}
		}
		relatedGraphPanel.getSubgraph().currentSelection = relatedSelection;
		relatedGraphPanel.repaint();
		
		selectStructureNodes(subg);
	}

	@SuppressWarnings("unchecked")
	private void selectStructureNodes(Subgraph subg) {
		if (treeId == 1) subg = relatedGraphPanel.getSubgraph();
		if (subg.currentSelection == null) return;
		
		AlignmentTreeNode aligTreeNode;
		Vector<Element> relatedSelection = new Vector<Element>();
		Vector<Element> selection = new Vector<Element>();
		Element relatedElement;
		Subgraph structureSubgraph = structureGraphPanel.getSubgraph();
		
		if (structureSubgraph.currentSelection != null) {
			if (structureSubgraph.currentSelection instanceof Element) {
				((Element)(structureSubgraph.currentSelection)).highlight &= ~HIGHLIGHT_MASK;
			} else {
				Vector<Element> vec = ((Vector<Element>)(structureSubgraph.currentSelection));
				for (int i = 0; i < vec.size(); i++) {
					vec.elementAt(i).highlight &= ~HIGHLIGHT_MASK;
				}
			}
		}
		
		if (subg.currentSelection instanceof Element) {
			selection.add((Element)(subg.currentSelection));
		} else {
			selection = ((Vector<Element>)(subg.currentSelection));
		}

		for (Element elem : selection) {
			aligTreeNode = alignment.getTree2().getNodeByName(elem.getName().substring(1));
			if (aligTreeNode == null || aligTreeNode.getNeutralLoss() == null || aligTreeNode.getNeutralLoss().getMolecularStructure() == null) continue;
			for (MolecularStructureAtom atom : aligTreeNode.getNeutralLoss().getMolecularStructure().getNodes()) {
				relatedElement = structureSubgraph.findNodeByName(atom.getLabel());
				relatedSelection.add(relatedElement);
				relatedElement.highlight |= SELECTION_MASK;
			}
		}
		structureSubgraph.currentSelection = relatedSelection;
		structureGraphPanel.repaint();
	}
}
