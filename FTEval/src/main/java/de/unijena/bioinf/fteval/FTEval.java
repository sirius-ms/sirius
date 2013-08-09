package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 * fteval gets a dataset and creates following directories
 * dataset/sdf
 * dataset/ms
 * dataset/profiles
 * dataset/profiles/profilename/dot
 *
 * as well as following files
 * dataset/fingerprints
 * dataset/tanimoto.csv
 * dataset/profiles/profilename/scores.csv
 *
 * Usage:
 *
 * fteval init dataset
 *
 * fteval compute profil.json
 *
 * fteval ssps
 *
 */
public class FTEval {

    public static final String VERSION_STRING = "1.0";

    public static void main(String[] args) {
        if (args.length==0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            return;
        }
        final String[] cropped = Arrays.copyOf(args, args.length-1);
        System.arraycopy(args, 1, cropped, 0, cropped.length);
        if (args[0].equals("init")) {
            init(cropped);
        } else if (args[0].equals("compute")) {
            compute(cropped);
        } else if (args[0].equals("ssps")) {
            ssps(cropped);
        } else {
            System.err.println("Unknown command '" + args[0] + "'. Allowed are 'init', 'compute' and 'ssps'");
        }
    }

    private static void printUsage() {
        System.out.println("FTEval evaluates the performance of SIRIUS on a given dataset.\n" +
                "Create an evaluation for a dataset with\nfteval init <nameofdataset>\n"+
                "Then copy all your ms files into the directory dataset/ms\n"+
                "As well as all sdf files into the directory dataset/sdf\n"+
                "You can then start the analysis with\ncd dataset\nfteval compute profilename.json\nfteval ssps");
    }

    public static void init(String[] args) {
        final InitOptions opts = CliFactory.parseArguments(InitOptions.class, args);
        final String name = opts.getName();
        final File target = new File(name);
        if (target.exists()) {
            System.err.println("A directory with the name '" + opts.getName() + "' still exists.");
            System.exit(1);
        }
        target.mkdir();
        final File sdfs = new File(target, "sdf");
        final File mss = new File(target, "ms");
        final File profiles = new File(target, "profiles");
        sdfs.mkdir();
        mss.mkdir();
        profiles.mkdir();
        if (opts.getSdf() != null) {
            final File[] files = opts.getSdf().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".sdf");
                }
            });
            for (File f : files)
                try {
                    Files.copy(f.toPath(), new File(sdfs, f.getName()).toPath());
                } catch (IOException e) {
                    System.err.println("Can't copy file '" + f.getAbsolutePath() + "':\n" + e.getMessage());
                }
        }
        if (opts.getMs() != null) {
            final File[] files = opts.getSdf().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".ms");
                }
            });
            for (File f : files)
                try {
                    Files.copy(f.toPath(), new File(mss, f.getName()).toPath());
                } catch (IOException e) {
                    System.err.println("Can't copy file '" + f.getAbsolutePath() + "':\n" + e.getMessage());
                }
        }
        System.out.println("Create new dataset '" + opts.getName() + "'");
        if (opts.getMs() != null) System.out.println("Dataset contains " + mss.listFiles().length + " compounds");
        else System.out.println("Dataset is still empty. Copy ms files into '" + mss.getAbsolutePath() + "' directory to proceede.");
        if (opts.getSdf() != null) System.out.println("Dataset contains " + mss.listFiles().length + " compound structures");
        else System.out.println("Chemical Structures are still missing. Copy sdf files into '" + sdfs.getAbsolutePath() + "' directory to proceede.");
        System.out.println("Move into directory '" + opts.getName() + "' and execute fteval compute.");
    }

    public static void compute(String[] args) {
        final Interact I = new Shell();
        final ComputeOptions opts = CliFactory.parseArguments(ComputeOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final String name;
        if (opts.getName()==null) {
            name = evalDB.removeExtName(new File(opts.getProfile()));
        } else name = opts.getName();
        final Profile prof;
        try {
            prof = new Profile(opts.getProfile());
        } catch (IOException e) {
            System.err.println("Cannot parse profile '" + opts.getProfile() + "':\n" + e.getMessage());
            return;
        }
        final File target = evalDB.profile(name);
        if (target.exists()) {
            if (!I.ask(name + " still exists. Do you want to replace it?")) {
                I.sayln("Cancel computation.");
                return;
            }
            try {
                deleteRecDir(target);
            } catch (IOException e) {
                System.err.println(e);
                return;
            }
        }
        target.mkdir();
        final File dot = new File(target, "dot");
        dot.mkdir();
        // write profile
        try {
            prof.writeToFile(new File(target, "profile.json"));
        } catch (IOException e) {
            System.err.println(e);
            return;
        }
        I.sayln("Compute trees for all ms files.");
        for (File f : evalDB.msFiles()) {
            I.sayln("Compute '" + f.getName() + "'");
            final Ms2Experiment exp;
            try {
                exp = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(f);
            } catch (IOException e) {
                System.err.println("Can't parse '" + f.getName() + "':\n" + e.getMessage());
                continue;
            }
            final FragmentationTree tree = prof.fragmentationPatternAnalysis.computeTrees(prof.fragmentationPatternAnalysis.preprocessing(exp)).withRecalibration().
                    onlyWith(Arrays.asList(exp.getMolecularFormula())).optimalTree();
            if (tree == null) {
                I.sayln("Don't find tree for '" + f.getName() + "'");
            } else {
                final String n = f.getName();
                final TreeAnnotation ano = new TreeAnnotation(tree, prof.fragmentationPatternAnalysis);
                final File treeFile = new File(dot, n.substring(0, n.lastIndexOf('.'))+".dot");
                try {
                    new FTDotWriter().writeTreeToFile(treeFile, tree, ano.getAdditionalProperties(),
                            ano.getVertexAnnotations(), ano.getEdgeAnnotations());
                } catch (IOException e) {
                    System.err.println("Cannot write tree to file '" + treeFile.getAbsolutePath() + "':\n" + e);
                }
            }
        }
    }

    private static void deleteRecDir(File target) throws IOException {
        Files.walkFileTree(target.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void ssps(String[] args) {

    }






}
