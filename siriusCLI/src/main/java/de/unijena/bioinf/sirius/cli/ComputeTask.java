package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.TreeOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ComputeTask extends TreeComputationTask {

    ComputeOptions options;

    public void compute() {
        try {
            final Iterator<Instance> instanceIterator = handleInput(options);
            while (instanceIterator.hasNext()) {
                final Instance i = instanceIterator.next();
                if (i.experiment.getMolecularFormula()==null) {
                    if (options.getMolecularFormula()==null) {
                        System.err.println("The molecular formula for '" + i.file + "' is missing. Add the molecular formula via --formula option or use sirius identify to predict the correct molecular formula");
                    } else {
                        final MutableMs2Experiment exp;
                        if (i.experiment instanceof MutableMs2Experiment) exp = (MutableMs2Experiment) i.experiment;
                        else exp = new MutableMs2Experiment(i.experiment);
                        exp.setMolecularFormula(MolecularFormula.parse(options.getMolecularFormula()));
                        System.out.println("Compute " + i.file + " (" + exp.getMolecularFormula() + ")");
                        final IdentificationResult result = sirius.compute(exp, MolecularFormula.parse(options.getMolecularFormula()), options);
                        output(i, result);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void output(Instance instance, IdentificationResult result) throws IOException {
        File target = options.getTarget();
        final String format;
        if (options.getFormat()!=null) {
            format = options.getFormat();
        } else {
            final String n = target.getName();
            final String ext = n.substring(n.lastIndexOf('.'));
            if (ext.equals(".json") || ext.equals(".dot")) {
                format = ext;
            } else format = "json";
        }

        if (target.isDirectory()) {
            target = getTargetName(target, instance, result, format);
        }

        if (format.equalsIgnoreCase("json")) {
            new FTJsonWriter().writeTreeToFile(target, result.getTree());
        } else if (format.equalsIgnoreCase("dot")) {
            new FTDotWriter(!options.isNoHTML(), !options.isNoIon()).writeTreeToFile(target, result.getTree());
        } else {
            throw new RuntimeException("Unknown format '" + format + "'");
        }

    }

    private File getTargetName(File target, Instance i, IdentificationResult result, String format) {
        final String inputName = i.fileNameWithoutExtension();
        return new File(target, inputName + "." + format);
    }

    @Override
    public String getName() {
        return "compute";
    }

    @Override
    public String getDescription() {
        return "compute a fragmentation tree for the given file, assuming that the (neutral!) molecular formula of the compound is known." +
                "If the molecular formula is not contained in the file then you will have to provide it via command line option.\n" +
                "usage: sirius compute [OPTIONS] {[NEUTRAL MOLECULAR FORMULA] <FILENAME>}+";
    }

    private static class InputFile {
        private MolecularFormula formula;
        private File file;
    }

    @Override
    public void setArgs(String[] args) {
        this.options = CliFactory.createCli(ComputeOptions.class).parseArguments(args);
        setup(options);
        // validate
        final File target = options.getTarget();
        if (target.exists() && !target.isDirectory()) {
            System.err.println("Specify a directory name as output directory");
            System.exit(1);
        }

        final String format = options.getFormat();
        if (format!=null && !format.equalsIgnoreCase("json") && !format.equalsIgnoreCase("dot")) {
            System.err.println("Unknown file format '" + format + "'. Available are 'dot' and 'json'");
            System.exit(1);
        }
    }

    @Override
    public void run() {
        compute();
    }
}
