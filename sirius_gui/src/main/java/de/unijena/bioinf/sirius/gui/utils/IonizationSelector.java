package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import javax.swing.*;
import java.util.Collections;
import java.util.Vector;

public class IonizationSelector extends JComboBox<String> {
    public static final String name = "Ionization";

    public IonizationSelector(Vector<String> ionizations, String selected) {
        Collections.sort(ionizations);
        setModel(new DefaultComboBoxModel<>(ionizations));
        setSelectedItem(selected);
    }

    public IonizationSelector(String selected) {
        this(new Vector<>(PeriodicTable.getInstance().getIonizationsAndUnknowns()), selected);
    }

    public IonizationSelector() {
        this(new Vector<>(PeriodicTable.getInstance().getIonizationsAndUnknowns()), PeriodicTable.getInstance().unknownPositivePrecursorIonType().getIonization().getName());
    }
}
