package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckBoxList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class AdductSelectionList extends JCheckBoxList<String> implements ListSelectionListener {

    private final JCheckBoxList<String> source;

    public AdductSelectionList(JCheckBoxList<String> sourceIonization) {
        super();
        source = sourceIonization;
        change();
        source.addListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        change();
    }

    private void change() {
        DefaultListModel<CheckBoxListItem<String>> m = (DefaultListModel<CheckBoxListItem<String>>) getModel();
        m.removeAllElements();
        for (String ionisation : source.getCheckedItems()) {
            for (PrecursorIonType adduct : PeriodicTable.getInstance().adductsByIonisation(ionisation)) {
                m.addElement(CheckBoxListItem.getNew(adduct.toString()));
            }
        }
    }

}
