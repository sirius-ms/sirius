package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.net.URL;

public class LibrarySpectrum {
    private String name;
    private OrderedSpectrum<Peak> fragmentationSpectrum;
    private MolecularFormula molecularFormula;
    private PrecursorIonType ionType;
    private Smiles smiles;
    private InChI inChI;
    private double ionMass;
    private URL source;

    public LibrarySpectrum(String name, Spectrum<Peak> fragmentationSpectrum, MolecularFormula molecularFormula, PrecursorIonType ionType, Smiles smiles, InChI inChI, URL source) {
        if (fragmentationSpectrum instanceof OrderedSpectrum){
            this.fragmentationSpectrum = (OrderedSpectrum<Peak>)fragmentationSpectrum;
        } else {
            this.fragmentationSpectrum = new SimpleSpectrum(fragmentationSpectrum);
        }

        this.name = name;
        this.molecularFormula = molecularFormula;
        this.ionType = ionType;
        this.smiles = smiles;
        this.inChI = inChI;
        this.source = source;

        this.ionMass = ionType.neutralMassToPrecursorMass(molecularFormula.getMass());
    }

    public static LibrarySpectrum fromExperiment(Ms2Experiment experiment, Spectrum<Peak> fragmentationSpectrum) {
        final String name = experiment.getName();
        final MolecularFormula formula = experiment.getMolecularFormula();
        final PrecursorIonType ionType = experiment.getPrecursorIonType();
        final Smiles smiles = experiment.getAnnotation(Smiles.class);
        final InChI inChI = experiment.getAnnotation(InChI.class);
        final URL source = experiment.getSource();
        return new LibrarySpectrum(name, fragmentationSpectrum, formula, ionType, smiles, inChI, source);
    }

    public String getName() {
        return name;
    }

    public OrderedSpectrum<Peak> getFragmentationSpectrum() {
        return fragmentationSpectrum;
    }

    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public Smiles getSmiles() {
        return smiles;
    }

    public InChI getInChI() {
        return inChI;
    }

    public double getIonMass() {
        return ionMass;
    }

    public String getStructure() {
        if (inChI!=null) {
            return inChI.in2D;
        } else if (smiles!=null) {
            return smiles.smiles;
        }
        return null;
    }

    public URL getSource() {
        return source;
    }
}
