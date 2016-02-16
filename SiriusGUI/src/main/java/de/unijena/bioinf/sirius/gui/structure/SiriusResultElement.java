package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;

public class SiriusResultElement {
	
	private TreeNode tree; //zur Anzeige
	private FTree ft;      //Kais Datenstruktur, falls IO Klassen benötigt
	private int rank;
	private double score;
	private MolecularFormula mf;

	protected FingerIdData fingerIdData;

	public SiriusResultElement() {
		this.tree = null;
		this.rank = Integer.MAX_VALUE;
		this.score = Double.NEGATIVE_INFINITY;
		this.mf = null;
		this.ft = null;
	}

    public FingerIdData getFingerIdData() {
        return fingerIdData;
    }

    public void setFingerIdData(FingerIdData fingerIdData) {
        this.fingerIdData = fingerIdData;
    }

    public TreeNode getTree() {
		return tree;
	}

	public void setTree(TreeNode tree) {
		this.tree = tree;
	}
	
	public FTree getRawTree() {
		return ft;
	}

	public void setRawTree(FTree ft) {
		this.ft = ft;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public MolecularFormula getMolecularFormula() {
		return mf;
	}

	public void setMolecularFormula(MolecularFormula mf) {
		this.mf = mf;
	}
	
	

}
