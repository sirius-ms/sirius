package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import javax.swing.*;
import java.util.Comparator;
import java.util.Vector;
import java.util.stream.Collectors;

public class PrecursorIonTypeSelector extends JComboBox<String> {
    public static final String name = "Adduct";

    public PrecursorIonTypeSelector(Vector<String> adducts, String selected) {
        setModel(new DefaultComboBoxModel<>(adducts));
        setSelectedItem(selected);
    }

    public PrecursorIonTypeSelector(String selected) {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList())), selected);
    }

    public PrecursorIonTypeSelector() {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList())), PeriodicTable.getInstance().unknownPositivePrecursorIonType().getIonization().getName());
    }

    public void refresh() {
        setModel(new DefaultComboBoxModel<>(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList()))));
    }

    static class PrecursorViewComparator implements Comparator<PrecursorIonType> {

        @Override
        public int compare(PrecursorIonType o1, PrecursorIonType o2) {
            int r = Boolean.compare(o1.isIonizationUnknown(), o2.isIonizationUnknown());
            if (r == 0) {
                r = Boolean.compare(o1.getCharge() > 0, o2.getCharge() > 0);
                if (r == 0) {
                    r = o1.toString().compareTo(o2.toString());
                }
            }
            return r;
        }
    }
}

