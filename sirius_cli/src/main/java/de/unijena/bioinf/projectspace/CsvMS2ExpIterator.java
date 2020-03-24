package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.CsvParser;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CsvMS2ExpIterator implements InstIterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);

    private final Iterator<InputFilesOptions.CsvInput> basIter;
    private final CsvParser parer = new CsvParser();
    private final Predicate<Ms2Experiment> filter;

    private Ms2Experiment next = null;

    public CsvMS2ExpIterator(List<InputFilesOptions.CsvInput> basIter, Predicate<Ms2Experiment> filter) {
        this(basIter.iterator(), filter);
    }

    public CsvMS2ExpIterator(Iterator<InputFilesOptions.CsvInput> basIter, Predicate<Ms2Experiment> filter) {
        this.basIter = basIter;
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        if (basIter.hasNext()) {
            final InputFilesOptions.CsvInput csvInput = basIter.next();
            final Ms2Experiment exp = parer.parseSpectra(csvInput.ms1, csvInput.ms2, csvInput.parentMz, csvInput.ionType, csvInput.formula);
            if (!filter.test(exp)) {
                LOG.info("Skipping instance (CSV)" + csvInput.ms2.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")) + " because it does not match the Filter criterion.");
                return hasNext();
            } else {
                next = exp;
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
            return next;
        } finally {
            next = null;
        }
    }
}
