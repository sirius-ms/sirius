package de.unijena.bioinf.sirius.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;

public class ExperiemtEditPanel extends JPanel {
    public final PrecursorSelector precursorSelection;
    public final IonizationSelector ionizationCB;
    public final JTextField nameTF;
    public final JTextField formulaTF;

    public ExperiemtEditPanel() {
        RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS, 15);
        rl.setAlignment(RelativeLayout.LEADING);
        setLayout(rl);

        nameTF = new JTextField(12);
        add(new TextHeaderBoxPanel("Name", nameTF));

        precursorSelection = new PrecursorSelector();
        add(new TextHeaderBoxPanel(PrecursorSelector.name, precursorSelection));

        ionizationCB = new IonizationSelector();
        add(new TextHeaderBoxPanel(IonizationSelector.name, ionizationCB));

        formulaTF = new JTextField(12);
        add(new TextHeaderBoxPanel("Molecular Formula", formulaTF));
    }

    public ExperiemtEditPanel(ExperimentContainer ec) {
        this();
        if (ec != null) {
            setData(ec);
        }
    }

    public double getSelectedIonMass() {
        return precursorSelection.getSelectedIonMass();
    }

    public boolean setSelectedIonMass(double ionMass) {
        return precursorSelection.setSelectedItem(ionMass);
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

    public void setData(ExperimentContainer ec) {
        nameTF.setText(ec.getName());
        ionizationCB.setSelectedItem(ec.getIonization().getIonization().getName());
        precursorSelection.setData(ec.getMs1Spectra(), ec.getMs2Spectra(), ec.getIonMass());
        setMolecularFomula(ec.getMs2Experiment());
    }

    public void setMolecularFomula(Ms2Experiment ec) {
        String formulaString = GuiUtils.extractMolecularFormulaString(ec);
        formulaTF.setText(formulaString);
    }
}
