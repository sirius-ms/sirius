package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.TreeOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ComputeTask extends TreeComputationTask {

    TreeOptions options;

    public void compute(List<InputFile> input) {
        try {

            final String tname = options.getTarget().getName();
            if (input.size() > 1 || (!tname.endsWith(".json") && !tname.endsWith(".dot"))) {
                options.getTarget().mkdirs();
            }

            final HashMap<File, MolecularFormula> map = new HashMap<File, MolecularFormula>();
            for (InputFile i : input) map.put(i.file, i.formula);

            final Iterator<Instance> instances = handleInput(new ArrayList<File>(map.keySet()));
            while (instances.hasNext()) {
                final Instance i = instances.next();
                progress.info("Compute '" + i.file.getName() + "'");

                final MolecularFormula formula;

                if (map.containsKey(i.file)) formula = map.get(i.file);
                else if (i.experiment.getMolecularFormula()!=null) formula=i.experiment.getMolecularFormula();
                else {
                    System.err.println("Molecular formula of '" + i.file + "' is unknown. Please add the molecular formula before the filename or run sirius identify to predict the correct molecular formula.");
                    System.exit(1);
                    return;
                }

                final IdentificationResult result = sirius.compute(i.experiment, formula, options);
                output(i, result);
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
            if (ext.equals("json") || ext.equals("dot")) {
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
        this.options = CliFactory.createCli(TreeOptions.class).parseArguments(args);
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

    private final static Pattern FORMULA_PATTERN = Pattern.compile("^([A-Z][a-z]*\\d*)+$");

    @Override
    public void run() {
        final List<InputFile> files = new ArrayList<InputFile>(options.getInput().size());
        files.add(new InputFile());
        for (String s : options.getInput()) {
            if (FORMULA_PATTERN.matcher(s).matches()) {
                files.get(files.size()-1).formula = MolecularFormula.parse(s);
            } else {
                final File f = new File(s);
                if (f.exists()) {
                    files.get(files.size()-1).file = f;
                    files.add(new InputFile());
                } else {
                    System.err.println("'" + f.getName() + "' is neither a molecular formula nor an existing file.");
                }
            }
        }
        if (files.get(files.size()-1).file==null) files.remove(files.size()-1);
        compute(files);
    }
}
