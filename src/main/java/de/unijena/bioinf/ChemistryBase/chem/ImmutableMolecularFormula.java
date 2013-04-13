package de.unijena.bioinf.ChemistryBase.chem;

import java.util.Arrays;

class ImmutableMolecularFormula extends MolecularFormula {

	private final short[] amounts;
	private final TableSelection selection;
	private final double mass;
	private final int hash;
	
	public ImmutableMolecularFormula(MolecularFormula formula) {
		this(formula.getTableSelection(), formula.buffer());
	}

	ImmutableMolecularFormula(TableSelection selection, short[] buffer) {
		int i = buffer.length-1;
		while (i >= 0 && buffer[i] == 0) --i;
		this.amounts = Arrays.copyOf(buffer, i+1);
		this.selection = selection;
		this.mass = calcMass();
		this.hash = super.hashCode();
	}
    /*
    protected int calculateHash() {
        final short[] buf = buffer();
        if (buf.length==0) return 0;
        int hash = 0;
        hash |= buf[0];
        if (buf.length == 1) return hash;
        hash |= buf[1]<<8;
        for (int i=1; i < buf.length; ++i) {
            hash |= buf[i]<<(8+i*3);
        }
        return hash;
    }
    */

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
	public double getMass() {
		return mass;
	}
	
	@Override
	public int getIntMass() {
		return calcIntMass();
	}
	
	@Override
	public TableSelection getTableSelection() {
		return selection;
	}

	@Override
	protected short[] buffer() {
		return amounts;
	}
	
	

}
