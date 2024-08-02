
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

package fragtreealigner.domainobjects.db;

import fragtreealigner.domainobjects.graphs.AlignmentTree;
import fragtreealigner.domainobjects.graphs.FragmentationTree;

import java.io.Serializable;

@SuppressWarnings("serial")
public class FragmentationTreeDatabaseEntry implements Serializable {
	private String filename;
	private FragmentationTree fragmentationTree;
	private AlignmentTree alignmentTree;
	private AlignmentTree decoyAlignmentTree;

	public FragmentationTreeDatabaseEntry(String filename, FragmentationTree fragmentationTree, AlignmentTree alignmentTree) {
		super();
		this.filename = filename;
		this.fragmentationTree = fragmentationTree;
		this.alignmentTree = alignmentTree;
	}

	public FragmentationTreeDatabaseEntry(String filename, FragmentationTree fragmentationTree, AlignmentTree alignmentTree, AlignmentTree decoyAlignmentTree) {
		this(filename, fragmentationTree, alignmentTree);
		this.decoyAlignmentTree = decoyAlignmentTree;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public FragmentationTree getFragmentationTree() {
		return fragmentationTree;
	}

	public void setFragmentationTree(FragmentationTree fragmentationTree) {
		this.fragmentationTree = fragmentationTree;
	}

	public AlignmentTree getAlignmentTree() {
		return alignmentTree;
	}

	public void setAlignmentTree(AlignmentTree alignmentTree) {
		this.alignmentTree = alignmentTree;
	}

	public AlignmentTree getDecoyAlignmentTree() {
		return decoyAlignmentTree;
	}

	public void setDecoyAlignmentTree(AlignmentTree decoyAlignmentTree) {
		this.decoyAlignmentTree = decoyAlignmentTree;
	}
}
