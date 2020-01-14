package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.CsvParser;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CsvMS2ExpIterator implements InstIterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);

    private final Iterator<InputFilesOptions.CsvInput> basIter;
    private final CsvParser parer = new CsvParser();
    private final double maxMz;

    private InputFilesOptions.CsvInput next = null;

    public CsvMS2ExpIterator(List<InputFilesOptions.CsvInput> basIter, double maxMz) {
        this(basIter.iterator(), maxMz);
    }

    public CsvMS2ExpIterator(Iterator<InputFilesOptions.CsvInput> basIter, double maxMz) {
        this.basIter = basIter;
        this.maxMz = maxMz;
    }

    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        if (basIter.hasNext()) {
            InputFilesOptions.CsvInput csvInput = basIter.next();
            if (csvInput.parentMz > maxMz) {
                LOG.info("Skipping instance (CSV)" + csvInput.ms2.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")) + " with mass: " + csvInput.parentMz + " > " + maxMz);
                return hasNext();
            } else {
                next = csvInput;
                return true;
            }
        }
        return false;
    }

    @Override
    public Ms2Experiment next() {
        try {
            if (!hasNext())
                return null;
            return parer.parseSpectra(next.ms1, next.ms2, next.parentMz, next.adduct);
        } finally {
            next = null;
        }
    }
}
