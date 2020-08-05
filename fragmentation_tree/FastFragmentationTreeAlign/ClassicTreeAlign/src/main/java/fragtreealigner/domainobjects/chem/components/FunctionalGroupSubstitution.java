
package fragtreealigner.domainobjects.chem.components;

import java.io.Serializable;


@SuppressWarnings("serial")
public class FunctionalGroupSubstitution implements Serializable {
	private FunctionalGroup functionalGroupBefore;
	private FunctionalGroup functionalGroupAfter;
	private boolean doubleBondBefore;
	private boolean doubleBondAfter;
	
	public FunctionalGroupSubstitution(FunctionalGroup functionalGroupBefore, FunctionalGroup functionalGroupAfter, boolean doubleBondBefore, boolean doubleBondAfter) {
		super();
		this.functionalGroupBefore = functionalGroupBefore;
		this.functionalGroupAfter = functionalGroupAfter;
		this.doubleBondBefore = doubleBondBefore;
		this.doubleBondAfter = doubleBondAfter;
	}

	public FunctionalGroup getFunctionalGroupBefore() {
		return functionalGroupBefore;
	}
	
	public FunctionalGroup getFunctionalGroupAfter() {
		return functionalGroupAfter;
	}
	
	public String getFunctionalGroupBeforeAsString() {
		return functionalGroupBefore.getMolecularFormula().toString();
	}
	
	public String getFunctionalGroupAfterAsString() {
		return functionalGroupAfter.getMolecularFormula().toString();
	}	
	
	public boolean isDoubleBondBefore() {
		return doubleBondBefore;
	}
	
	public boolean isDoubleBondAfter() {
		return doubleBondAfter;
	}
}
