package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

/**
 * MSInfo contains the spectra and additional information extracted out of a *.ms-file
 *
 * Created by IntelliJ IDEA.
 * User: Marcus
 * Date: 05.12.12
 * Time: 17:19
 * To change this template use File | Settings | File Templates.
 */
public class MSInfo{
    String moleculeNameString = null;
    String molecularFormulaString = null;
    double parentMass = Double.NEGATIVE_INFINITY;
    double charge;
    Peak parentPeak;
    MsSpectrum[] spectra;

    public MSInfo() {
    }

    public String getMoleculeNameString() {
        return moleculeNameString;
    }

    public void setMoleculeNameString(String moleculeNameString) {
        this.moleculeNameString = moleculeNameString;
    }

    public String getMolecularFormulaString() {
        return molecularFormulaString;
    }

    public void setMolecularFormulaString(String molecularFormulaString) {
        this.molecularFormulaString = molecularFormulaString;
    }

    public double getParentMass() {
        return parentMass;
    }

    public void setParentMass(double parentMass) {
        this.parentMass = parentMass;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    public Peak getParentPeak() {
        return parentPeak;
    }

    public void setParentPeak(Peak parentPeak) {
        this.parentPeak = parentPeak;
    }

    public MsSpectrum[] getSpectra() {
        return spectra;
    }

    public void setSpectra(MsSpectrum[] spectra) {
        this.spectra = spectra;
    }
}