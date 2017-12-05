package de.unijena.bioinf.fingerid.fingerprints;

import ca.odell.glazedlists.gui.TableFormat;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by kaidu on 22.05.17.
 */
public class FingerprintTableFormat implements TableFormat<MolecularPropertyTableEntry> {


    protected NumberFormat decimalFormat = DecimalFormat.getPercentInstance(Locale.US);
    protected FingerprintTable table;
    protected static String[] columns = new String[]{
            "Index", "Type", "Description", "#Atoms", "F1-score", "Posterior Probability"
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
    public Object getColumnValue(MolecularPropertyTableEntry m, int column) {
        switch (column) {
            case 0: return m.absoluteIndex;
            case 1: return m.getFingerprintTypeName();
            case 2: return m.getMolecularProperty().getDescription();
            case 3: return m.getMatchSizeDescription();
            case 4: return m.getFScore();
            case 5: return m.getProbability();
            default:return null;
        }
    }
}
