package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * File based input Iterator that allows to iterate over the {@see de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s parsed from
 * multiple files (also different types) that are supported by the {@see de.unijena.bioinf.babelms.MsExperimentParser}.
 */
public class MS2ExpInputIterator implements InstIterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<Path> fileIter;
    private final Predicate<Ms2Experiment> filter;
    private final MsExperimentParser parser = new MsExperimentParser();
    private final boolean ignoreFormula;


    Path currentFile;
    Iterator<Ms2Experiment> currentExperimentIterator;

    public MS2ExpInputIterator(Collection<Path> input, double maxMz, boolean ignoreFormula) {
        this(input, (exp) -> exp.getIonMass() <= maxMz, ignoreFormula);
    }

    public MS2ExpInputIterator(Collection<Path> input, Predicate<Ms2Experiment> filter, boolean ignoreFormula) {
        this.fileIter = input.iterator();
        this.filter = filter;
        this.ignoreFormula = ignoreFormula;
        currentExperimentIterator = fetchNext();
    }


    @Override
    public boolean hasNext() {
        return !instances.isEmpty();
    }

    @Override
    public Ms2Experiment next() {
        fetchNext();
        return instances.poll();
    }

    private Iterator<Ms2Experiment> fetchNext() {
        start:
        while (true) {
            if (currentExperimentIterator == null || !currentExperimentIterator.hasNext()) {
                if (fileIter.hasNext()) {
                    currentFile = fileIter.next();
                    try {
                        GenericParser<Ms2Experiment> p = parser.getParser(currentFile);
                        if (p == null) {
                            LOG.error("Unknown file format: '" + currentFile + "'");
                        } else currentExperimentIterator = p.parseFromPathIterator(currentFile);
                    } catch (IOException e) {
                        LOG.error("Cannot parse file '" + currentFile + "':\n", e);
                    }
                } else return null;
            } else {
                try {
                    MutableMs2Experiment experiment = Sirius.makeMutable(currentExperimentIterator.next());

                    if (!filter.test(experiment)) {
                        LOG.info("Skipping instance " + experiment.getName() + " because it did not pass the filter setting.");
                    } else if (experiment.getMolecularFormula() != null && experiment.getMolecularFormula().numberOf("D") > 0) {
                        LOG.warn("Deuterium Formula found in: " + experiment.getName() + " Instance will be Ignored: ");
                    } else {
                        if (ignoreFormula)
                            experiment.setMolecularFormula(null);
                        instances.add(experiment);
                        return currentExperimentIterator;
                    }
                } catch (Exception e) {
                    LOG.error("Error while parsing compound! Skipping entry", e);
                }
            }
        }
    }
}
