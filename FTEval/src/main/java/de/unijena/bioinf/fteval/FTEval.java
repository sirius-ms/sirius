package de.unijena.bioinf.fteval;

import au.com.bytecode.opencsv.CSVReader;
import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.data.DoubleDataMatrix;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.ftblast.Dataset;
import de.unijena.bioinf.ftblast.ScoreTable;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import javax.swing.border.CompoundBorder;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
 * fteval tanimoto x1 x2 x3
 * fteval tanimoto all
 *
 * fteval align
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
        } else if (args[0].equals("tanimoto")) {
            tanimoto(cropped);
        } else if (args[0].equals("align")) {
            align(cropped);
        } else if (args[0].equals("ssps")) {
            ssps(cropped);
        } else {
            System.err.println("Unknown command '" + args[0] + "'. Allowed are 'init', 'compute' and 'ssps'");
        }
    }

    private static void align(String[] args) {
        final Interact I = new Shell();
        final AlignOpts opts = CliFactory.parseArguments(AlignOpts.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final ArrayList<String> arguments = new ArrayList<String>();
        arguments.add("-n");
        arguments.add(String.valueOf(Runtime.getRuntime().availableProcessors()-1));
        if (!opts.isNoFingerprints()) arguments.add("-f");
        arguments.add("-z");
        if (opts.isNoMultijoin()) arguments.add("-j");
        else arguments.addAll(Arrays.asList("-j", "3"));
        arguments.add("-x");
        for (String profil : evalDB.profiles()) {
            final File dots = new File(evalDB.profile(profil), "dot");
            arguments.addAll(Arrays.asList("--align", dots.getAbsolutePath(), "-m",
                    new File(evalDB.profile(profil), "matrix.csv").getAbsolutePath()));
            de.unijena.bioinf.ftalign.Main.main(arguments.toArray(new String[arguments.size()]));
        }
    }

    private static void tanimoto(String[] args) {
        // TODO: parameters
        final Interact I = new Shell();
        final EvalBasicOptions opts = CliFactory.parseArguments(EvalBasicOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final ChemicalSimilarity chem = new ChemicalSimilarity(evalDB);
        for (File sdf : evalDB.sdfFiles()) {
            try {
                chem.add(sdf.getName());
            } catch (IOException e) {
                System.err.println("Can't parse '" + sdf.getName() + "':\n" +e.getMessage());
            }
        }
        try {
            final BufferedWriter inchiWriter = new BufferedWriter(new FileWriter("fingerprints/inchi.txt"));
            for (ChemicalSimilarity.Compound c : chem.getCompounds()) {
                inchiWriter.write(c.name + ":" + c.inchi);
                inchiWriter.newLine();
            }
            inchiWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int i=-1;
        for (File dir : chem.getDirectories()) {
            ++i;
            if (dir.exists()) {
                if (I.ask("A directory with name '" + dir.getName() + "' is still existing. Do you want to replace it?")) {
                    try {
                        deleteRecDir(dir);
                    } catch (IOException e) {
                        System.err.println("Can't delete directory '" + dir + "':\n"+e.getMessage());
                    }
                } else continue;
            }
            dir.mkdir();
            float[][][] matrix = chem.computeTanimoto();
            I.sayln("Compute " + dir.getName());
            try {
                final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(dir, "tanimoto.csv")));
                // write header
                final List<ChemicalSimilarity.Compound> compounds = chem.getCompounds();
                for (int j=0; j <compounds.size(); ++j) {
                    fileWriter.append('"').append(compounds.get(j).name).append('"');
                    if (j+1 < compounds.size()) fileWriter.append(",");
                }
                fileWriter.newLine();
                // write rows
                for (int row=0; row < matrix[i].length; ++row) {
                    fileWriter.append('"').append(compounds.get(row).name).append('"');
                    for (int col=0; col < compounds.size(); ++col) {
                        fileWriter.append(',').append(String.valueOf(matrix[i][row][col]));
                    }
                    fileWriter.newLine();
                }
                fileWriter.close();
            } catch (IOException e) {
                System.err.println("Error while writing in '" + new File(dir, "tanimoto.csv") + "':\n" + e.getMessage());
            }
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
        final File fingerprints = new File(target, "fingerprints");
        sdfs.mkdir();
        mss.mkdir();
        fingerprints.mkdir();
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
            final int choice = I.choice(name + " still exists.", "Replace all", "Compute missing", "Skip");
            if (choice==2) {
                I.sayln("Cancel computation.");
                return;
            } else if (choice == 0) {
                try {
                    deleteRecDir(target);
                } catch (IOException e) {
                    System.err.println(e);
                    return;
                }
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
            final String n = f.getName();
            final File treeFile = new File(dot, n.substring(0, n.lastIndexOf('.'))+".dot");
            if (treeFile.exists()) {
                continue;
            } else {
                I.sayln("Compute '" + f.getName() + "'");
            }
            try {
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
                    final TreeAnnotation ano = new TreeAnnotation(tree, prof.fragmentationPatternAnalysis);
                    try {
                        new FTDotWriter().writeTreeToFile(treeFile, tree, ano.getAdditionalProperties(),
                                ano.getVertexAnnotations(), ano.getEdgeAnnotations());
                    } catch (IOException e) {
                        System.err.println("Cannot write tree to file '" + treeFile.getAbsolutePath() + "':\n" + e);
                    }
                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                System.err.println("SKIP '" + f.getName() + "'");
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

    private static Iterator<String[]> parseMatrix(File file) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(file));
        final CSVReader reader = new CSVReader(r);
        return new Iterator<String[]>() {
            String[] row = reader.readNext();
            @Override
            public boolean hasNext() {
                return row != null;
            }

            @Override
            public String[] next() {
                if (hasNext()) {
                    final String[] r = row;
                    readNext();
                    return r;
                } else throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            private void readNext() {
                try {
                    row = reader.readNext();
                    if (row==null) reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    }

    public static void ssps(String[] args) {
        final Interact I = new Shell();
        final EvalBasicOptions opts = CliFactory.parseArguments(EvalBasicOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final List<Iterator<String[]>> templates = new ArrayList<Iterator<String[]>>();
        final List<Iterator<String[]>> others = new ArrayList<Iterator<String[]>>();
        final List<String> names = new ArrayList<String>();
        for (String p : evalDB.profiles()) {
            try {
                templates.add(parseMatrix(evalDB.scoreMatrix(p)));
                names.add(p);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        for (String tanimoto : evalDB.fingerprints()) {
            try {
                others.add(parseMatrix(evalDB.fingerprint(tanimoto)));
                names.add(tanimoto);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        final DoubleDataMatrix matrices = DoubleDataMatrix.overlay(templates, others, names, null, 0d);
        final Dataset dataset = new Dataset(new ScoreTable("Pubchem", matrices.getLayer("Pubchem")));
        for (int k=2; k < 5; ++k) {
            System.out.println("SSPS (k = " + k + ": ");
            System.out.println("Opt: " + dataset.sspsOpt(k));
            System.out.println("Random: " + dataset.sspsAverageRandom(k));
            for (int i=0; i < matrices.getLayerHeader().length; ++i) {
                if (matrices.getLayerHeader()[i].equals("Pubchem")) continue;
                else {
                    final ScoreTable tab = new ScoreTable(matrices.getLayerHeader()[i], matrices.getLayer(i));
                    System.out.println(tab.getName() + ": " + dataset.ssps(tab, k));
                }
            }
        }




    }

}
