package de.unijena.bioinf.babelms.ms;

import com.sun.xml.xsom.impl.scd.Iterators;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.babelms.SpectralParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvParser extends SpectralParser {

    public CsvParser() {

    }

    private final static Pattern PEAK_PATTERN = Pattern.compile("^([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)(\\s+|,|;)([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)");

    @Override
    public Iterator<Ms2Spectrum<Peak>> parseSpectra(BufferedReader reader) throws IOException {
        String line;
        final MutableMs2Spectrum spec = new MutableMs2Spectrum();
        while ((line=reader.readLine())!=null) {
            final Matcher m = PEAK_PATTERN.matcher(line);
            if (m.find()) {
                spec.addPeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(3)));
            }
        }
        reader.close();
        return Iterators.singleton((Ms2Spectrum<Peak>)spec);
    }
}
