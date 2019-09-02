package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.sirius.Sirius;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * File based input Iterator that allows to iterate over the {@see de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s parsed from
 * multiple files (also different types) that are supported by the {@see de.unijena.bioinf.babelms.MsExperimentParser}.
 * */
public class SiriusInputIterator implements Iterator<Ms2Experiment> {
    private static final Logger LOG = LoggerFactory.getLogger(SiriusInputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<File> fileIter;
    private final double maxMz;
    private final MsExperimentParser parser = new MsExperimentParser();
    private final boolean ignoreFormula;

    private final SiriusProjectSpace space;
    private Function<Ms2Experiment,String> nameFormatter = new StandardMSFilenameFormatter();

    File currentFile;
    Iterator<Ms2Experiment> currentExperimentIterator;

    public SiriusInputIterator(Collection<File> input, SiriusProjectSpace out, double maxMz) {
        this(input, out, maxMz, false);
    }

    public SiriusInputIterator(Collection<File> input, SiriusProjectSpace out, double maxMz, boolean ignoreFormula) {
        fileIter = input.iterator();
        space = out;
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
                        } else currentExperimentIterator = p.parseFromFileIterator(currentFile);
                    } catch (IOException e) {
                        LOG.error("Cannot parse file '" + currentFile + "':\n", e);
                    }
                } else return null;
            } else {
                MutableMs2Experiment experiment = Sirius.makeMutable(currentExperimentIterator.next());

                if (experiment.getIonMass() > maxMz){
                    LOG.info("Skipping instance "+ experiment.getName() +" with mass: " + experiment.getIonMass() + " > " + maxMz);
                }else if (experiment.getMolecularFormula() != null && experiment.getMolecularFormula().numberOf("D") > 0) {
                    LOG.warn("Deuterium Formula found in: " + experiment.getName() + " Instance will be Ignored: ");
                }else {
                    if (ignoreFormula)
                        experiment.setMolecularFormula(null);
                    instances.add(experiment);
                    return currentExperimentIterator;
                }
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Iterator<CompoundContainer> asExpResultIterator() {
        return new ExperimentResultIterator();
    }


    private class ExperimentResultIterator implements Iterator<CompoundContainer> {

        @Override
        public boolean hasNext() {
            return SiriusInputIterator.this.hasNext();
        }

        @Override
        public CompoundContainer next() {
            Ms2Experiment n = SiriusInputIterator.this.next();
            if (n == null) return null;

            final String name = nameFormatter.apply(n);

            //todo I am not sure if this optional stuff is super useful
            return space.newCompoundWithUniqueIndex(name, (idx)-> idx + "_" + name).map(c -> {
                c.addAnnotation(Ms2Experiment.class, n);
                try {
                    space.updateCompound(c, Ms2Experiment.class);
                } catch (IOException e) {
                    LOG.error("Could not save Ms2Experiment to Project-Space", e);
                }
                return c;
            }).orElse(null);
        }
    }


}
