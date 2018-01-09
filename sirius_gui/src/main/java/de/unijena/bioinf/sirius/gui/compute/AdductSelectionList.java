package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckBoxList;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

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
        List<String> m = new ArrayList<>();
        for (String ionisation : source.getCheckedItems()) {
            for (PrecursorIonType adduct : PeriodicTable.getInstance().adductsByIonisation(ionisation)) {
                m.add(adduct.toString());
            }
        }
        replaceElements(m);
        checkAll();
    }

}
