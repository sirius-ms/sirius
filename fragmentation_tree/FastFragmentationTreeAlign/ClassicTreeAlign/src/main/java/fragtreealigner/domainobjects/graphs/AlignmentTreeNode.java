
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

package fragtreealigner.domainobjects.graphs;

import fragtreealigner.domainobjects.chem.components.Compound;
import fragtreealigner.domainobjects.chem.components.NeutralLoss;

@SuppressWarnings("serial")
public class AlignmentTreeNode extends TreeNode<AlignmentTreeNode, AlignmentTreeEdge> {
	private NeutralLoss neutralLoss;
	private Compound compound;
	private double weight;
	
	public AlignmentTreeNode() {
		super();
	}
	
	public AlignmentTreeNode(String label) {
		super(label);
	}
	
	public AlignmentTreeNode(String label, NeutralLoss neutralLoss) {
		this(label);
		this.setNeutralLoss(neutralLoss);
	}

	public AlignmentTreeNode(String label, NeutralLoss neutralLoss, double weight) {
		this(label, neutralLoss);
		this.setWeight(weight);
	}

	@Override
	public void setContent(AlignmentTreeNode node) {
		super.setContent(node);
		this.neutralLoss = node.getNeutralLoss();
		this.weight = node.getWeight();
	}

	public void setNeutralLoss(NeutralLoss neutralLoss) {
		this.neutralLoss = neutralLoss;
	}

	public NeutralLoss getNeutralLoss() {
		return neutralLoss;
	}

	public void setCompound(Compound compound) {
		this.compound = compound;
	}

	public Compound getCompound() {
		return compound;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		if (neutralLoss == null || compound == null) return getLabel();
		else return (getLabel() + "["+ compound.getMolecularFormula()+ " " + neutralLoss.getMolecularFormula() + "]");
	}
	
	@Override
	public AlignmentTreeNode clone() {
		AlignmentTreeNode clonedAligTreeNode = new AlignmentTreeNode(new String(label));
		if (neutralLoss != null) clonedAligTreeNode.setNeutralLoss(neutralLoss.clone());
		return clonedAligTreeNode;
	}
}
