package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;

/**
 * File based input Iterator that allows to iterate over the {@see de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s parsed from
 * multiple files (also different types) that are supported by the {@see de.unijena.bioinf.babelms.MsExperimentParser}.
 */
public class MS2ExpInputIterator implements Iterator<Ms2Experiment> {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<Path> fileIter;
    private final double maxMz;
    private final MsExperimentParser parser = new MsExperimentParser();
    private final boolean ignoreFormula;


    Path currentFile;
    Iterator<Ms2Experiment> currentExperimentIterator;

    public MS2ExpInputIterator(Collection<Path> input, double maxMz) {
        this(input, maxMz, false);
    }

    public MS2ExpInputIterator(Collection<Path> input, double maxMz, boolean ignoreFormula) {
        fileIter = input.iterator();
        this.maxMz = maxMz;
        currentExperimentIterator = fetchNext();
        this.ignoreFormula = ignoreFormula;
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
        //todo csv support
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

                    if (experiment.getIonMass() > maxMz) {
                        LOG.info("Skipping instance " + experiment.getName() + " with mass: " + experiment.getIonMass() + " > " + maxMz);
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

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public InstanceIteratorMS2Exp asInstanceIterator(ProjectSpaceManager projectSpace) {
        return new InstanceIteratorMS2Exp(this, projectSpace);
    }
}
