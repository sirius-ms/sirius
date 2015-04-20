package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class CLI {

    private Options options;

    public static void main(String[] args) {
        new CLI(CliFactory.createCli(Options.class).parseArguments(args)).run();
    }

    public CLI(Options options) {
        this.options = options;
    }

    public void run() {
        final OutputHandler out = OutputHandler.create(options);
        final InputHandler in = InputHandler.create(options);
        final IdentifyFormulaHandler identify;
        try {
            identify = IdentifyFormulaHandler.create(options);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        final TaskHandler taskHandler = TaskHandler.create(options);
        final Iterator<Instance> compoundIterator;
        try {
            compoundIterator = in.receiveInput(taskHandler, options);
        } catch (InvalidInputException e) {
            System.err.println(e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        while (compoundIterator.hasNext()) {
            final Instance experiment = compoundIterator.next();
            final IdentificationResult result = identify.identify(experiment);
            out.handle(result);
        }
    }
}
