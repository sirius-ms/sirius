
package fragtreealigner.domainobjects.chem.structure;

import fragtreealigner.domainobjects.graphs.Edge;

@SuppressWarnings("serial")
public class MolecularStructureBond extends Edge<MolecularStructureBond, MolecularStructureAtom> {
	private BondType bondType;

	public MolecularStructureBond(MolecularStructureAtom atom1, MolecularStructureAtom atom2) {
		super(atom1, atom2);
	}

	public MolecularStructureBond(MolecularStructureAtom atom1, MolecularStructureAtom atom2, String label, BondType bondType) {
		super(atom1, atom2, label);
		this.bondType = bondType;
	}

	public void setContent(MolecularStructureBond bond) {
		super.setContent(bond);
		this.bondType = bond.getBondType();
	}

	public void setBondType(BondType bondType) {
		this.bondType = bondType;
	}

	public BondType getBondType() {
		return bondType;
	}
	
	@Override
	public String dotParams() {
		String dotParams = super.dotParams();
		dotParams += ", penwidth=\"" + (bondType.ordinal() * 3 + 1) + "\"";
		return dotParams;		
	}
}
