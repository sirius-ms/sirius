package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.utils.CheckBoxListCellRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class AdductSelectionList extends JList<String> implements ListSelectionListener {

    private final JList<String> source;

    public AdductSelectionList(JList<String> sourceIonization) {
        super(new DefaultListModel<String>());
        setCellRenderer(new CheckBoxListCellRenderer());
        source = sourceIonization;
        change(0, source.getModel().getSize() - 1);
        source.addListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        change(e.getFirstIndex(), e.getLastIndex());
    }

    private void change(int first, int last) {

        DefaultListModel<String> m = (DefaultListModel<String>) getModel();
        m.removeAllElements();
        for (int i = first; i <= last; i++) {
            if (source.isSelectedIndex(i)) {
                String ionisation = source.getModel().getElementAt(i);
                for (PrecursorIonType adduct : PeriodicTable.getInstance().adductsByIonisation(ionisation)) {
                    m.addElement(adduct.toString());
                }
            }
        }
    }

}
