package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentifyOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IdentifyTask extends TreeComputationTask {

    IdentifyOptions options;

    @Override
    public void compute(List<File> input) {
        try {
            final Iterator<Instance> instances = handleInput(input);
            while (instances.hasNext()) {
                final Instance i = instances.next();
                progress.info("Compute '" + i.file.getName() + "'");
                final List<IdentificationResult> results = sirius.identify(i.experiment, options, progress);
                int rank=1;
                int n = (int)Math.ceil(Math.log10(results.size()));
                for (IdentificationResult result : results) {
                    printf("%"+n+"d.) %s\t%.2f\n", rank++, result.getMolecularFormula().toString(), result.getScore());
                }
                output(i, results);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void output(Instance instance, List<IdentificationResult> results) throws IOException {
        final File target = options.getTarget();
        String format = options.getFormat();
        if (format==null) format = "json";
        for (IdentificationResult result : results) {
            final File name = getTargetName(target, instance, result, format);
            if (format.equalsIgnoreCase("json")) {
                new FTJsonWriter().writeTreeToFile(name, result.getTree());
            } else if (format.equalsIgnoreCase("dot")) {
                new FTDotWriter().writeTreeToFile(name, result.getTree(), null, null, null);
            } else {
                throw new RuntimeException("Unknown format '" + format + "'");
            }
        }

    }

    private File getTargetName(File target, Instance i, IdentificationResult result, String format) {
        final String inputName = i.fileNameWithoutExtension();
        final File name;
        if (options.getNumberOfCandidates()<=1) {
            name = new File(target, inputName + "." + format);
        } else {
            name = new File(target, inputName + "_" + result.getRank() + "_" + result.getMolecularFormula().toString() + "." + format);
        }
        return name;
    }

    @Override
    public String getName() {
        return "identify";
    }

    @Override
    public String getDescription() {
        return "identify the molecular formula of an unknown compound via MS and MS/MS analysis.\nusage: sirius identify [OPTIONS] [INPUTFILES]\nFor further help run: sirius identify --help\n";
    }

    @Override
    public void setArgs(String[] args) {
        this.options = CliFactory.createCli(IdentifyOptions.class).parseArguments(args);
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
        final List<File> files = new ArrayList<File>(options.getInput().size());
        for (String s : options.getInput()) {
            files.add(new File(s));
        }


        compute(files);
    }
}
