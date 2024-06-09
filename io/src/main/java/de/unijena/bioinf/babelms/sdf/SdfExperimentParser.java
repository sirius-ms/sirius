package de.unijena.bioinf.babelms.sdf;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.intermediate.ExperimentData;
import de.unijena.bioinf.babelms.intermediate.ExperimentDataParser;
import io.github.dan2097.jnainchi.InchiFlag;
import io.github.dan2097.jnainchi.InchiStatus;
import lombok.extern.slf4j.Slf4j;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static de.unijena.bioinf.ChemistryBase.chem.InChIs.isStandardInchi;
import static de.unijena.bioinf.ChemistryBase.chem.InChIs.newInChI;

@Slf4j
public class SdfExperimentParser implements Parser<Ms2Experiment> {

    InputStream lastSeenInputStream = null;
    BufferedReader lastWrappingReader = null;

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        IteratingSDFReader sdfReader = new IteratingSDFReader(reader, SilentChemObjectBuilder.getInstance());
        if (sdfReader.hasNext()) {
            IAtomContainer sdfData = sdfReader.next();
            ExperimentData data = extractData(sdfData);
            MutableMs2Experiment experiment = new ExperimentDataParser().parse(data);
            experiment.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(source));
            return experiment;
        }
        return null;
    }

    private ExperimentData extractData(IAtomContainer sdfData) {
        ExperimentData data = ExperimentData.builder()
                .id(sdfData.getProperty("ID") != null ? sdfData.getProperty("ID") : sdfData.getProperty("NAME"))
                .spectrum(parseSpectrum(sdfData.getProperty("MASS SPECTRAL PEAKS")))
                .spectrumLevel(sdfData.getProperty("SPECTRUM TYPE"))
                .splash(sdfData.getProperty("SPLASH"))
                .precursorMz(sdfData.getProperty("PRECURSOR M/Z"))
                .precursorIonType(sdfData.getProperty("PRECURSOR TYPE"))
                .instrumentation(sdfData.getProperty("INSTRUMENT") + " " + sdfData.getProperty("INSTRUMENT TYPE"))
                .collisionEnergy(sdfData.getProperty("COLLISION ENERGY"))
                .compoundName(sdfData.getProperty("NAME"))
                .molecularFormula(sdfData.getProperty("FORMULA"))
                .build();

        fillInchi(data, sdfData);
        fillSmiles(data, sdfData);
        return data;
    }

    private SimpleSpectrum parseSpectrum(String peaksStr) {
        if (peaksStr == null) {
            return null;
        }
        String[] peaks = peaksStr.split("\n");
        double[] masses = new double[peaks.length];
        double[] intensities = new double[peaks.length];
        for (int i = 0; i < peaks.length; i++) {
            String[] pair = peaks[i].split(" ");
            masses[i] = Double.parseDouble(pair[0]);
            intensities[i] = Double.parseDouble(pair[1]);
        }
        return new SimpleSpectrum(masses, intensities);
    }

    private void fillInchi(ExperimentData data, IAtomContainer sdfData) {
        try {
            InChI inchi = getInchi(sdfData);
            if (inchi != null) {
                data.setInchi(inchi.in3D);
                data.setInchiKey(inchi.key);
            }
        } catch (CDKException e) {
            log.warn("Could not create InChI from sdf data", e);
        }
    }

    /**
     * Temporary copy-paste from InChISMILESUtils. Cannot use it directly because of a circular dependency, todo issue #114
     */
    public static InChI getInchi(IAtomContainer atomContainer) throws CDKException {
        // this will create a standard inchi, see: https://egonw.github.io/cdkbook/inchi.html
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer, InchiFlag.SNon); //removing stereoInformation produces much less warnings, including 'Omitted undefined stereo'
        InchiStatus state = inChIGenerator.getStatus();
        if (state != InchiStatus.ERROR) {
            if (state == InchiStatus.WARNING)
                log.debug("Warning while reading AtomContainer with title '" + atomContainer.getTitle() + "' -> " + inChIGenerator.getMessage());
            String inchi = inChIGenerator.getInchi();
            if (inchi == null) return null;
            if (!isStandardInchi(inchi))
                throw new IllegalStateException("Non standard Inchi was created ('" + inchi + "'), which is not expected behaviour. Please submit a bug report!");
            String key = inChIGenerator.getInchiKey();
            return newInChI(key, inchi);
        } else {
            throw new CDKException("Error while creating InChI. State: '" + state + "'. Message: '" + inChIGenerator.getMessage() + "'.");
        }
    }

    private void fillSmiles(ExperimentData data, IAtomContainer sdfData) {
        try {
            data.setSmiles(SmilesGenerator.unique().create(sdfData));
        } catch (CDKException e) {
            log.warn("Could not create smiles from sdf data", e);
        }
    }

    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        if (inputStream != lastSeenInputStream) {
            lastSeenInputStream = inputStream;
            lastWrappingReader = FileUtils.ensureBuffering(new InputStreamReader(inputStream));
        }
        return parse(lastWrappingReader, source);
    }
}
