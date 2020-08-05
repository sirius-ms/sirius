
package fragtreealigner.domainobjects.chem.components;

import fragtreealigner.domainobjects.chem.basics.MolecularFormula;
import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class Compound extends ChemicalComponent {
	
	public Compound(String name, double mass) {
		super(name, mass);
	}	
	
	public Compound(String name, double mass, MolecularFormula molecularFormula) {
		this(name, mass);
		this.molecularFormula = molecularFormula;
	}
	
	public Compound(String name, double mass, MolecularFormula molecularFormula, Session session) {
		this(name, mass, molecularFormula);
		this.session = session;
	}
	
	public Compound(String name, double mass, String molFormulaStr, Session session) {
		this(name, mass, new MolecularFormula(molFormulaStr, session));
		this.session = session;
	}
	
	@Override
	public Compound clone() {
		Compound clonedCompound = new Compound(new String(name), mass, new MolecularFormula(molecularFormula.toString(), session), session);
		return clonedCompound;
	}
}
