package de.unijena.bioinf.fteval;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.data.DoubleDataMatrix;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.ftblast.Dataset;
import de.unijena.bioinf.ftblast.ScoreTable;
import de.unijena.bioinf.spectralign.SpectralAligner;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * fteval gets a dataset and creates following directories
 * dataset/sdf
 * dataset/ms
 * dataset/profiles
 * dataset/profiles/profilename/dot
 * <p/>
 * as well as following files
 * dataset/fingerprints
 * dataset/tanimoto.csv
 * dataset/profiles/profilename/scores.csv
 * <p/>
 * Usage:
 * <p/>
 * fteval initiate dataset
 * <p/>
 * fteval compute profil.json
 * <p/>
 * fteval tanimoto x1 x2 x3
 * fteval tanimoto all
 * <p/>
 * fteval align
 * <p/>
 * fteval ssps
 */
public class FTEval {

    public static final String VERSION_STRING = "1.0";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            return;
        }
        final String[] cropped = Arrays.copyOf(args, args.length - 1);
        System.arraycopy(args, 1, cropped, 0, cropped.length);
        if (args[0].equals("initiate")) {
            init(cropped);
        } else if (args[0].equals("cleanup")) {
            removeIdenticalCompounds(cropped);
        } else if (args[0].equals("compute")) {
            compute(cropped);
        } else if (args[0].equals("tanimoto")) {
            tanimoto(cropped);
        } else if (args[0].equals("align")) {
            align(cropped);
        } else if (args[0].equals("filter")) {
            filter(cropped);
        } else if (args[0].equals("peaks")) {
            peakcounting(cropped);
        } else if (args[0].equals("decoy")) {
            decoy(cropped);
        } else if (args[0].equals("standardize")) {
            standardize(cropped);
        } else if (args[0].equals("ssps")) {
            ssps(cropped);
        } else if (args[0].equals("test")) {
            test(cropped);
        } else {
            System.err.println("Unknown command '" + args[0] + "'. Allowed are 'initiate', 'compute', 'align', 'decoy' and 'ssps'");
        }
    }

    private static void filter(String[] cropped) {
        final Interact I = Shell.withAlternative();
        final EvalBasicOptions opts = CliFactory.parseArguments(EvalBasicOptions.class, cropped);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        // delete all identical compounds
        final HashSet<String> inchis2d = new HashSet<String>();
        final List<File> toDelete = new ArrayList<File>();
        final Pattern split2D = Pattern.compile("/[btmsifr]");
        I.sayln("Search for duplicates");
        for (File sdf : evalDB.sdfFiles()) {
            try {
                InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
                final BufferedReader reader = new BufferedReader(new FileReader(sdf));
                final ISimpleChemObjectReader chemReader = new ReaderFactory().createReader(reader);
                final AtomContainer mol = chemReader.read(new AtomContainer());
                final InChIGenerator gen = factory.getInChIGenerator(mol);
                if (gen.getReturnStatus() != INCHI_RET.OKAY) {
                    System.err.println(gen.getMessage());
                    toDelete.add(sdf);
                }
                String inchi = gen.getInchi();
                // to 2D
                final Matcher m = split2D.matcher(inchi);
                if (m.find()) {
                    inchi = inchi.substring(0, m.start());
                }
                if (inchis2d.contains(inchi)) toDelete.add(sdf);
                else inchis2d.add(inchi);
            } catch (CDKException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (File f : toDelete) {
            I.sayln("delete " + f.getName());
            f.delete();
        }
    }

    private static void standardize(String[] cropped) {
        final Interact I = Shell.withAlternative();
        final EvalBasicOptions opts = CliFactory.parseArguments(EvalBasicOptions.class, cropped);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final Standardize std = new Standardize();
        final List<File> files = new ArrayList<File>();
        for (String p : evalDB.profiles()) {
            for (File f : evalDB.scoreMatrix(p).getParentFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".csv") && !name.equalsIgnoreCase("decoymatrix.csv");
                }
            })) {
                files.add(f);
            }
        }
        for (String tanimoto : evalDB.fingerprints()) {
            files.add(evalDB.fingerprint(tanimoto));
        }
        for (File score : evalDB.otherScores()) {
            files.add(score);
        }
        I.sayln("Reading matrices");
        for (File f : files) {
            try {
                std.merge(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        I.sayln("Reordering matrices");
        for (File f : files) {
            try {
                writeMatrix(new File(f.getAbsolutePath() + ".std"), std.reorderFile(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void writeMatrix(File f, String[][] strings) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(f));
        writer.writeAll(Arrays.asList(strings));
        writer.close();
    }

    private static void test(String[] args) {
        final Interact I = Shell.withAlternative();
        final SSPSBasicOptions opts = CliFactory.parseArguments(SSPSBasicOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final List<Iterator<String[]>> templates = new ArrayList<Iterator<String[]>>();
        final List<Iterator<String[]>> others = new ArrayList<Iterator<String[]>>();
        final List<String> names = new ArrayList<String>();
        for (String p : evalDB.profiles()) {
            try {
                templates.add(parseMatrix(new File("/home/kai/Documents/temp/fingerprinttest/tablex.csv")));
                names.add("test");
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        final DoubleDataMatrix matrices = DoubleDataMatrix.overlay(templates, others, names, null, 0d);
        final ScoreTable sc = new ScoreTable("test", matrices.getLayer(0));
        sc.toFingerprints(false);
        sc.getOrdered();
    }

    private static void peakcounting(String[] args) {
        final Interact I = Shell.withAlternative();
        final PeakcountingOptions opts = CliFactory.parseArguments(PeakcountingOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        // peak counting
        final SpectralAligner aligner = new SpectralAligner();
        aligner.setLimit(opts.getLimit());
        aligner.setThreshold(opts.getThreshold());
        de.unijena.bioinf.spectralign.Main.process(aligner, evalDB.msDir(), new File(evalDB.otherScoreDir(), extendName(opts.getTarget(), ".csv")));
    }

    private static String extendName(String name, String extension) {
        if (name.endsWith(extension)) return name;
        else return name + extension;
    }

    private static void align(String[] args) {
        final Interact I = Shell.withAlternative();
        final AlignOpts opts = CliFactory.parseArguments(AlignOpts.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final ArrayList<String> profiles = new ArrayList<String>(Arrays.asList(evalDB.profiles()));
        if (opts.names() != null && !opts.names().isEmpty()) {
            profiles.retainAll(opts.names());
        }

        // peak counting
        //de.unijena.bioinf.spectralign.Main.main(new String[]{evalDB.msDir().getPath(), new File(evalDB.otherScoreDir(), "peakcounting.csv").getPath() });

        // tree alignments
        for (String profil : profiles) {
            final File dots = new File(evalDB.profile(profil), "dot");
            final ArrayList<String> arguments = getAlignArguments(opts);
            arguments.addAll(Arrays.asList("--align", dots.getAbsolutePath(), "-m",
                    new File(evalDB.profile(profil), opts.getTarget()).getAbsolutePath()));
            if (opts.getXtra() != null) {
                for (String s : opts.getXtra()) {
                    if (s.charAt(0) == '"') {
                        s = s.substring(1, s.length() - 1);
                    }
                    arguments.add(s);
                }
            }
            System.err.println(arguments);
            de.unijena.bioinf.ftalign.Main.main(arguments.toArray(new String[arguments.size()]));
        }
    }

    // if two compounds are identical, remove one of them
    // if the tree of two compounds contains less than 5 losses, remove it
    private static void removeIdenticalCompounds(String[] args) {
        final Interact I = Shell.withAlternative();
        final CleanupOpts opts = CliFactory.parseArguments(CleanupOpts.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());

        // delete small trees
        final ArrayList<String> toDelete = new ArrayList<String>();
        final int deleteSmallTrees = opts.getEdgeLimit();
        if (deleteSmallTrees > 0) {
            for (String p : evalDB.profiles()) {
                for (File d : evalDB.dotFiles(p)) {
                    final int cl = countNumberOfLosses(d);
                    if (cl < deleteSmallTrees) {
                        final String name = d.getName().substring(0, d.getName().indexOf('.'));
                        System.out.println("delete " + name + " due to low loss number " + cl);
                        toDelete.add(name);
                    }
                }
            }
        }
        for (String s : toDelete) deleteInstance(evalDB, s);

        // get two fingerprints
        final ArrayList<File> fingerprints = new ArrayList<File>();
        if (evalDB.fingerprint("Pubchem").exists()) fingerprints.add(evalDB.fingerprint("Pubchem"));
        if (evalDB.fingerprint("MACCS").exists()) fingerprints.add(evalDB.fingerprint("MACCS"));
        if (fingerprints.size() < 2) {
            for (String s : evalDB.fingerprints()) {
                if (!s.equals("Pubchem") && !s.equals("MACCS") && evalDB.fingerprint(s).exists())
                    fingerprints.add(evalDB.fingerprint(s));
            }
        }
        if (fingerprints.size() < 2) {
            System.out.println("Don't find fingerprints. Please call 'sirius fteval tanimoto' first.");
            return;
        }
        if (!evalDB.garbageDir().exists()) evalDB.garbageDir().mkdir();
        final ArrayList<Iterator<String[]>> iters = new ArrayList<Iterator<String[]>>();
        final List<String> names = new ArrayList<String>();
        for (File f : fingerprints)
            try {
                iters.add(parseMatrix(f));
                names.add(f.getName().substring(0, f.getName().indexOf('.')));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        final DoubleDataMatrix matrix = DoubleDataMatrix.overlay(iters, Collections.<Iterator<String[]>>emptyList(), names,
                null, Double.NEGATIVE_INFINITY);
        for (int row = 0; row < matrix.getRowHeader().length; ++row) {
            for (int col = row + 1; col < matrix.getColHeader().length; ++col) {
                if (matrix.getValues()[0][row][col] > 0.99999 && matrix.getValues()[1][row][col] > 0.99999) {
                    // entry row/col is a identical compound
                    final String nameOfFirst = matrix.getRowHeader()[row];
                    final String nameOfLast = matrix.getColHeader()[col];
                    String nameToDelete = nameOfLast;
                    {
                        for (String prof : evalDB.profiles()) {
                            final File f1 = new File(evalDB.dotDir(prof), nameOfFirst + ".dot");
                            final File f2 = new File(evalDB.dotDir(prof), nameOfLast + ".dot");
                            if (f1.exists() && f2.exists()) {
                                if (f1.length() > f2.length()) nameToDelete = nameOfLast;
                                else nameToDelete = nameOfFirst;
                                break;
                            } else if (f1.exists() && !f2.exists()) {
                                nameToDelete = nameOfLast;
                            } else if (f2.exists() && !f1.exists()) {
                                nameToDelete = nameOfFirst;
                            }
                        }
                    }

                    System.out.println(nameOfFirst + " and " + nameOfLast + " are identical! Delete " + nameToDelete + "!");
                    deleteInstance(evalDB, nameToDelete);
                }
            }
        }


    }

    private static int countNumberOfLosses(File d) {
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(d));
            int loss = 0;
            while (reader.ready()) {
                final String line = reader.readLine();
                if (line == null) break;
                if (line.contains("->")) ++loss;
            }
            return loss;
        } catch (IOException e) {
            return 0;
        }
    }

    private static void deleteInstance(EvalDB evalDB, String nameToDelete) {
        final List<File> files = new ArrayList<File>();
        // delete ms
        files.add(new File(evalDB.msDir(), nameToDelete + ".ms"));
        // delete dots
        for (String prof : evalDB.profiles()) {
            files.add(new File(evalDB.dotDir(prof), nameToDelete + ".dot"));
        }
        for (File f : files) {
            if (f.exists()) {
                try {
                    Files.move(f.toPath(), new File(evalDB.garbageDir(), f.getName()).toPath());
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    private static ArrayList<String> getAlignArguments(AlignOpts opts) {
        final ArrayList<String> arguments = new ArrayList<String>();
        arguments.add("-n");
        arguments.add(String.valueOf(Runtime.getRuntime().availableProcessors()));
        //if (!opts.isNoFingerprints()) arguments.add("-f");
        if (!opts.isNoNormalizing())
            arguments.add("-z");
        if (opts.isNoMultijoin()) arguments.add("-j");
        else arguments.addAll(Arrays.asList("-j", "3"));
        arguments.add("-x");
        return arguments;
    }

    private static void tanimoto(String[] args) {
        // TODO: parameters
        final Interact I = Shell.withAlternative();
        final EvalBasicOptions opts = CliFactory.parseArguments(EvalBasicOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final ChemicalSimilarity chem = new ChemicalSimilarity(evalDB);
        if (evalDB.inchiDir().exists()) {
            I.sayln("parse inchi files");
            for (File inchi : evalDB.inchiFiles()) {
                try {
                    final BufferedReader reader = new BufferedReader(new FileReader(inchi));
                    final String inchiStr = reader.readLine();
                    reader.close();
                    chem.addInchi(inchi.getName(), inchiStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            I.sayln("parse sdf files");
            for (File sdf : evalDB.sdfFiles()) {
                try {
                    chem.add(sdf.getName());
                } catch (IOException e) {
                    System.err.println("Can't parse '" + sdf.getName() + "':\n" + e.getMessage());
                }
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

        int i = -1;
        for (File dir : chem.getDirectories()) {
            ++i;
            if (dir.exists()) {
                if (I.ask("A directory with name '" + dir.getName() + "' is still existing. Do you want to replace it?")) {
                    try {
                        deleteRecDir(dir);
                    } catch (IOException e) {
                        System.err.println("Can't delete directory '" + dir + "':\n" + e.getMessage());
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
                fileWriter.write("\"scores\"");
                for (int j = 0; j < compounds.size(); ++j) {
                    fileWriter.append(",\"").append(compounds.get(j).name).append('"');
                }
                fileWriter.newLine();
                // write rows
                for (int row = 0; row < matrix[i].length; ++row) {
                    fileWriter.append('"').append(compounds.get(row).name).append('"');
                    for (int col = 0; col < compounds.size(); ++col) {
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
                "Create an evaluation for a dataset with\nfteval initiate <nameofdataset>\n" +
                "Then copy all your ms files into the directory dataset/ms\n" +
                "As well as all sdf files into the directory dataset/sdf\n" +
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
        final File other = new File(target, "scores");
        other.mkdir();
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
        else
            System.out.println("Dataset is still empty. Copy ms files into '" + mss.getAbsolutePath() + "' directory to proceede.");
        if (opts.getSdf() != null)
            System.out.println("Dataset contains " + mss.listFiles().length + " compound structures");
        else
            System.out.println("Chemical Structures are still missing. Copy sdf files into '" + sdfs.getAbsolutePath() + "' directory to proceede.");
        System.out.println("Move into directory '" + opts.getName() + "' and execute fteval compute.");
    }

    public static void compute(String[] args) {
        final Interact I = Shell.withAlternative();
        final ComputeOptions opts = CliFactory.parseArguments(ComputeOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final String name;
        if (opts.getName() == null) {
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
            if (choice == 2) {
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
            final File treeFile = new File(dot, n.substring(0, n.lastIndexOf('.')) + ".dot");
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
                final FTree tree = prof.fragmentationPatternAnalysis.computeTrees(prof.fragmentationPatternAnalysis.preprocessing(exp)).withRecalibration().
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
                    if (row == null) reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    }

    public static void decoy(String[] args) {
        final Interact I = Shell.withAlternative();
        final AlignOpts opts = CliFactory.parseArguments(AlignOpts.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        for (String profil : evalDB.profiles()) {
            final File decoy = evalDB.decoy(profil);
            final File dots = new File(evalDB.profile(profil), "dot");
            final ArrayList<String> arguments = getAlignArguments(opts);
            arguments.addAll(Arrays.asList("--align", dots.getAbsolutePath(), "--with", evalDB.decoy(profil).getAbsolutePath(), "-m",
                    new File(evalDB.profile(profil), "decoymatrix.csv").getAbsolutePath()));
            if (opts.getXtra() != null) arguments.addAll(opts.getXtra());
            if (!(new File(evalDB.profile(profil), "decoymatrix.csv")).exists())
                de.unijena.bioinf.ftalign.Main.main(arguments.toArray(new String[arguments.size()]));
            try {
                calculateQValues(new File(evalDB.profile(profil), "matrix.csv"), new File(evalDB.profile(profil), "decoymatrix.csv"));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void calculateQValues(File query, File decoy) throws IOException {
        final List<Hit> hits = new ArrayList<Hit>();
        final DoubleDataMatrix matrix1 = DoubleDataMatrix.overlay(Arrays.asList(parseMatrix(query)), null, Arrays.asList("query"), null, Double.NEGATIVE_INFINITY);
        final DoubleDataMatrix matrix2 = DoubleDataMatrix.overlay(Arrays.asList(parseMatrix(decoy)), null, Arrays.asList("query"), null, Double.NEGATIVE_INFINITY);
        final TObjectIntHashMap<String> mapper = new TObjectIntHashMap<String>(matrix1.getRowHeader().length);
        for (int i = 0; i < matrix2.getRowHeader().length; ++i) {
            mapper.put(matrix2.getRowHeader()[i], i);
        }
        final double[][] m1 = matrix1.getLayer(0);
        final double[][] m2 = matrix2.getLayer(0);
        final double[][] m3 = new double[m1.length][m1[0].length];
        for (int row = 0; row < matrix1.getRowHeader().length; ++row) {
            for (int col = 0; col < matrix1.getColHeader().length; ++col) {
                hits.add(new Hit(row, col, m1[row][col], false));
            }
            final int decoyRow = mapper.get(matrix1.getRowHeader()[row]);
            for (int col = 0; col < matrix2.getColHeader().length; ++col) {
                hits.add(new Hit(decoyRow, col, m2[decoyRow][col], true));
            }
            Collections.sort(hits, Collections.reverseOrder());
            int decoys = 0;
            int k = 0;
            for (int i = 0; i < hits.size(); ++i) {
                if (hits.get(i).decoy || i + 1 == hits.size()) {
                    // compute q values for all j=k..i
                    final double qvalue = decoys / ((double) i - decoys);
                    for (int j = k; j <= i; ++j) {
                        final Hit h = hits.get(j);
                        if (!h.decoy) m3[h.left][h.right] = -qvalue + m1[h.left][h.right] / 100000d;
                    }
                    if (hits.get(i).decoy) ++decoys;
                    k = i + 1;
                }
            }
            hits.clear();
        }
        final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(query.getParent(), "qvalues.csv")));
        writer.write("scores");
        for (int j = 0; j < matrix1.getColHeader().length; ++j) {
            writer.write(",");
            writer.write(matrix1.getColHeader()[j]);
        }
        writer.newLine();
        for (int i = 0; i < matrix1.getRowHeader().length; ++i) {
            writer.write(matrix1.getRowHeader()[i]);
            for (int j = 0; j < matrix1.getColHeader().length; ++j) {
                writer.write(",");
                writer.write(String.valueOf(m3[i][j]));
            }
            writer.newLine();
        }
        writer.close();
    }

    private static void calculateQValuesOld(File query, File decoy) throws IOException {
        final List<Hit> hits = new ArrayList<Hit>();
        final DoubleDataMatrix matrix1 = DoubleDataMatrix.overlay(Arrays.asList(parseMatrix(query)), null, Arrays.asList("query"), null, Double.NEGATIVE_INFINITY);
        final DoubleDataMatrix matrix2 = DoubleDataMatrix.overlay(Arrays.asList(parseMatrix(decoy)), null, Arrays.asList("query"), null, Double.NEGATIVE_INFINITY);
        final double[][] m1 = matrix1.getLayer(0);
        final double[][] m2 = matrix2.getLayer(0);
        for (int i = 0; i < matrix1.getRowHeader().length; ++i) {
            for (int j = 0; j < matrix1.getColHeader().length; ++j) {
                hits.add(new Hit(i, j, m1[i][j], false));
            }
        }
        for (int i = 0; i < matrix2.getRowHeader().length; ++i) {
            for (int j = 0; j < matrix2.getColHeader().length; ++j) {
                hits.add(new Hit(i, j, m2[i][j], true));
            }
        }
        Collections.sort(hits, Collections.reverseOrder());
        final int dbLength = matrix2.getRowHeader().length;
        final int decoyLength = matrix2.getColHeader().length;
        int decoys = 0;
        int k = 0;
        for (int i = 0; i < hits.size(); ++i) {
            if (hits.get(i).decoy) {
                ++decoys;
                // compute q values for all j=k..i
                final double qvalue = decoys / ((double) i - decoys);
                for (int j = k; j < i; ++j) {
                    final Hit h = hits.get(j);
                    if (!h.decoy) m1[h.left][h.right] = qvalue;
                }
                k = i + 1;
            }
        }
        final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(query.getParent(), "qvalues.csv")));
        writer.write("scores");
        for (int j = 0; j < matrix1.getColHeader().length; ++j) {
            writer.write(",");
            writer.write(matrix1.getColHeader()[j]);
        }
        writer.newLine();
        for (int i = 0; i < matrix1.getRowHeader().length; ++i) {
            writer.write(matrix1.getRowHeader()[i]);
            for (int j = 0; j < matrix1.getColHeader().length; ++j) {
                writer.write(",");
                writer.write(String.valueOf(m1[i][j]));
            }
            writer.newLine();
        }
        writer.close();
    }

    public static void ssps(String[] args) {
        final Interact I = Shell.withAlternative();
        final SSPSBasicOptions opts = CliFactory.parseArguments(SSPSBasicOptions.class, args);
        final EvalDB evalDB = new EvalDB(opts.getDataset());
        final List<Iterator<String[]>> templates = new ArrayList<Iterator<String[]>>();
        final List<Iterator<String[]>> others = new ArrayList<Iterator<String[]>>();
        final List<String> names = new ArrayList<String>();
        boolean std = true;
        I.sayln("Reading matrices");
        for (String p : evalDB.profiles()) {
            try {
                for (File f : evalDB.scoreMatrix(p).getParentFile().listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".csv") && !name.equalsIgnoreCase("decoymatrix.csv");
                    }
                })) {
                    final File ff = new File(f.getAbsoluteFile() + ".std");
                    templates.add(parseMatrix(ff.exists() ? ff : f));
                    if (f.getName().equals("matrix.csv")) names.add(p);
                    else {
                        final String suffix = f.getName().substring(0, f.getName().indexOf('.'));
                        names.add(p + "_" + suffix);
                    }
                    std &= ff.exists();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        for (String tanimoto : evalDB.fingerprints()) {
            try {
                final File f = evalDB.fingerprint(tanimoto);
                final File ff = new File(f.getAbsoluteFile() + ".std");
                std &= ff.exists();
                others.add(parseMatrix(ff.exists() ? ff : f));
                names.add(tanimoto);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        for (File score : evalDB.otherScores()) {
            try {
                final File ff = new File(score.getAbsoluteFile() + ".std");
                std &= ff.exists();
                others.add(parseMatrix(ff.exists() ? ff : score));
                names.add(score.getName().substring(0, score.getName().indexOf('.')));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        templates.addAll(others);
        final DoubleDataMatrix matrices;
        if (std) {
            System.out.println("Matrices already rearranged");
            matrices = DoubleDataMatrix.overlay(templates, null, names, null, Double.NaN);
            for (int i = 0; i < matrices.getValues().length; ++i) {
                final double[][] row = matrices.getValues()[i];
                for (int j = 0; j < row.length; ++j) {
                    for (int k = 0; k < row[j].length; ++k) {
                        if (Double.isNaN(row[j][k])) {
                            throw new RuntimeException("NaN in (" + i + "," + j + "," + k + ") " + names.get(i) + " : " + matrices.getRowHeader()[j] + matrices.getColHeader()[k]);
                        }
                    }
                }
            }
        } else {
            System.out.println("Rearrange matrices");
            matrices = DoubleDataMatrix.overlayIntersection(templates, names, null);
        }

        final Dataset dataset = new Dataset(new ScoreTable("Pubchem", matrices.getLayer("Pubchem")), opts.getK());
        /*
        I.sayln("Only allow compounds from different databases");
        int splitpoint = 0;
        char firstchar = matrices.getRowHeader()[0].charAt(0);
        for (String s : matrices.getRowHeader()) {
            if (s.charAt(0) != firstchar) break;
            ++splitpoint;
        }
        */

        for (int i = 0; i < matrices.getLayerHeader().length; ++i) {
            final String name = matrices.getLayerHeader()[i];
            if (!name.equals("Pubchem")) {
                final ScoreTable sc = new ScoreTable(name, matrices.getLayer(i));
                if (!opts.isNoFingerprint() && !name.equalsIgnoreCase("Pubchem") && !name.equalsIgnoreCase("MACCS") &&
                        !name.equalsIgnoreCase("Extended") && !name.equals("KlekotaRoth"))
                    sc.toFingerprints(opts.isSpearman());
                dataset.add(sc);
            }
        }
        if (!opts.isNoSSPS()) {
            // filter identical compounds
            I.sayln("Filterint identical compounds");
            dataset.filterOutIdenticalCompounds("Pubchem", "MACCS");

            //dataset.allowOnlyCompoundsFromDifferentDatasets();

            I.sayln("Calculating SSPS");
        } else I.sayln("Calculating Correlation");

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new FileWriter(opts.getTarget()));
            writer.append("k,opt,rand");
            for (int i = 0; i < matrices.getLayerHeader().length; ++i) {
                if (!matrices.getLayerHeader()[i].equals("Pubchem")) {
                    writer.append(",");
                    writer.append(matrices.getLayerHeader()[i]);
                }
            }
            writer.newLine();
            if (opts.isNoSSPS()) {
                int pubchemId = 0;
                final int MAXK = opts.getK();
                final double[][] results = new double[matrices.getLayerHeader().length][];
                for (int i = 0; i < matrices.getLayerHeader().length; ++i) {
                    if (matrices.getLayerHeader()[i].equals("Pubchem")) pubchemId = i;
                    final ScoreTable tab = dataset.getTable(matrices.getLayerHeader()[i]);//new ScoreTable(matrices.getLayerHeader()[i], matrices.getLayer(i));
                    results[i] = dataset.averageChemicalSimilarity(tab, MAXK);//dataset.averageChemicalSimilarityCrossDB(tab, MAXK, splitpoint);
                }
                for (int k = 0; k < MAXK; ++k) {
                    writer.append(String.valueOf(k + 1));
                    writer.append(',');
                    writer.append(String.valueOf(results[pubchemId][k]));
                    writer.append(',');
                    writer.append(String.valueOf(dataset.sspsAverageRandom()));
                    for (int i = 0; i < matrices.getLayerHeader().length; ++i) {
                        if (matrices.getLayerHeader()[i].equals("Pubchem")) continue;
                        else {
                            writer.append(",");
                            writer.append(String.valueOf(results[i][k]));
                        }
                    }
                    writer.newLine();
                    ;
                }
            } else {
                final int MAXK = opts.getK() * 10;
                for (int k = 1; k <= MAXK; ++k) {
                    I.say(".");
                    System.out.flush();
                    final float K = k / 10f;
                    writer.append(String.valueOf(K));
                    writer.append(",");
                    writer.append(String.valueOf(dataset.sspsOpt(K)));
                    writer.append(",");
                    writer.append(String.valueOf(dataset.sspsAverageRandom()));
                    for (int i = 0; i < matrices.getLayerHeader().length; ++i) {
                        if (matrices.getLayerHeader()[i].equals("Pubchem")) continue;
                        else {
                            writer.append(",");
                            final ScoreTable tab = dataset.getTable(matrices.getLayerHeader()[i]);//new ScoreTable(matrices.getLayerHeader()[i], matrices.getLayer(i));
                            writer.append(String.valueOf(dataset.ssps(tab, K)));
                        }
                    }
                    writer.newLine();
                    I.sayln("");
                }
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static class Hit implements Comparable<Hit> {
        private final boolean decoy;
        private final double score;
        private final int left, right;

        private Hit(int left, int right, double score, boolean decoy) {
            this.left = left;
            this.right = right;
            this.score = score;
            this.decoy = decoy;
        }

        @Override
        public int compareTo(Hit o) {
            return Double.compare(score, o.score);
        }
    }

}
