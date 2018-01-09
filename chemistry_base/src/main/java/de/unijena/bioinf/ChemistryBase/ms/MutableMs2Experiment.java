package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class MutableMs2Experiment implements Ms2Experiment {

    private PrecursorIonType precursorIonType = PrecursorIonType.unknown();
    private List<SimpleSpectrum> ms1Spectra;
    private SimpleSpectrum mergedMs1Spectrum;
    private List<MutableMs2Spectrum> ms2Spectra;
    private HashMap<Class<Object>, Object> annotations;
    private double ionMass;
    private double moleculeNeutralMass;
    private MolecularFormula molecularFormula;
    private URL source;
    private String name;

    public MutableMs2Experiment() {
        this.ms1Spectra = new ArrayList<>();
        this.ms2Spectra = new ArrayList<>();
        this.annotations = new HashMap<>();
        this.source = null;
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
        this.annotations = new HashMap<>();
        final Iterator<Map.Entry<Class<Object>, Object>> iter = experiment.forEachAnnotation();
        while (iter.hasNext()) {
            final Map.Entry<Class<Object>, Object> v = iter.next();
            this.annotations.put(v.getKey(), v.getValue());
        }
        this.ionMass = experiment.getIonMass();
        this.moleculeNeutralMass = experiment.getMoleculeNeutralMass();
        this.molecularFormula = experiment.getMolecularFormula();
        this.source = experiment.getSource();
        this.name = experiment.getName();
    }

    public void setSource(URL source) {
        this.source = source;
    }

    public void setSource(File source) {
        try {
            this.source = source.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public URL getSource() {
        return source;
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
    public double getMoleculeNeutralMass() {
        return moleculeNeutralMass;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    @Override
    public Iterator<Map.Entry<Class<Object>, Object>> forEachAnnotation() {
        return annotations.entrySet().iterator();
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

    public void setMoleculeNeutralMass(double moleculeNeutralMass) {
        this.moleculeNeutralMass = moleculeNeutralMass;
    }

    public void setMolecularFormula(MolecularFormula molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    @Override
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T val = getAnnotation(klass);
        if (val == null) throw new NullPointerException("No annotation for key: " + klass.getName());
        else return val;
    }

    @Override
    public <T> T getAnnotation(Class<T> klass) {
        return (T) annotations.get(klass);
    }

    @Override
    public <T> T getAnnotation(Class<T> klass, T defaultValue) {
        final T val = getAnnotation(klass);
        if (val == null) return defaultValue;
        else return val;
    }

    @Override
    public <T> boolean hasAnnotation(Class<T> klass) {
        return annotations.containsKey(klass);
    }

    @Override
    public <T> boolean setAnnotation(Class<T> klass, T value) {
        final T val = (T) annotations.put((Class<Object>) klass, value);
        return val != null;
    }

    @Override
    public MutableMs2Experiment clone() {
        return new MutableMs2Experiment(this);
    }
}
