package de.unijena.bioinf.babelms.mgf;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.Parser;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by kaidu on 21.04.2015.
 */
public class MgfParser implements Parser<Ms2Experiment> {
    @Override
    public Ms2Experiment parse(BufferedReader reader) throws IOException {
        String line;
        boolean reading=false;
        MutableMs2Experiment ms2 = new MutableMs2Experiment();
        while ((line=reader.readLine())!=null) {
            if (line.startsWith("BEGIN IONS")) {
                reading=true;
            } else if (line.startsWith("END IONS")) {
                reading=false;
                break;
            }
        }
        return ms2;
    }
}
