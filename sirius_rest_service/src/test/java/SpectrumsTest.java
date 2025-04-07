import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.ms.middleware.model.spectra.*;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

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
        SimpleSpectrum spectrum = new SimpleSpectrum(msData.getMasses(), msData.getIntensities());

        //test for 3 different adducts

        //[M+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+H+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(precursorMz, spectrum, tree);

        Assert.assertEquals("C14H12N2O4", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);

        PeakAnnotation lastPeak = StreamSupport.stream(annotatedSpectrum.spliterator(), false).map(AnnotatedPeak::getPeakAnnotation).filter(Objects::nonNull)
                .filter(p -> "C14H12N2O4".equals(p.getMolecularFormula()))
                .findAny().orElseThrow();

        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));
        //[M+H+NH3]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+NH4+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);


        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(precursorMz, spectrum, tree);

        Assert.assertEquals("C14H9NO4", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M+NH4]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);


        lastPeak = StreamSupport.stream(annotatedSpectrum.spliterator(), false).map(AnnotatedPeak::getPeakAnnotation).filter(Objects::nonNull)
                .filter(p -> "C14H9NO4".equals(p.getMolecularFormula()))
                .findAny().orElseThrow();
        Assert.assertEquals(PrecursorIonType.fromString("[M+NH4]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));


        //[M-H2O+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M-H2O+H+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        annotatedSpectrum = Spectrums.createMergedMsMsWithAnnotations(precursorMz, spectrum, tree);

        Assert.assertEquals("C14H14N2O5", annotatedSpectrum.getSpectrumAnnotation().getMolecularFormula());
        Assert.assertEquals(PrecursorIonType.fromString("[M-H2O+H]+"), PrecursorIonType.fromString(annotatedSpectrum.getSpectrumAnnotation().getAdduct()));
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationMz() < 0.001);
        Assert.assertTrue(annotatedSpectrum.getSpectrumAnnotation().getMassDeviationPpm() < 5);
        Assert.assertEquals(273.0869833160905, annotatedSpectrum.getSpectrumAnnotation().getExactMass().doubleValue(), 1e-6);

        lastPeak = StreamSupport.stream(annotatedSpectrum.spliterator(), false).map(AnnotatedPeak::getPeakAnnotation).filter(Objects::nonNull)
                .filter(p -> "C14H12N2O4".equals(p.getMolecularFormula()))
                .findAny().orElseThrow();
        Assert.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(lastPeak.getAdduct()));
    }
}
