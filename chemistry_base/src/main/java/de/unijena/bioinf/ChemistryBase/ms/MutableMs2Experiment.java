package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MutableMs2Experiment implements Ms2Experiment {

    private PrecursorIonType precursorIonType;
    private List<SimpleSpectrum> ms1Spectra;
    private SimpleSpectrum mergedMs1Spectrum;
    private List<MutableMs2Spectrum> ms2Spectra;

    private double ionMass;
    private MolecularFormula molecularFormula;
    private String name;


    private Annotated.Annotations<Ms2ExperimentAnnotation> annotations;

    @Override
    public Annotations<Ms2ExperimentAnnotation> annotations() {
        return annotations;
    }

    public MutableMs2Experiment() {
        this.ms1Spectra = new ArrayList<>();
        this.ms2Spectra = new ArrayList<>();
        this.annotations = new Annotations<>();
        this.name = "";
    }

    public MutableMs2Experiment(Ms2Experiment experiment) {
        this.precursorIonType = experiment.getPrecursorIonType();
        this.ms1Spectra = new ArrayList<>();
        for (Spectrum<Peak> spec : experiment.getMs1Spectra())
            ms1Spectra.add(new SimpleSpectrum(spec));
        this.mergedMs1Spectrum = experiment.getMergedMs1Spectrum() == null ? null : new SimpleSpectrum(experiment.getMergedMs1Spectrum());
        this.ms2Spectra = new ArrayList<>();
        int id = 0;
        for (Ms2Spectrum<Peak> ms2spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum ms2 = new MutableMs2Spectrum(ms2spec);
            ms2.setScanNumber(id++);
            this.ms2Spectra.add(ms2);

        }
        this.annotations = experiment.annotations().clone();
        this.ionMass = experiment.getIonMass();
//        this.moleculeNeutralMass = experiment.getMoleculeNeutralMass();
        this.molecularFormula = experiment.getMolecularFormula();
        this.name = experiment.getName();
    }

    @Override
    public MutableMs2Experiment mutate() {
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Nullable
    public URL getSource() {
        if (hasAnnotation(SpectrumFileSource.class))
            return getAnnotation(SpectrumFileSource.class).value;
        if (hasAnnotation(MsFileSource.class))
            return getAnnotation(MsFileSource.class).value;
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PrecursorIonType getPrecursorIonType() {
        return precursorIonType;
    }

    @Override
    public List<SimpleSpectrum> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public SimpleSpectrum getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    @Override
    public List<MutableMs2Spectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public void setPrecursorIonType(PrecursorIonType precursorIonType) {
        this.precursorIonType = precursorIonType;
    }

    public void setMs1Spectra(List<SimpleSpectrum> ms1Spectra) {
        this.ms1Spectra = ms1Spectra;
    }

    public void setMergedMs1Spectrum(SimpleSpectrum mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }

    public void setMs2Spectra(List<MutableMs2Spectrum> ms2Spectra) {
        this.ms2Spectra = ms2Spectra;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    public void setMolecularFormula(MolecularFormula molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    @Override
    public MutableMs2Experiment clone() {
        return new MutableMs2Experiment(this);
    }
}
