package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Ms2ExperimentShallowCopy implements Ms2Experiment {
    private final Ms2Experiment experiment;
    protected final PrecursorIonType ionType;

    public Ms2ExperimentShallowCopy(Ms2Experiment experiment, PrecursorIonType ionType) {
        this.experiment = experiment;
        if (ionType != null)
            this.ionType = ionType;
        else this.ionType = PrecursorIonType.unknown();

    }

    @Override
    public URL getSource() {
        return experiment.getSource();
    }

    @Override
    public String getName() {
        return experiment.getName();
    }

    @Override
    public PrecursorIonType getPrecursorIonType() {
        return ionType;
    }

    @Override
    public <T extends Spectrum<Peak>> List<T> getMs1Spectra() {
        return experiment.getMs1Spectra();
    }

    @Override
    public <T extends Spectrum<Peak>> T getMergedMs1Spectrum() {
        return experiment.getMergedMs1Spectrum();
    }

    @Override
    public <T extends Ms2Spectrum<Peak>> List<T> getMs2Spectra() {
        return experiment.getMs2Spectra();
    }

    @Override
    public double getIonMass() {
        return experiment.getIonMass();
    }

    @Override
    public double getMoleculeNeutralMass() {
        return getPrecursorIonType().precursorMassToNeutralMass(getIonMass());
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return experiment.getMolecularFormula();
    }

    @Override
    public Annotations<Ms2ExperimentAnnotation> annotations() {
        return experiment.annotations();
    }

    @Override
    public Iterator<Map.Entry<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation>> forEachAnnotation() {
        return experiment.forEachAnnotation();
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> T getAnnotationOrThrow(Class<T> klass) {
        return experiment.getAnnotationOrThrow(klass);
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> T getAnnotation(Class<T> klass) {
        return experiment.getAnnotation(klass);
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> T getAnnotation(Class<T> klass, T defaultValue) {
        return experiment.getAnnotation(klass, defaultValue);
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> boolean hasAnnotation(Class<T> klass) {
        return experiment.hasAnnotation(klass);
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> boolean setAnnotation(Class<T> klass, T value) {
        return experiment.setAnnotation(klass, value);
    }

    @Override
    public <T extends Ms2ExperimentAnnotation> Object clearAnnotation(Class<T> klass) {
        return experiment.clearAnnotation(klass);
    }

    @Override
    public Ms2Experiment clone() {
        return new Ms2ExperimentShallowCopy(experiment, ionType);
    }

    @Override
    public void clearAllAnnotations() {
        experiment.clearAllAnnotations();
    }
}
