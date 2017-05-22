package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Created by kaidu on 22.05.17.
 */
public class FingerprintTableFormat implements TableFormat {

    protected FingerprintTable table;

    protected static String[] columns = new String[]{
            "index", "type", "description", "probability"
    };

    public FingerprintTableFormat(FingerprintTable table) {
        this.table = table;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getColumnValue(Object baseObject, int column) {
        MolecularPropertyTableEntry m = ((MolecularPropertyTableEntry)baseObject);
        switch (column) {
            case 0: return m.absoluteIndex;
            case 1: return m.getFingerprintTypeName();
            case 2: return m.getMolecularProperty().getDescription();
            case 3: return m.getProbability();
            default:return null;
        }
    }
}
