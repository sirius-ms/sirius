
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

@SuppressWarnings("serial")
public class AlignmentTreeEdge extends TreeEdge<AlignmentTreeEdge, AlignmentTreeNode> {
	public AlignmentTreeEdge(AlignmentTreeNode fromNode, AlignmentTreeNode toNode) {
		super(fromNode, toNode);
	}

	public AlignmentTreeEdge(AlignmentTreeNode fromNode, AlignmentTreeNode toNode, String label) {
		super(fromNode, toNode, label);
	}
	
	@Override
	public AlignmentTreeEdge clone() {
		AlignmentTreeEdge clonedAligTreeEdge = new AlignmentTreeEdge(fromNode, toNode);
		if (label != null) clonedAligTreeEdge.setLabel(new String(label));
		return clonedAligTreeEdge;
	}
	
	public void setContent(AlignmentTreeEdge edge) {
		super.setContent(edge);
	}
}
