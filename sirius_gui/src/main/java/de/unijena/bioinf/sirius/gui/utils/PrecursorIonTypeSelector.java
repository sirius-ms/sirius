package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import javax.swing.*;
import java.util.Collections;
import java.util.Vector;
import java.util.stream.Collectors;

public class PrecursorIonTypeSelector extends JComboBox<String> {
    public static final String name = "Adduct";

    public PrecursorIonTypeSelector(Vector<String> adducts, String selected) {
        Collections.sort(adducts);
        setModel(new DefaultComboBoxModel<>(adducts));
        setSelectedItem(selected);
    }

    public PrecursorIonTypeSelector(String selected) {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().map(PrecursorIonType::toString).sorted().collect(Collectors.toList())), selected);
    }

    public PrecursorIonTypeSelector() {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().map(PrecursorIonType::toString).sorted().collect(Collectors.toList())), PeriodicTable.getInstance().unknownPositivePrecursorIonType().getIonization().getName());
    }
}
