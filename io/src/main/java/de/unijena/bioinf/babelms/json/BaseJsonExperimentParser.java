package de.unijena.bioinf.babelms.json;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class BaseJsonExperimentParser implements JsonExperimentParser {

    protected JsonNode root;
    protected String recordId;
    protected MutableMs2Experiment experiment;

    @Override
    public Ms2Experiment parse(JsonNode root) {
        this.root = root;
        recordId = getRecordId();
        experiment = new MutableMs2Experiment();

        extractData();

        addSpectrum();
        addAnnotations();

        return experiment;
    }

    protected abstract String getRecordId();

    protected abstract void extractData();

    protected void addSpectrum() {
        SimpleSpectrum spectrum = getSpectrum();

        String msLevelStr = getMsLevel();
        int msLevel;
        if ("MS1".equalsIgnoreCase(msLevelStr) || "1".equals(msLevelStr)) {
            msLevel = 1;
        } else if ("MS2".equalsIgnoreCase(msLevelStr) || "MSMS".equalsIgnoreCase(msLevelStr) || "2".equals(msLevelStr)) {
            msLevel = 2;
        } else {
            throw new RuntimeException("Unsupported ms level " + msLevelStr + " in record " + recordId + ". Expecting MS1 or MS2.");
        }

        if (msLevel == 1) {
            experiment.getMs1Spectra().add(spectrum);
        } else {
            double precursorMz = getPrecursorMz().orElseGet(() -> {
                log.warn("Precursor m/z is not set for MS2 record " + recordId + ", setting to 0.");
                return 0d;
            });
            MutableMs2Spectrum ms2Spectrum = new MutableMs2Spectrum(spectrum, precursorMz, getCollisionEnergy().orElse(null), 2);
            getIonization().ifPresent(ms2Spectrum::setIonization);
            experiment.getMs2Spectra().add(ms2Spectrum);
        }
    }

    protected void addAnnotations() {
        getPrecursorMz().ifPresent(experiment::setIonMass);
        getPrecursorIonType().ifPresent(experiment::setPrecursorIonType);

        getCompoundName().ifPresent(experiment::setName);
        getInstrumentation().ifPresent(instrumentation -> experiment.setAnnotation(MsInstrumentation.class, instrumentation));
        getMolecularFormula().ifPresent(experiment::setMolecularFormula);
        getInchi().ifPresent(experiment::annotate);
        getSmiles().ifPresent(experiment::annotate);
        getSplash().ifPresent(experiment::annotate);
        getRetentionTime().ifPresent(experiment::annotate);

        List<String> tags = getTags();
        if (!tags.isEmpty()) {
            experiment.annotate(new Tagging(tags.toArray(new String[0])));
        }
    }


    /**
     * @return expected "MS1" or "MS2" or "MSMS" or "1" or "2"
     */
    protected abstract String getMsLevel();

    protected abstract SimpleSpectrum getSpectrum();

    protected Optional<Double> getPrecursorMz() {
        return Optional.empty();
    }

    protected Optional<CollisionEnergy> getCollisionEnergy() {
        return Optional.empty();
    }

    protected Optional<Ionization> getIonization() {
        return Optional.empty();
    }

    protected Optional<PrecursorIonType> getPrecursorIonType() {
        return Optional.empty();
    }

    protected Optional<String> getCompoundName() {
        return Optional.empty();
    }

    protected Optional<MsInstrumentation> getInstrumentation() {
        return Optional.empty();
    }

    protected Optional<MolecularFormula> getMolecularFormula() {
        return Optional.empty();
    }

    protected Optional<InChI> getInchi() {
        return Optional.empty();
    }

    protected Optional<Smiles> getSmiles() {
        return Optional.empty();
    }

    protected Optional<Splash> getSplash() {
        return Optional.empty();
    }

    protected Optional<RetentionTime> getRetentionTime() {
        return Optional.empty();
    }

    protected List<String> getTags() {
        return List.of();
    }
}
