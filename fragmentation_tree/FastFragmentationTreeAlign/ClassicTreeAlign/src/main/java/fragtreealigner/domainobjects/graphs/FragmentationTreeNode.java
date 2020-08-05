
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

@SuppressWarnings("serial")
public class FragmentationTreeNode extends TreeNode<FragmentationTreeNode, FragmentationTreeEdge> {
	private Compound compound;
	private float doubleBondEquivalent;
	private double deviation;
	private String collisionEnergy;
	private double intensity;

	public FragmentationTreeNode() {
		super();
	}
	
	public FragmentationTreeNode(String label) {
		this();
		this.label = label;
	}
	
	public FragmentationTreeNode(String label, Compound compound, float doubleBondEquivalent, double deviation, String collisionEnergy) {
		this();
		this.label = label;
		this.compound = compound;
		this.doubleBondEquivalent = doubleBondEquivalent;
		this.deviation = deviation;
		this.collisionEnergy = collisionEnergy;
	}
	
	@Override
	public void setContent(FragmentationTreeNode node) {
		super.setContent(node);
		this.compound = node.getCompound();
		this.doubleBondEquivalent = node.getDoubleBondEquivalent();
		this.deviation = node.getDeviation();
		this.collisionEnergy = node.getCollisionEnergy();
		this.intensity = node.getIntensity();
	}

	
	public void setCompound(Compound compound) {
		this.compound = compound;
	}

	public Compound getCompound() {
		return compound;
	}

	public void setDoubleBondEquivalent(float doubleBondEquivalent) {
		this.doubleBondEquivalent = doubleBondEquivalent;
	}
	
	public float getDoubleBondEquivalent() {
		return doubleBondEquivalent;
	}
	
	public void setDeviation(double deviation) {
		this.deviation = deviation;
	}
	
	public double getDeviation() {
		return deviation;
	}
	
	public void setCollisionEnergy(String collisionEnergy) {
		this.collisionEnergy = collisionEnergy;
	}
	
	public String getCollisionEnergy() {
		return collisionEnergy;
	}

	public double getIntensity() {
		return intensity;
	}

	public void setIntensity(double intensity) {
		this.intensity = intensity;
	}

	public String getCmlId() {
		return "ion" + getId() + "_" + getCompound().getMolecularFormula();
	}
	
	@Override
	public String toString() {
		String str = getLabel() + "[DBE:" + doubleBondEquivalent + ",Dev:" + deviation + ",CE:" + collisionEnergy;
		if (compound != null) str += ",Comp:" + compound.getName();
		str += "]";
		return str;
	}
	
	@Override
	public FragmentationTreeNode clone() {
		FragmentationTreeNode clonedFragTreeNode = new FragmentationTreeNode(new String(label));
		if (compound != null) clonedFragTreeNode.setCompound(compound.clone());
		if (collisionEnergy != null) clonedFragTreeNode.setCollisionEnergy(new String(collisionEnergy));
		clonedFragTreeNode.setDoubleBondEquivalent(doubleBondEquivalent);
		clonedFragTreeNode.setDeviation(deviation);
		clonedFragTreeNode.setIntensity(intensity);
		
		return clonedFragTreeNode;
	}

//	public void removeInEdge(FragmentationTreeEdge e) {
//		inEdges.remove(e);		
//	}
//
//	public void removeOutEdge(FragmentationTreeEdge e) {
//		outEdges.remove(e);		
//	}
}
