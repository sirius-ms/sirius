package de.unijena.bioinf.fingerid.fingerprints;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.graph.matrix.TopologicalMatrix;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeighbourhoodFingerprintUtils {

    private IChemObjectBuilder DEF = DefaultChemObjectBuilder.getInstance();

    public static void main(String[] args) {
        //findSubstructures(null);
        try {
            getAverageTanimoto();
        } catch (CDKException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getAverageTanimoto() throws CDKException, IOException {
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final List<IAtomContainer> molecules = new ArrayList<>();
        final List<short[]> fingerprints = new ArrayList<>();
        final List<String> inchikeys = new ArrayList<>();
        for (File f : new File("D:/arbeit/daten/fingerid/unique_fingerprints").listFiles()) {
            if (f.getName().endsWith(".fpt")) {
                try {
                    final String key = f.getName().substring(0,14);
                    inchikeys.add(key);
                    final String i = Files.readAllLines(new File("inchis", key + ".inchi").toPath(), Charset.defaultCharset()).get(0);
                    molecules.add(factory.getInChIToStructure(i,DefaultChemObjectBuilder.getInstance()).getAtomContainer());
                    final String r = Files.readAllLines(f.toPath(), Charset.defaultCharset()).get(0);
                    final TShortArrayList list = new TShortArrayList();
                    for (int k=0; k < r.length(); ++k) {
                        if (r.charAt(k)=='1') list.add((short)k);
                    }
                    fingerprints.add(list.toArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("fertig mit einlesen");
        final double[][] tanimoto = new double[molecules.size()][molecules.size()];
        double average = 0d;
        for (int i=0; i < fingerprints.size(); ++i) {
            average += 1d;
            tanimoto[i][i] = 1d;
            for (int j=0; j < i; ++j) {
                final double t = tanimoto(fingerprints.get(i), fingerprints.get(j));
                average += 2*t;
                tanimoto[i][j] = tanimoto[j][i] = t;
            }
        }
        average /= (fingerprints.size()*fingerprints.size());
        System.out.println("Average tanimoto: " + average);
        final List<String> smiles = Files.readAllLines(new File("substructures.tsv").toPath(), Charset.defaultCharset());
        final double[] avgTanimoto = new double[smiles.size()];
        final TIntArrayList[] ids = new TIntArrayList[smiles.size()];
        for (int k=0; k < ids.length; ++k) ids[k] = new TIntArrayList();
        final SMARTSQueryTool tool = new SMARTSQueryTool("C=C", DefaultChemObjectBuilder.getInstance());
        Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
        tool.setAromaticity(aroma);
        for (int k=0; k < molecules.size(); ++k) {
            final IAtomContainer mol = molecules.get(k);
            SmartsMatchers.prepare(mol, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            aroma.apply(mol);
            for (int s=0; s < smiles.size(); ++s) {
                tool.setSmarts(smiles.get(s));
                if (tool.matches(mol)) {
                    ids[s].add(k);
                }
            }
            System.out.print("."); System.out.flush();
            if (k % 50 == 0) System.out.println("");
        }

        try (final BufferedWriter bw = Files.newBufferedWriter(new File("substructures_tanimoto.tsv").toPath(), Charset.defaultCharset())) {
            for (int k=0; k < smiles.size(); ++k) {
                final int[] havit = ids[k].toArray();
                double tan = 0d;
                for (int i : havit) {
                    for (int j : havit) {
                        tan += tanimoto[i][j];
                    }
                }
                tan /= (havit.length*havit.length);
                bw.write(smiles.get(k));
                bw.write('\t');
                bw.write(String.valueOf(havit.length));
                bw.write('\t');
                bw.write(String.valueOf(tan));
                for (int id : havit) {
                    bw.write('\t');
                    bw.write(inchikeys.get(id));
                }
                bw.write('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double tanimoto(short[] a, short[] b) {
        int i=0; int j=0;
        int shared=0;
        int union=0;
        while (i < a.length && j < b.length) {
            if (a[i]==b[j]) {
                ++shared;
                ++union;
                ++i; ++j;
            } else if (a[i] < b[j]) {
                ++union;
                ++i;
            } else {
                ++union;
                ++j;
            }
        }
        return ((double)shared)/((double)union);
    }

    public static void findSubstructures(List<String> inchis) {
        // now generate substructures
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter("substructures.tsv"))){
            final Map<String, Integer> structures = new NeighbourhoodFingerprintUtils().generateSubstructuresFor(inchis);
            final Iterator<Map.Entry<String,Integer>> iter = structures.entrySet().iterator();
            final int MIN_COUNT = (int)Math.ceil(inchis.size()*0.01);
            final int MAX_COUNT = (int)Math.ceil(inchis.size()*0.2);
            while (iter.hasNext()) {
                final Map.Entry<String,Integer> entry=iter.next();
                final int count = entry.getValue();
                if (count < MIN_COUNT  || count > MAX_COUNT) iter.remove();
                else bw.write(entry.getKey() + "\t" + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }

    public void compute(String smartsFile, String inchiFile) throws IOException, CDKException {
        final ArrayList<String> smiles = new ArrayList<>();
        {
            final BufferedReader r = new BufferedReader(new FileReader(smartsFile));
            String line=null;
            while ((line=r.readLine())!=null) {
                if (line.contains("Si") || line.contains("Se") || line.contains("+") || line.contains("-")) continue;
                smiles.add(line);
            }
            r.close();
        }
        final ArrayList<IAtomContainer> molecules = new ArrayList<>();
        final BufferedReader r = new BufferedReader(new FileReader(inchiFile));
        final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        final SmilesGenerator gen = new SmilesGenerator().aromatic();
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final String[] smarts = new String[smiles.size()];
        final SMARTSQueryTool tool = new SMARTSQueryTool("cc", DEF);
        for (int k=0; k < smiles.size(); ++k) {
            smarts[k] = smiles.get(k);
        }
        System.out.println(smiles.size());
        String line=null;
        int inchiline = -1;
        while ((line=r.readLine())!=null) {
            if (inchiline<0) {
                inchiline=0;
                for (int k=0; k < line.length(); ++k) {
                    if (line.charAt(k)=='\t') ++inchiline;
                    if (line.startsWith("InChI=", k)) break;
                }
            }
            final String inchi = inchi2d(line.split("\t")[inchiline]);
            final String formula = inchi.split("/")[inchiline];
            if (formula.contains("Se") || formula.contains("Si")) continue;
            final IAtomContainer mol = factory.getInChIToStructure(inchi, DEF).getAtomContainer();
            SmartsMatchers.prepare(mol, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(mol);
            molecules.add(mol);
        }

        final int[][] matrix = new int[molecules.size()][smarts.length];

        for (int k=0; k < molecules.size(); ++k) {
            for (int i=0; i < smarts.length; ++i) {
                tool.setSmarts(smarts[i]);
                if (tool.matches(molecules.get(k)))
                    matrix[k][i] = tool.countMatches();
            }
            System.out.println(k);
        }

        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("matrix.csv"));
            for (int k=0; k < molecules.size(); ++k) {
                for (int i=0; i < smarts.length; ++i) {
                    bw.write(String.valueOf(Math.min(9, matrix[k][i])));
                }
                bw.write('\n');
            }
            bw.close();
        }

        // search redundant rows

        //final Mask mask = Mask.compute(matrix);

        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("used_smarts.csv"));
            for (String s : smiles) {
                bw.write(s);
                bw.write('\n');
            }
            bw.close();
        }
        /*
        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("mask.csv"));
            bw.write(mask.toString());
            bw.close();
        }
        */

    }


    public void compute2(String smartsFile, String inchiFile) throws IOException, CDKException {
        final ArrayList<String> smiles = new ArrayList<>();
        {
            final BufferedReader r = new BufferedReader(new FileReader(smartsFile));
            String line=null;
            while ((line=r.readLine())!=null) {
                if (line.contains("Si") || line.contains("Se") || line.contains("+") || line.contains("-")) continue;
                smiles.add(line);
            }
            r.close();
        }
        final ArrayList<IAtomContainer> molecules = new ArrayList<>();
        final BufferedReader r = new BufferedReader(new FileReader(inchiFile));
        final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        final SmilesGenerator gen = new SmilesGenerator().aromatic();
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final String[] smarts = new String[smiles.size()];
        final SMARTSQueryTool tool = new SMARTSQueryTool("cc", DEF);
        for (int k=0; k < smiles.size(); ++k) {
            smarts[k] = smiles.get(k);
        }
        System.out.println(smiles.size());
        String line=null;
        int inchiline = -1;
        while ((line=r.readLine())!=null) {
            if (inchiline<0) {
                inchiline=0;
                for (int k=0; k < line.length(); ++k) {
                    if (line.charAt(k)=='\t') ++inchiline;
                    if (line.startsWith("InChI=", k)) break;
                }
            }
            final String inchi = inchi2d(line.split("\t")[inchiline]);
            final String formula = inchi.split("/")[inchiline];
            if (formula.contains("Se") || formula.contains("Si")) continue;
            final IAtomContainer mol = factory.getInChIToStructure(inchi, DEF).getAtomContainer();
            SmartsMatchers.prepare(mol, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(mol);
            molecules.add(mol);
        }

        final int[][] matrix = new int[molecules.size()][];

        for (int k=0; k < molecules.size(); ++k) {
            final int[] bits = findPaths(molecules.get(k));
            matrix[k] = bits;
        }

        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("matrix.csv"));
            for (int k=0; k < molecules.size(); ++k) {
                for (int i=0; i < matrix[k].length; ++i) {
                    if (matrix[k][i] > 1000) bw.write("-");
                    else bw.write(String.valueOf(matrix[k][i]));
                    if (i < matrix[k].length-1) bw.write('\t');
                }
                bw.write('\n');
            }
            bw.close();
        }

        // search redundant rows

        //final Mask mask = Mask.compute(matrix);

        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("used_smarts.csv"));
            for (String s : smiles) {
                bw.write(s);
                bw.write('\n');
            }
            bw.close();
        }
        /*
        {
            final BufferedWriter bw = new BufferedWriter(new FileWriter("mask.csv"));
            bw.write(mask.toString());
            bw.close();
        }
        */

    }

    public void count(String smartsFile, String inchiFile) throws IOException, CDKException {
        final BufferedReader r = new BufferedReader(new FileReader(smartsFile));
        String line=null;
        final ArrayList<String> smiles = new ArrayList<>();
        while ((line=r.readLine())!=null) {
            if (line.contains("Si") || line.contains("Se") || line.contains("+") || line.contains("-]")) continue;
            smiles.add(line);
        }
        r.close();
        calculateFrequencies(new File(inchiFile), null, smiles, new File("counting.csv"));

    }

    public void freq(String smartsFile, String inchiFile, String elem) throws IOException, CDKException {
        final BufferedReader r = new BufferedReader(new FileReader(smartsFile));
        String line=null;
        final ArrayList<String> smilesCl = new ArrayList<>(), smilesBr = new ArrayList<>(), smilesI = new ArrayList<>(),
                smilesF = new ArrayList<>(), smilesP = new ArrayList<>(), smilesS=new ArrayList<>(), smilesChno = new ArrayList<>();
        while ((line=r.readLine())!=null) {
            if (line.contains("Si") || line.contains("Se") || line.contains("+") || line.contains("-")) continue;
            boolean halogen=false;
            if (line.contains("Cl")) {
                smilesCl.add(line);
                halogen=true;
            }
            if (line.contains("F") || line.contains("f")) {
                smilesF.add(line);
                halogen=true;
            }
            if (line.contains("Br") || line.contains("br")) {
                smilesBr.add(line);
                halogen=true;
            }
            if (line.contains("I") || (line.contains("i")) ) {
                smilesI.add(line);
                halogen=true;
            }
            if (line.contains("P") || line.contains("p")) {
                smilesP.add(line);
            }
            if (line.contains("S") || line.contains("s")) {
                smilesS.add(line);
            }
            if (!halogen)
                smilesChno.add(line);
        }
        r.close();
        if (elem == null || elem.equalsIgnoreCase("Br"))
            calculateFrequencies(new File(inchiFile), "Br", smilesBr, new File("freq_Br.csv"));
        if (elem == null || elem.equalsIgnoreCase("I"))
            calculateFrequencies(new File(inchiFile), "I", smilesI, new File("freq_I.csv"));
        if (elem == null || elem.equalsIgnoreCase("F"))
            calculateFrequencies(new File(inchiFile), "F", smilesF, new File("freq_F.csv"));
        if (elem == null || elem.equalsIgnoreCase("Cl"))
            calculateFrequencies(new File(inchiFile), "Cl", smilesCl, new File("freq_Cl.csv"));
        if (elem == null || elem.equalsIgnoreCase("P"))
            calculateFrequencies(new File(inchiFile), "P", smilesP, new File("freq_P.csv"));
        if (elem == null || elem.equalsIgnoreCase("S"))
            calculateFrequencies(new File(inchiFile), "S", smilesS, new File("freq_S.csv"));
        if (elem == null || elem.equalsIgnoreCase("all"))
            calculateFrequencies(new File(inchiFile), null, smilesChno, new File("freq_all.csv"));

    }

    public void calculateFrequencies(File inchiFile, String element, List<String> smiles, File target) throws IOException, CDKException {
        final BufferedReader r = new BufferedReader(new FileReader(inchiFile));
        final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        final SmilesGenerator gen = new SmilesGenerator().aromatic();
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final SMARTSQueryTool tool = new SMARTSQueryTool("C", DEF);
        System.out.println(smiles.size());
        final int[] frequencies = new int[smiles.size()];
        int counter = 0;
        String line=null;
        while ((line=r.readLine())!=null) {
            final String inchi = inchi2d(line.split("\t")[3]);
            final String formula = inchi.split("/")[1];
            if (formula.contains("Se") || formula.contains("Si")) continue;
            if (element!=null && !formula.contains(element)) continue;
            final IAtomContainer mol = factory.getInChIToStructure(inchi, DEF).getAtomContainer();
            SmartsMatchers.prepare(mol, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(mol);
            ++counter;
            for (int k=0; k < smiles.size(); ++k) {
                tool.setSmarts(smiles.get(k));
                if (tool.matches(mol)) {
                    ++frequencies[k];
                }
            }
            if ((counter % 100)==0) {
                System.out.println(counter);
            }
        }
        final BufferedWriter bw = new BufferedWriter(new FileWriter(target));
        for (int k=0; k < smiles.size(); ++k) {
            final double f = ((double)frequencies[k])/counter;
            bw.write(smiles.get(k) + "\t" + frequencies[k] + "\t" + f + "\n");
        }
        bw.close();
    }

    public static HashMap<String, Integer> calculateFrequencies(List<String> inchis, final List<String> smarts) throws IOException, CDKException {
        Collections.sort(smarts, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.compare(o1.length(), o2.length());
            }
        });
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final ConcurrentHashMap<String, Integer> counter = new ConcurrentHashMap<>(smarts.size());
        String line=null;
        for (final String inchi : inchis) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println(inchi);
                        final SMARTSQueryTool tool = new SMARTSQueryTool("C", DefaultChemObjectBuilder.getInstance());
                        final IAtomContainer mol = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
                        SmartsMatchers.prepare(mol, true);
                        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                        Aromaticity aroma = new Aromaticity(ElectronDonation.daylight(),
                                Cycles.allOrVertexShort());
                        aroma.apply(mol);
                        for (int k=0; k < smarts.size(); ++k) {
                            final String smart = smarts.get(k);
                            tool.setSmarts(smart);
                            if (tool.matches(mol)) {
                                boolean done;
                                do {
                                    final Integer c = counter.get(smart);
                                    if (c==null) {
                                        done = counter.putIfAbsent(smart, 1) == null;
                                    } else {
                                        done = counter.replace(smart, c, c+1);
                                    }
                                } while (!done);
                            }
                        }
                    } catch (CDKException e) {
                        e.printStackTrace();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        try {
            service.shutdown();
            service.awaitTermination(10, TimeUnit.HOURS);
            return new HashMap<>(counter);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Set<String> generateSubstructures2For(List<String> inchis) throws CDKException {
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final HashSet<String> structs = new HashSet<>();
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.println(inchis.size());
        for (final String inchi : inchis) {
            Future f = service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        IAtomContainer mol = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
                        SmartsMatchers.prepare(mol, false);
                        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                        Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
                        aroma.apply(mol);
                        final SmartsBuilder builder = new SmartsBuilder(mol);
                        structs.addAll(builder.buildRadialSmarts2());
                        System.out.println(inchi + " :::: " + structs.size());
                    } catch (CDKException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        service.shutdown();
        try {
            service.awaitTermination(8, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return structs;
    }

    public Map<String, Integer> generateSubstructuresFor(List<String> inchis) throws CDKException {
        final ConcurrentHashMap<String, Integer> counter = new ConcurrentHashMap<>();
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        final SmilesGenerator generator = SmilesGenerator.unique().aromatic();
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (final String inchi : inchis) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        IAtomContainer mol = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
                        SmartsMatchers.prepare(mol, false);
                        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                        Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
                        aroma.apply(mol);
                        for (IAtom a : mol.atoms()) {
                            final List<IAtomContainer> allthem = createSubStructs(createSubMolecule(mol, a), a);
                            for (IAtomContainer oneofthem : allthem) {
                                final String smart = generator.create(oneofthem);
                                if (smart.isEmpty()) continue;
                                boolean done;
                                do {
                                    final Integer c = counter.get(smart);
                                    if (c==null) {
                                        done = counter.putIfAbsent(smart, 1) == null;
                                    } else {
                                        done = counter.replace(smart, c, c+1);
                                    }
                                } while (!done);
                            }
                        }
                    } catch (CDKException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(8, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return counter;
    }

    public void run(String filename, String target) throws IOException, CDKException {

        final BufferedReader r = new BufferedReader(new FileReader(filename));
        final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        final SmilesGenerator gen = new SmilesGenerator().aromatic();
        final InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        String line=null;
        final HashSet<String> smiles = new HashSet<>();
        int c=0;
        while ((line=r.readLine())!=null) {
            try {
                final String inchi = inchi2d(line.split("\t")[3]);
                final IAtomContainer mol = factory.getInChIToStructure(inchi, DEF).getAtomContainer();
                SmartsMatchers.prepare(mol, true);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
                aroma.apply(mol);
                for (IAtom a : mol.atoms()) {
                    final List<IAtomContainer> allthem = createSubStructs(createSubMolecule(mol, a), a);
                    for (IAtomContainer oneofthem : allthem) {
                        final String smart = gen.create(oneofthem);
                        if (smart.isEmpty()) continue;
                        if (!smiles.contains(smart)) {
                            final SMARTSQueryTool tool = new SMARTSQueryTool(smart, DEF);
                            if (tool.matches(mol)) smiles.add(smart);
                            else System.err.println(smart);
                        }
                    }
                }
                System.out.println(++c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final BufferedWriter fw = new BufferedWriter(new FileWriter(new File(target)));
        for (String s : smiles) {
            fw.write(s);
            fw.write('\n');
        }
        fw.close();
    }

    private List<IAtomContainer> createSubStructs(IAtomContainer mol, IAtom center) {
        final ShortestPaths sp = new ShortestPaths(mol, center);
        final ArrayList<IAtomContainer> output = new ArrayList<>();
        final ArrayDeque<IAtomContainer> todo = new ArrayDeque<>();
        todo.add(mol);
        while (!todo.isEmpty()) {
            final IAtomContainer m = todo.pollLast();
            int max =0;
            for (IAtom a : m.atoms()) {
                max = Math.max(max, sp.distanceTo(a));
            }
            for (IAtom a : m.atoms()) {
                if (sp.distanceTo(a)==max) {
                    final IAtomContainer newMol = cloneWithout(m,a);
                    output.add(newMol);
                    if (newMol.getAtomCount() > 0)
                        todo.add(newMol);
                }
            }
        }
        return output;
    }


    private IAtomContainer cloneWithout(IAtomContainer mol, IAtom todel) {
        final IAtomContainer newMol = DEF.newInstance(IAtomContainer.class);
        for (IAtom atom : mol.atoms()) {
            if (atom != todel) {
                newMol.addAtom(atom);
            }
        }
        for (IBond b : mol.bonds()) {
            if (!b.contains(todel)) {
                newMol.addBond(b);
            }
        }
        return newMol;
    }

    private IAtomContainer createSubMolecule(IAtomContainer molecule, IAtom center) {
        final IAtomContainer m = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
        PathTools.breadthFirstSearch(molecule, Collections.singletonList(center), m, 8);
        final ArrayList<IBond> todel = new ArrayList<>();
        for (IBond b : m.bonds()) {
            if (!(m.contains(b.getAtom(0)) && m.contains(b.getAtom(1)))){
                todel.add(b);
            }
        }
        for (IBond b : todel) {
            m.removeBond(b);
        }
        resetFlags(molecule);
        return m;
    }

    private static void resetFlags(IAtomContainer atomContainer) {
        for (int f = 0; f < atomContainer.getAtomCount(); f++) {
            atomContainer.getAtom(f).setFlag(CDKConstants.VISITED, false);
        }
        for (int f = 0; f < atomContainer.getBondCount(); f++) {
            atomContainer.getBond(f).setFlag(CDKConstants.VISITED, false);
        }

    }

    private static Pattern inchi2dPattern = Pattern.compile("/[btmrsfi]");
    private static String inchi2d(String inchi) {
        final Matcher m = inchi2dPattern.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }



    public int[] findShortestPaths(IAtomContainer atomContainer) throws CDKException {
        final int[][] matrix = TopologicalMatrix.getMatrix(atomContainer);
        final TIntArrayList bits = new TIntArrayList();
        {
            SmartsMatchers.prepare(atomContainer, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(atomContainer);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(atomContainer);
        }

        final SMARTSQueryTool tool = new SMARTSQueryTool("cc", DefaultChemObjectBuilder.getInstance());
        final TIntArrayList leftu = new TIntArrayList();
        final TIntArrayList rightu = new TIntArrayList();
        int c=0;
        for (int i = 0; i < ShortestPathFingerprinter.smartstrings.length; i++) {
            for (int j = i+1; j < ShortestPathFingerprinter.smartstrings.length; j++) {
                tool.setSmarts(ShortestPathFingerprinter.smartstrings[i]);
                if (tool.matches(atomContainer)) {
                    List<List<Integer>> left = tool.getUniqueMatchingAtoms();
                    // get unique atoms
                    for (List<Integer> atl : left) {
                        for (int ei : atl) {
                            if (atomContainer.getAtom(ei).getAtomicNumber()==ShortestPathFingerprinter.atomicnumbers[i]) {
                                leftu.add(ei);
                                break;
                            }
                        }
                    }
                    tool.setSmarts(ShortestPathFingerprinter.smartstrings[j]);
                    if (tool.matches(atomContainer)) {
                        List<List<Integer>> right = tool.getUniqueMatchingAtoms();
                        for (List<Integer> atl : right) {
                            for (int ei : atl) {
                                if (atomContainer.getAtom(ei).getAtomicNumber()==ShortestPathFingerprinter.atomicnumbers[j]) {
                                    rightu.add(ei);
                                    break;
                                }
                            }
                        }
                    }
                }
                int shortestPath = Integer.MAX_VALUE;
                for (int x=0; x < leftu.size(); ++x) {
                    for (int y=0; y < rightu.size(); ++y) {
                        if (leftu.get(x)!=rightu.get(y)) {
                            shortestPath = Math.min(shortestPath, matrix[leftu.get(x)][rightu.get(y)]);
                        }
                    }
                }
                bits.add(shortestPath);
                leftu.clear();
                rightu.clear();
            }
        }
        return bits.toArray();
    }



    public int[] findPaths(IAtomContainer atomContainer) throws CDKException {
        final int[][] matrix = TopologicalMatrix.getMatrix(atomContainer);
        final TIntArrayList bits = new TIntArrayList();
        {
            SmartsMatchers.prepare(atomContainer, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(atomContainer);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(atomContainer);
        }

        final SMARTSQueryTool tool = new SMARTSQueryTool("cc", DefaultChemObjectBuilder.getInstance());
        final TIntArrayList leftu = new TIntArrayList();
        final TIntArrayList rightu = new TIntArrayList();
        int c=0;
        for (int i = 0; i < ShortestPathFingerprinter.smartstrings.length; i++) {
            for (int j = i+1; j < ShortestPathFingerprinter.smartstrings.length; j++) {
                tool.setSmarts(ShortestPathFingerprinter.smartstrings[i]);
                if (tool.matches(atomContainer)) {
                    List<List<Integer>> left = tool.getUniqueMatchingAtoms();
                    // get unique atoms
                    for (List<Integer> atl : left) {
                        for (int ei : atl) {
                            if (atomContainer.getAtom(ei).getAtomicNumber()==ShortestPathFingerprinter.atomicnumbers[i]) {
                                leftu.add(ei);
                                break;
                            }
                        }
                    }
                    tool.setSmarts(ShortestPathFingerprinter.smartstrings[j]);
                    if (tool.matches(atomContainer)) {
                        List<List<Integer>> right = tool.getUniqueMatchingAtoms();
                        for (List<Integer> atl : right) {
                            for (int ei : atl) {
                                if (atomContainer.getAtom(ei).getAtomicNumber()==ShortestPathFingerprinter.atomicnumbers[j]) {
                                    rightu.add(ei);
                                    break;
                                }
                            }
                        }
                    }
                }
                leftu.sort();
                rightu.sort();
                final int[] paths = new int[30];
                for (int x=0; x < leftu.size(); ++x) {
                    if (x > 0 && leftu.get(x)==leftu.get(x-1)) continue;
                    for (int y=0; y < rightu.size(); ++y) {
                        if (y > 0 && rightu.get(y)==rightu.get(y-1)) continue;
                        if (leftu.get(x)!=rightu.get(y)) {
                            paths[matrix[leftu.get(x)][rightu.get(y)]] = 1;
                        }
                    }
                }
                bits.addAll(paths);
                leftu.clear();
                rightu.clear();
            }
        }
        return bits.toArray();
    }



}
