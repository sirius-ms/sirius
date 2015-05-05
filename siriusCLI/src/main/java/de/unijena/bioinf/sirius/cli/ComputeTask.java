package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ComputeTask extends TreeComputationTask {

    ComputeOptions options;

    public void compute() {
        try {
            final Iterator<Instance> instanceIterator = handleInput(options);
            while (instanceIterator.hasNext()) {
                Instance i = instanceIterator.next();
                if (i.experiment.getMolecularFormula() == null && options.getMolecularFormula() == null) {
                    System.err.println("The molecular formula for '" + i.file + "' is missing. Add the molecular formula via --formula option or use sirius identify to predict the correct molecular formula");
                } else {
                    if (i.experiment.getMolecularFormula()==null) {
                        final MutableMs2Experiment expm;
                        if (i.experiment instanceof MutableMs2Experiment) expm = (MutableMs2Experiment) i.experiment;
                        else expm = new MutableMs2Experiment(i.experiment);
                        expm.setMolecularFormula(MolecularFormula.parse(options.getMolecularFormula()));
                        i = new Instance(expm, i.file);
                    }
                    System.out.println("Compute " + i.file + " (" + i.experiment.getMolecularFormula() + ")");
                    final IdentificationResult result = sirius.compute(i.experiment, i.experiment.getMolecularFormula(), options);
                    output(i, result);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void output(Instance instance, IdentificationResult result) throws IOException {
        if (result==null) {
            System.out.println("Cannot find valid tree with molecular formula '" + instance.experiment.getMolecularFormula() + "' that supports the data. You might try to increase the allowed mass deviation with parameter --ppm-max");
            return;
        }
        result.getTree().normalizeStructure();
        File target = options.getOutput();
        if (target==null) target = new File(".");
        String format;
        final String n = target.getName();
        final int i = n.lastIndexOf('.');
        if (i >= 0) {
            final String ext = n.substring(i);
            if (ext.equals(".json") || ext.equals(".dot")) {
                format = ext;
            } else format = "json";
        } else {
            if (!target.exists()) target.mkdirs();
            format = "json";
        }
        if (options.getFormat() != null) {
            format = options.getFormat();
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
        if (options.isAnnotating()) {
            final File anoName = getTargetName(target, instance, result, "csv");
            new AnnotatedSpectrumWriter().writeFile(anoName, instance.optTree);
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
        final File target = options.getOutput();
        if (target.exists() && !target.isDirectory()) {
            System.err.println("Specify a directory name as output directory");
            System.exit(1);
        }

        final String format = options.getFormat();
        if (format != null && !format.equalsIgnoreCase("json") && !format.equalsIgnoreCase("dot")) {
            System.err.println("Unknown file format '" + format + "'. Available are 'dot' and 'json'");
            System.exit(1);
        }
    }

    @Override
    public void run() {
        compute();
    }
}
