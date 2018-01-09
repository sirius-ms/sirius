package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import javax.swing.*;

public class ExperiemtEditPanel extends JPanel {
    public final PrecursorSelector precursorSelection;
    public final IonizationSelector ionizationCB;
    public final JTextField nameTF;

    public ExperiemtEditPanel() {
        RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS, 15);
        rl.setAlignment(RelativeLayout.LEADING);
        setLayout(rl);

        nameTF = new JTextField(12);
        nameTF.setEditable(true);
        add(new TextHeaderBoxPanel("Name", nameTF));

        precursorSelection = new PrecursorSelector();
        add(new TextHeaderBoxPanel(PrecursorSelector.name, precursorSelection));

        ionizationCB = new IonizationSelector();
        add(new TextHeaderBoxPanel(IonizationSelector.name, ionizationCB));
    }

    public Double getSelectedIonMass() {
        return precursorSelection.getSelectedIonMass();
    }

    public String getExperiementName() {
        return nameTF.getText();
    }

    public PrecursorIonType getSelectedIonization() {
        String item = (String) ionizationCB.getSelectedItem();
        if (item != null)
            return PeriodicTable.getInstance().ionByName(item);
        return PeriodicTable.getInstance().getUnknownPrecursorIonType(1);
    }
}
