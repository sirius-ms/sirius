package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class MSInput {

    private Spectrum<Peak> ms1Spectrum;
    private final List<Ms2SpectrumImpl> ms2Spectra;
    private String name;
    private MolecularFormula formula;
    private double formulaChargedMass;
    private double modificationMass;
    private Ionization standardIon;
    private final HashMap<String, String> optionalProperties;


    public MSInput(String name) {
        this.name = name;
        this.optionalProperties = new HashMap<String, String>();
        this.ms2Spectra = new ArrayList<Ms2SpectrumImpl>();
    }

    public double getModificationMass() {
        return modificationMass;
    }

    public void setModificationMass(double modificationMass) {
        this.modificationMass = modificationMass;
    }

    public double getFormulaChargedMass() {
        return formulaChargedMass;
    }

    public void setFormulaChargedMass(double formulaChargedMass) {
        this.formulaChargedMass = formulaChargedMass;
    }

    public Spectrum<Peak> getMs1Spectrum() {
        return ms1Spectrum;
    }

    public void setMs1Spectrum(Spectrum<Peak> ms1Spectrum) {
        this.ms1Spectrum = ms1Spectrum;
    }

    public List<Ms2SpectrumImpl> getMs2Spectra() {
        return ms2Spectra;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public void setFormula(MolecularFormula formula) {
        this.formula = formula;
    }

    public Ionization getStandardIon() {
        return standardIon;
    }

    public void setStandardIon(Ionization standardIon) {
        this.standardIon = standardIon;
    }

    public HashMap<String, String> getOptionalProperties() {
        return optionalProperties;
    }
}
