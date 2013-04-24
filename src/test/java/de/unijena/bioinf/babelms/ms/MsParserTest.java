package de.unijena.bioinf.babelms.ms;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.*;

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
        //values example file
        double[] retention = new double[]{91.4615,   92.1733,   92.9055,  93.6412,  89.8151};
        float[] collision = new float[]{  35.0f,     45.0f,     55.0f,    70.0f            };
        double[] tic = new double[]{      1386770.0, 1068350.0, 802941.0, 497284.0         };
        int[] spectrumSize = new int[]{   34,        32,        20,       28,       3      };

        JenaMsParser msParser = new JenaMsParser();
        MSInfo msInfo = msParser.getData(new File(this.getClass().getClassLoader().getResource("Adenosine.ms").getFile()));

        assertEquals("molecule name differs", msInfo.getMoleculeNameString(), "Adenosine");
        assertEquals("molecular formula differs", msInfo.getMolecularFormulaString(), "C10H13N5O4");
        assertEquals("charge differs", (int)msInfo.getCharge(), 1);
        assertEquals("parent mass differs", msInfo.getParentMass(), 268.1000061035156);

        MsSpectrum[] spectra = msInfo.getSpectra();
        assertTrue("there are 5 spectra in the file but parsed where " + spectra.length, spectra.length == 5);

        for (MsSpectrum msSpectrum : spectra) {
            int current = -1;
            for (int i = 0; i < spectra.length; i++) {
                if (retention[i] == msSpectrum.getRetentionTime()) current = i;
            }

            assertFalse("There is no spectrum with retention time "+msSpectrum.getRetentionTime(), current==-1);
            assertEquals("peak count in spectrum "+current+" differs", msSpectrum.size(), spectrumSize[current]);

            if (current == 4) assertEquals("msLevel of spectrum "+current+" differs", msSpectrum.getMsLevel(), 1);
            else {
                assertEquals("msLevel of spectrum "+current+" differs", msSpectrum.getMsLevel(), 2);
                assertEquals("collision energy in spectrum "+current+" differs", msSpectrum.getCollisionEnergy().getMinEnergy(), collision[current], 1e-12);
                assertEquals("tic in spectrum "+current+" differs", msSpectrum.getTotalIonCurrent(), tic[current]);
            }
        }
    }
}
