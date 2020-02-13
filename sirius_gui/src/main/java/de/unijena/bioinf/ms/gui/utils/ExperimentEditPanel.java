package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.MsExperiments2;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;

import javax.swing.*;

//todo property change support so that other views can listen to changes input data before applying them
public class ExperimentEditPanel extends JPanel {
    public final PrecursorSelector precursorSelection;
    public final PrecursorIonTypeSelector ionizationCB;
    public final JTextField nameTF;
    public final JTextField formulaTF;

    public ExperimentEditPanel() {
        RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS, 15);
        rl.setAlignment(RelativeLayout.LEADING);
        setLayout(rl);

        nameTF = new JTextField(12);
        add(new TextHeaderBoxPanel("Name", nameTF));

        precursorSelection = new PrecursorSelector();
        add(new TextHeaderBoxPanel(PrecursorSelector.name, precursorSelection));

        ionizationCB = new PrecursorIonTypeSelector();
        add(new TextHeaderBoxPanel(PrecursorIonTypeSelector.name, ionizationCB));

        formulaTF = new JTextField(12);
        add(new TextHeaderBoxPanel("Molecular Formula", formulaTF));
    }

    public ExperimentEditPanel(InstanceBean ec) {
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
            return PeriodicTable.getInstance().ionByNameOrThrow(item);
        return PeriodicTable.getInstance().getUnknownPrecursorIonType(1);
    }

    public void setData(InstanceBean ec) {
        nameTF.setText(ec.getName());
        ionizationCB.setSelectedItem(ec.getIonization().toString());
        precursorSelection.setData(ec.getMs1Spectra(), ec.getMs2Spectra(), ec.getIonMass());
        setMolecularFomula(ec.getExperiment());
    }

    public void setMolecularFomula(Ms2Experiment ec) {
        String formulaString = MsExperiments2.extractMolecularFormulaString(ec);
        formulaTF.setText(formulaString);
    }

    public boolean validateFormula() {
        String formulaString = formulaTF.getText();
        if (formulaString == null || formulaString.isEmpty())
            return true;
        MolecularFormula mf = MolecularFormula.parseOrNull(formulaString);

        return mf != null && !mf.equals(MolecularFormula.emptyFormula());
    }

    public MolecularFormula getMolecularFormula() {
        String formulaString = formulaTF.getText();
        if (formulaString == null || formulaString.isEmpty())
            return null;
        return MolecularFormula.parseOrNull(formulaString);
    }
}
