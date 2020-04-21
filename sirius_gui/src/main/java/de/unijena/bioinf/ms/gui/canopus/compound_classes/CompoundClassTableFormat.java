package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import ca.odell.glazedlists.gui.TableFormat;

public class CompoundClassTableFormat implements TableFormat<ClassyfirePropertyBean> {
    protected static String[] columns = new String[]{
            "Index",
            "Name",
            "Posterior Probability",
            "Description",
            "ID",
            "Parent",
//            "Positive training examples",
//            "Predictor quality (F1)"
    };

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getColumnValue(ClassyfirePropertyBean prop, int column) {
        int col = 0;
        if (column == col++) return prop.getAbsoluteIndex();
        if (column == col++) return prop.getMolecularProperty().getName();
        if (column == col++) return prop.getProbability();
        if (column == col++) return prop.getMolecularProperty().getDescription();
        if (column == col++) return prop.getMolecularProperty().getChemontIdentifier();
        if (column == col++) return prop.getMolecularProperty().getParent().getName();
//        if (column == col++) return prop.getNumberOfTrainingExamples();
//        if (column == col) return prop.getFScore();
        return null;
    }
}
