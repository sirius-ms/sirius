package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InputIterator implements Iterator<Ms2Experiment> {
    private static final Logger LOG = LoggerFactory.getLogger(InputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<File> fileIter;
    private final double maxMz;
    private final MsExperimentParser parser = new MsExperimentParser();

    File currentFile;
    Iterator<Ms2Experiment> currentExperimentIterator = fetchNext();

    public InputIterator(Collection<File> input, double maxMz) {
        fileIter = input.iterator();
        this.maxMz = maxMz;
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
                        } else currentExperimentIterator = p.parseFromFileIterator(currentFile);
                    } catch (IOException e) {
                        LOG.error("Cannot parse file '" + currentFile + "':\n", e);
                    }
                } else return null;
            } else {
                MutableMs2Experiment experiment = Sirius.makeMutable(currentExperimentIterator.next());
                instances.add(experiment);
                return currentExperimentIterator;
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Iterator<ExperimentResult> asExpResultIterator() {
        return new ExperimentReultIterator();
    }


    private class ExperimentReultIterator implements Iterator<ExperimentResult> {

        @Override
        public boolean hasNext() {
            return InputIterator.this.hasNext();
        }

        @Override
        public ExperimentResult next() {
            Ms2Experiment n = InputIterator.this.next();
            if (n == null) return null;
            return new ExperimentResult(n, new ArrayList<>());//todo precise array with default value??
        }
    }


}
