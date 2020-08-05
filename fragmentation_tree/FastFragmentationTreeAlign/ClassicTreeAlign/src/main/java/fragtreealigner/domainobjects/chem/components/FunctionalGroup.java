
package fragtreealigner.domainobjects.chem.components;

import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class FunctionalGroup extends ChemicalComponent {
	private int type;
	
	public FunctionalGroup(String name, double mass, String molFormulaStr, int type, Session session) {
		super(name, mass, molFormulaStr, session);
		this.setType(type);
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
