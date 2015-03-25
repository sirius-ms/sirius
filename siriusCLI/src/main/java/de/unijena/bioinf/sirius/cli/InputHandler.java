package de.unijena.bioinf.sirius.cli;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class InputHandler {

    public static InputHandler create(Options options) {
        return new InputHandler();
    }

    public Iterator<Instance> receiveInput(TaskHandler handler, Options options) throws InvalidInputException, IOException {
        final MsExperimentParser parser = new MsExperimentParser();
        final ArrayList<Iterator<Instance>> iterators = new ArrayList<Iterator<Instance>>();
        for (final File f : handler.getInputFiles()) {
            final GenericParser<Ms2Experiment> concreteParser = parser.getParser(f);
            if (concreteParser==null) {
                throw new InvalidInputException("Given input format is unknown: '" + f.getAbsolutePath() + "'");
            } else {
                iterators.add(Iterators.transform(concreteParser.parseFromFileIterator(f), new Function<Ms2Experiment, Instance>() {
                    @Override
                    public Instance apply(Ms2Experiment input) {
                        return new Instance(input, f);
                    }
                }));
            }
        }
        return Iterators.concat(iterators.iterator());
    }

}
