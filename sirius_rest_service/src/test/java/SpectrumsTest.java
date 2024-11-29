import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.PeakAnnotation;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class SpectrumsTest {

    @Test
    public void testSpectrumAnnotation() throws IOException {
        InputStream inputStream;
        FTJsonReader treeReader;
        FTree tree;
        AnnotatedSpectrum annotatedSpectrum;

        double precursorMz = 273.08658;


        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/merged_ms2.json");
        String msDataJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        BasicSpectrum msData = new ObjectMapper().readValue(msDataJson, BasicSpectrum.class);
        MutableMs2Spectrum ms2 = new MutableMs2Spectrum(msData);
        ms2.setPrecursorMz(precursorMz);

        MutableMs2Experiment experiment = new MutableMs2Experiment();
        experiment.setIonMass(precursorMz);
        experiment.setMs2Spectra(Collections.singletonList(ms2));


        //test for 3 different adducts

        //[M+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+H+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+H]+"));

        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(experiment, tree);

        Assert.assertEquals("C14H12N2O4", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);

        PeakAnnotation lastPeak = annotatedSpectrum.getPeakAnnotationAt(annotatedSpectrum.size()-1);
        Assert.assertEquals("C14H12N2O4", lastPeak.getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));


        //[M+H+NH3]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+NH4+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+NH4]+"));

        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(experiment, tree);

        Assert.assertEquals("C14H9NO4", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+NH4]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);

        lastPeak = annotatedSpectrum.getPeakAnnotationAt(annotatedSpectrum.size()-1);
        Assert.assertEquals("C14H9NO4", lastPeak.getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+NH4]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));


        //[M-H2O+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M-H2O+H+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        experiment.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"));

        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(experiment, tree);

        Assert.assertEquals("C14H14N2O5", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M-H2O+H]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);

        lastPeak = annotatedSpectrum.getPeakAnnotationAt(annotatedSpectrum.size()-1);
        Assert.assertEquals("C14H12N2O4", lastPeak.getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));
    }
}
