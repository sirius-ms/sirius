package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;

import java.util.regex.Pattern;

public class SiriusResultElement {
	
	private TreeNode tree; //zur Anzeige
	private FTree ft;      //Kais Datenstruktur, falls IO Klassen benötigt
	private int rank;
	private double score;
	private MolecularFormula mf;

	protected volatile FingerIdData fingerIdData;
	public volatile ComputingStatus fingerIdComputeState = ComputingStatus.UNCOMPUTED;

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

	private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");
	public String getFormulaAndIonText() {
		final PrecursorIonType ionType = ft.getAnnotationOrThrow(PrecursorIonType.class);
		String niceName = ionType.toString();
		niceName = pat.matcher(niceName).replaceAll("");
		if (ionType.isIonizationUnknown()) {
			return mf.toString();
		} else {
			return mf.toString() + " " + niceName;
		}
	}

	public int getCharge() {
		return ft.getAnnotationOrThrow(PrecursorIonType.class).getCharge();
	}

	public void setMolecularFormula(MolecularFormula mf) {
		this.mf = mf;
	}
	
	

}
