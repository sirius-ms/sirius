
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
