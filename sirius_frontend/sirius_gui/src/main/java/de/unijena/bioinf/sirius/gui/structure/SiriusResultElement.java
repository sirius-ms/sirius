package de.unijena.bioinf.sirius.gui.structure;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdData;
import org.jdesktop.beans.AbstractBean;

import java.util.regex.Pattern;

public class SiriusResultElement extends AbstractBean implements Comparable<SiriusResultElement> {
	
	private boolean bestHit = false;
	private TreeNode tree; //zur Anzeige
	private IdentificationResult resultElement;

	protected volatile FingerIdData fingerIdData;
	public volatile ComputingStatus fingerIdComputeState = ComputingStatus.UNCOMPUTED;

	public SiriusResultElement(IdentificationResult result) {
		this.tree = null;
		this.resultElement = result;
		this.fingerIdData = null;
	}

	public IdentificationResult getResult() {
        return resultElement;
    }

	public void buildTreeVisualization(Function<FTree, TreeNode> builder) {
        this.tree = builder.apply(resultElement.getResolvedTree());
    }

    public FingerIdData getFingerIdData() {
        return fingerIdData;
    }

    public void setFingerIdData(FingerIdData fingerIdData) {
        this.fingerIdData = fingerIdData;
    }

    public int getRank() {
		return resultElement.getRank();
	}

	public double getScore() {
		return resultElement.getScore();
	}

	public MolecularFormula getMolecularFormula() {
		return resultElement.getMolecularFormula();
	}

	private final static Pattern pat = Pattern.compile("^\\s*\\[\\s*M\\s*|\\s*\\]\\s*\\d*\\s*[\\+\\-]\\s*$");
	public String getFormulaAndIonText() {
		final PrecursorIonType ionType = resultElement.getRawTree().getAnnotationOrThrow(PrecursorIonType.class);
		final MolecularFormula mf = resultElement.getMolecularFormula();
		String niceName = ionType.toString();
		niceName = pat.matcher(niceName).replaceAll("");
		if (ionType.isIonizationUnknown()) {
			return mf.toString();
		} else {
			return mf.toString() + " " + niceName;
		}
	}

	public int getCharge() {
		return resultElement.getResolvedTree().getAnnotationOrThrow(PrecursorIonType.class).getCharge();
	}

    public TreeNode getTreeVisualization() {
        return tree;
    }

	public boolean isBestHit() {
		return bestHit;
	}

	public void setBestHit(boolean bestHit) {
		boolean old = this.bestHit;
		this.bestHit = bestHit;
		firePropertyChange("best_hit",old, this.bestHit);
	}

	@Override
	public int compareTo(SiriusResultElement o) {
		return Integer.compare(getRank(),o.getRank());
	}
}
