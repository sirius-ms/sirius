package de.unijena.bioinf.babelms.ms;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: Marcus
 * Date: 06.12.12
 * Time: 08:38
 * To change this template use File | Settings | File Templates.
 */
public class MsParserTest{

    @Test
    public void readFile() throws Exception {
        /*
        //values example file
        double[] retention = new double[]{91.4615,   92.1733,   92.9055,  93.6412,  89.8151};
        float[] collision = new float[]{  35.0f,     45.0f,     55.0f,    70.0f            };
        double[] tic = new double[]{      1386770.0, 1068350.0, 802941.0, 497284.0         };
        int[] spectrumSize = new int[]{   34,        32,        20,       28,       3      };

        JenaMsParser msParser = new JenaMsParser();
        Ms2Experiment experiment = new GenericParser<Ms2Experiment>(msParser).parseFile(new File(this.getClass().getClassLoader().getResource("Adenosine.ms").getFile()));

        assertEquals("molecule name differs", experiment.getName(), "Adenosine");
        assertEquals("molecular formula differs", experiment.getMolecularFormula(), MolecularFormula.parse("C10H13N5O4"));
        assertEquals("charge differs", (int)experiment.getPrecursorIonType().getCharge(), 1);
        assertEquals("parent mass differs", experiment.getIonMass(), 268.1000061035156);

        List<SimpleSpectrum> ms1spectra = experiment.getMs1Spectra();
        List<? extends Ms2Spectrum<Peak>> ms2spectra = experiment.getMs2Spectra();
        assertTrue("there are 4 ms2 spectra in the file but parsed where " + ms2spectra.size(), ms2spectra.size() == 4);
        assertTrue("there is 1 ms1 spectrum in the file but parsed where " + ms1spectra.size(), ms1spectra.size() == 1);
        */
    }
}
