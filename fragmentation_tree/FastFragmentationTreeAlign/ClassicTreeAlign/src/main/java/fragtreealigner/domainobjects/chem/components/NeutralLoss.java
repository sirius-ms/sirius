
package fragtreealigner.domainobjects.chem.components;

import fragtreealigner.domainobjects.chem.basics.MolecularFormula;
import fragtreealigner.util.Session;

import java.util.HashMap;

@SuppressWarnings("serial")
public class NeutralLoss extends ChemicalComponent {
	private boolean isCommon;
	private HashMap<String, Integer> functionalGroups;

	public NeutralLoss(String name, double mass, Session session) {
		super(name, mass, session);
		this.molecularFormula = new MolecularFormula(name, session);
	}	

	public NeutralLoss(String name, double mass, MolecularFormula molecularFormula, boolean analysis, Session session) {
		super(name, mass, molecularFormula, session);
		if ((analysis) && (session != null)) {
			this.setCommon(session.getParameters().chemInfo.lossIsCommonNeutralLoss(this));
			this.setFunctionalGroups(session.getParameters().chemInfo.determineFunctionalGroups(this));
		}
	}
	
	public NeutralLoss(String name, double mass, MolecularFormula molecularFormula, Session session) {
		this(name, mass, molecularFormula, true, session);
	}
	
	public NeutralLoss(String name, double mass, String molFormulaStr, boolean analysis, Session session) {
		this(name, mass, new MolecularFormula(molFormulaStr, session), analysis, session);
	}
	
	public NeutralLoss(String name, double mass, String molFormulaStr, Session session) {
		this(name, mass, new MolecularFormula(molFormulaStr, session), session);
	}

	public void setCommon(boolean isCommon) {
		this.isCommon = isCommon;
	}

	public boolean isCommon() {
		return isCommon;
	}

	public void setFunctionalGroups(HashMap<String, Integer> functionalGroups) {
		this.functionalGroups = functionalGroups;
	}

	public HashMap<String, Integer> getFunctionalGroups() {
		return functionalGroups;
	}
	
	public int hasFunctionalGroup(String molFormulaStr) {
		if (!functionalGroups.containsKey(molFormulaStr)) return 0;
		else return functionalGroups.get(molFormulaStr);
	}
	
	@Override
	public NeutralLoss clone() {
		NeutralLoss neutralLoss = new NeutralLoss(new String(name), mass, new MolecularFormula(molecularFormula.toString(), session), session);
		// TODO wrong result if HashMap functionalGroups was adapted
		return neutralLoss;
	}
}
