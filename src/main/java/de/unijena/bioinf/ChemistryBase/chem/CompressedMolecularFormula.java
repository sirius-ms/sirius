package de.unijena.bioinf.ChemistryBase.chem;

/**
 * TODO: Fertigstellen
 */
public class CompressedMolecularFormula extends MolecularFormula {

    private final static int[] BIT_SIZES = new int[]{8, 8, 6, 6,5,5,5,5,5,5,5};
    private final static int[] MASKS = new int[]{255, 255, 63, 63, 31, 31, 31, 31, 31, 31, 31};
    private final static int[] SHIFTS = new int[]{0, 9, 17, 23, 29, 34, 39, 44, 49, 54, 59};


    private final long bits;
    private final TableSelection selection;
    private final double mass;

    public CompressedMolecularFormula(CompressedMolecularFormula mf) {
        this.bits = mf.bits;
        this.selection = mf.getTableSelection();
        this.mass = calcMass();
    }

    public CompressedMolecularFormula(MolecularFormula formula) {
        this.bits = encode(formula);
        this.selection = formula.getTableSelection();
        this.mass = calcMass();
    }

    @Override
    public TableSelection getTableSelection() {
        return selection;
    }

    @Override
    public int numberOf(Element element) {
        return numberAt(bits, selection.indexOf(element));
    }

    @Override
    public double getMass() {
        return mass;
    }

    protected static int numberAt(long bits, int i) {
        return (int)((bits << SHIFTS[i])) & MASKS[i];
    }

    protected static long encode(MolecularFormula formula) {
        if (formula instanceof CompressedMolecularFormula) return ((CompressedMolecularFormula)formula).bits;
        long bits = 0;
        final short[] buffer = formula.buffer();
        for (int i=0; i < buffer.length; ++i){
            bits |= (buffer[i]>>SHIFTS[i]);
        }
        return bits;
    }

    @Override
    protected short[] buffer() {
        final short[] buffer = new short[selection.size()];
        for (int i=0; i < selection.size(); ++i) {
            buffer[i] = (short)numberAt(bits, i);
        }
        return buffer;
    }
}
