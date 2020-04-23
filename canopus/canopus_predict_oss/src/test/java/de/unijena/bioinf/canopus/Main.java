package de.unijena.bioinf.canopus;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main (String[] args) throws IOException {
        Canopus c = Canopus.loadFromFile(new File("/home/fleisch/workspace/sirius_frontend/sirius_cli/src/main/resources/canopus.data"));
        System.out.println();
    }

    /*private static float[] readVector(File f) throws IOException {
        final TFloatArrayList list = new TFloatArrayList();
        try (final BufferedReader br = KernelToNumpyConverter.getReader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                list.add(Float.parseFloat(line));
            }
        }
        return list.toArray();
    }

    public static void predictDataset(File fingeridFile, File canopusFile, File dir, boolean isos) throws IOException {
        final Canopus canopus = Canopus.loadFromFile(canopusFile);
        predictDataset(fingeridFile, canopus, dir, isos);
    }

    public static void predictDataset(File fingeridFile, Canopus canopus, File dir, boolean isos) throws IOException {
        final Prediction csi = Prediction.loadFromFile(fingeridFile);
        final Sirius sirius = new Sirius("qtof");
        if (isos) {
            sirius.getMs2Analyzer().setIsotopeHandling(FragmentationPatternAnalysis.IsotopeInMs2Handling.ALWAYS);
            final PeakScorer noIsotopePenalizer = new PeakScorer() {
                @Override
                public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
                    if (input.getAnnotation(MsInstrumentation.class, MsInstrumentation.Unknown).hasIsotopesInMs2()) {
                        for (int k = 0; k < peaks.size(); ++k) {
                            scores[k] -= 2 * peaks.get(k).getRelativeIntensity();
                        }
                    }
                }

                @Override
                public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

                }

                @Override
                public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

                }
            };
            sirius.getMs2Analyzer().getFragmentPeakScorers().add(noIsotopePenalizer);
        }
        final PrintStream output = new PrintStream(dir.getName() + "_results.csv");
        final PrintStream output2 = new PrintStream(dir.getName() + "_results_platt.csv");
        final PrintStream output3 = new PrintStream(dir.getName() + "_count.csv");
        final TObjectIntHashMap<String> counter = new TObjectIntHashMap<>(2000, 0.75f, 0);
        output.print("name");
        output2.print("name");
        for (String name : canopus.getKlassNames()) {
            output.print("\t" + name);
            output2.print("\t" + name);
        }
        output.print('\n');
        output2.print('\n');
        for (File ms : dir.listFiles()) {
            if (!ms.getName().endsWith(".ms")) continue;
            System.out.println("Process " + ms.getName());
            final Ms2Experiment experiment = MsIO.readExperimentFromFile(ms).next();
            final IdentificationResult r = sirius.compute(experiment, experiment.getMolecularFormula());
            SpectralPreprocessor.Preprocessed pre = SpectralPreprocessor.preprocess(sirius, r, experiment);
            final ProbabilityFingerprint fp = csi.predictProbabilityFingerprint(pre.spectrum, pre.tree, pre.precursorMz);
            final double[] platts = canopus.predictProbability(experiment.getMolecularFormula(), fp);
            final double[] decs = canopus.predictDecisionValues(experiment.getMolecularFormula(), fp, false);
            final List<String> names = canopus.getKlassNames();
            output.print(ms.getName());
            output2.print(ms.getName());
            for (int i = 0; i < platts.length; ++i) {
                output.print('\t');
                output2.print('\t');
                output.print(decs[i]);
                output2.print(platts[i]);
                if (platts[i] >= 0.5 || decs[i] >= 0) {
                    System.out.println(names.get(i) + ": \t" + decs[i] + "\t( " + platts[i] + " )");
                    counter.adjustOrPutValue(names.get(i), 1, 1);
                }
            }
            output.print('\n');
            output2.print('\n');
        }
        final double total;
        {
            int sum = 0;
            for (int val : counter.values()) sum += val;
            total = sum;
        }
        counter.forEachEntry(new TObjectIntProcedure<String>() {
            @Override
            public boolean execute(String a, int b) {
                output3.println(a + "\t" + b + "\t" + ((100d * b) / total));
                return true;
            }
        });

        csi.shutdown();
        output.close();
        output2.close();
        output3.close();

    }

    public static ArrayFingerprint getFp(FingerprintVersion v, String arrayAsString) throws SQLException {
        if (arrayAsString == null) throw new RuntimeException("NULL fingerprint. WTF?");
        final TShortArrayList indizes = new TShortArrayList(122);
        int k = 1;
        for (int i = 1; i < arrayAsString.length() - 1; ++i) {
            if (arrayAsString.charAt(i) == ',') {
                indizes.add(Short.parseShort(arrayAsString.substring(k, i)));
                k = i + 1;
            }
        }
        return new ArrayFingerprint(v, indizes.toArray());
    }

    public static ArrayFingerprint getFp2(FingerprintVersion v, Array array) throws SQLException {
        final ResultSet r = array.getResultSet();
        final TShortArrayList list = new TShortArrayList(122);
        while (r.next()) list.add(r.getShort(2));
        return new ArrayFingerprint(v, list.toArray());
    }

    *//**
     * Preprocess
     * parameter: path to csi fingerid
     *//*
    public static void main(String[] args) {
//        Canopus.main(args);
        //Sampler.main(args);
        //
        runDataset();
        System.exit(0);

        final File csiPath = new File(args[0]);

        // copy important files

        try {
            if (!new File("fingerid.data").exists())
                Files.createSymbolicLink(new File("fingerid.data").getAbsoluteFile().toPath(), new File(csiPath, "fingerid.data").getAbsoluteFile().toPath());
            final TrainedCSIFingerId trainedCSIFingerId = TrainedCSIFingerId.load(new File("fingerid.data"));
            System.out.println(trainedCSIFingerId.getFingerprintIndizes().length + " <-> " + trainedCSIFingerId.getMaskedFingerprintVersion().allowedIndizes().length);
            if (!new File("fingerprint_indizes.txt").exists())
                try (BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("fingerprint_indizes.txt"))) {
                    for (int index : trainedCSIFingerId.getFingerprintIndizes()) {
                        bw.write(String.valueOf(index));
                        bw.newLine();
                    }
                }
            if (!new File("fingerprints.mask").exists())
                Files.copy(new File(csiPath, "fingerprints/fingerprints.mask").getAbsoluteFile().toPath(), new File("fingerprints.mask").getAbsoluteFile().toPath());
            filterCsvFiles(trainedCSIFingerId, new File(csiPath, "predictions/crossvalidation.csv").getAbsoluteFile(), new File("crossvalidation.csv"));
            filterCsvFiles(trainedCSIFingerId, new File(csiPath, "predictions/independent.csv").getAbsoluteFile(), new File("independent.csv"));
            if (!new File("fingerprints.csv").exists())
                downloadFingerprints(trainedCSIFingerId);
            prepare(trainedCSIFingerId);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void runDataset() {
        try {
            predictDataset(new File("fingerid.data"), new File("canopus_1_fixed.data.gz"), new File("diatomes"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recomputeMissingTEMP(final TrainedCSIFingerId trainedCSIFingerId) throws IOException {
        final CdkFingerprintVersion cdk = (CdkFingerprintVersion) trainedCSIFingerId.getMaskedFingerprintVersion().getMaskedFingerprintVersion();
        try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("fingerprints_add2.csv"))) {
            final Fingerprinter fingerprinter = Fingerprinter.getFor(cdk);
            try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("missing_fp.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split("\t");
                    final String inchikey = cols[0];
                    final String formula = cols[1];
                    System.out.println(inchikey);
                    final boolean[] fp = new boolean[cols.length - 2];
                    for (int i = 2; i < cols.length; ++i) {
                        fp[i - 2] = (cols[i].equals("1"));
                    }
                    final ArrayFingerprint afp = trainedCSIFingerId.getMaskedFingerprintVersion().mask(fp).asArray();
                    bw.write(inchikey);
                    bw.write("\t");
                    bw.write(formula);
                    bw.write("\t");
                    bw.write(afp.toTabSeparatedString());
                    bw.newLine();
                }
            }
            try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("missing_smiles.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split("\t");
                    final String inchikey = cols[0];
                    final String inchi = cols[1];
                    System.out.println(inchikey);
                    try {
                        final IAtomContainer mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(inchi);
                        final BooleanFingerprint fp = new BooleanFingerprint(cdk, fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(mol)));
                        bw.write(inchikey);
                        bw.write("\t");
                        bw.write(MolecularFormulaManipulator.getHillString(MolecularFormulaManipulator.getMolecularFormula(mol)));
                        bw.write("\t");
                        bw.write(trainedCSIFingerId.getMaskedFingerprintVersion().mask(fp).toTabSeparatedString());
                        bw.newLine();
                    } catch (CDKException | IllegalArgumentException | NullPointerException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }

    public static void recomputeMissing(final TrainedCSIFingerId trainedCSIFingerId) throws IOException {
        final CdkFingerprintVersion cdk = (CdkFingerprintVersion) trainedCSIFingerId.getMaskedFingerprintVersion().getMaskedFingerprintVersion();
        try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("fingerprints_add.csv"))) {
            final Fingerprinter fingerprinter = Fingerprinter.getFor(cdk);
            try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("missing_inchis.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split("\t");
                    final String inchikey = cols[0];
                    final String inchi = cols[1];
                    try {
                        final IAtomContainer mol = InChIGeneratorFactory.getInstance().getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
                        final BooleanFingerprint fp = new BooleanFingerprint(cdk, fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(mol)));
                        bw.write(inchikey);
                        bw.write("\t");
                        bw.write(MolecularFormulaManipulator.getHillString(MolecularFormulaManipulator.getMolecularFormula(mol)));
                        bw.write("\t");
                        bw.write(trainedCSIFingerId.getMaskedFingerprintVersion().mask(fp).toTabSeparatedString());
                        bw.newLine();
                    } catch (CDKException | IllegalArgumentException | NullPointerException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("missing_smiles.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = line.split("\t");
                    final String inchikey = cols[0];
                    final String inchi = cols[1];
                    try {
                        final IAtomContainer mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(inchi);
                        final BooleanFingerprint fp = new BooleanFingerprint(cdk, fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(mol)));
                        bw.write(inchikey);
                        bw.write("\t");
                        bw.write(MolecularFormulaManipulator.getHillString(MolecularFormulaManipulator.getMolecularFormula(mol)));
                        bw.write("\t");
                        bw.write(trainedCSIFingerId.getMaskedFingerprintVersion().mask(fp).toTabSeparatedString());
                        bw.newLine();
                    } catch (CDKException | IllegalArgumentException | NullPointerException e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (CDKException e) {
            e.printStackTrace();
        }
    }

    public static void downloadFingerprints(final TrainedCSIFingerId trainedCSIFingerId) throws IOException {
        final HashSet<String> inchikeys = new HashSet<>();
        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("crossvalidation.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                inchikeys.add(line.split("\t", 3)[1].substring(0, 14));
            }
        }
        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("independent.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                inchikeys.add(line.split("\t", 3)[1].substring(0, 14));
            }
        }
        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("compounds.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                inchikeys.add(line.split("\t", 2)[0].substring(0, 14));
            }
        }

        final CdkFingerprintVersion cdk = CdkFingerprintVersion.getComplete();
        final MaskedFingerprintVersion mk = trainedCSIFingerId.getMaskedFingerprintVersion();
        try (final ChemicalDatabase db = new ChemicalDatabase()) {
            db.useConnection((pooled) -> {
                final Connection c = pooled.connection;
                final PreparedStatement st = c.prepareStatement("SELECT fingerprint, formula FROM fingerprints_tmp WHERE inchi_key_1 = ? AND fp_id = 1");
                try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("fingerprints.csv"))) {
                    final int total = inchikeys.size();
                    int missing = 0, found = 0, enumerated = 0;
                    for (String key : inchikeys) {
                        ++enumerated;
                        st.setString(1, key);
                        try (final ResultSet s = st.executeQuery()) {
                            if (s.next()) {
                                final String formula = s.getString(2);
                                final String arrayAsString = s.getString(1);
                                final ArrayFingerprint fp;
                                if (arrayAsString == null) {
                                    fp = mk.mask(getFp2(cdk, s.getArray(1)));
                                } else {
                                    fp = mk.mask(getFp(cdk, arrayAsString));
                                }
                                bw.write(key);
                                bw.write('\t');
                                bw.write(formula);
                                bw.write('\t');
                                bw.write(fp.toTabSeparatedString());
                                bw.newLine();
                                ++found;
                            } else {
                                ++missing;
                            }
                        }
                        if (enumerated % 10000 == 0) {
                            System.out.println("Progress: " + ((enumerated * 100d) / total) + " %");
                            System.out.println("Found: " + found + ", missing: " + missing);
                        }
                    }

                }

                return null;
            });
        } catch (ChemicalDatabaseException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public static void prepare(TrainedCSIFingerId trainedCSIFingerId) throws IOException {
        final HashSet<MolecularFormula> formulas = new HashSet<>();
        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("fingerprints.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] parts = line.split("\t", 3);
                final MolecularFormula formula = MolecularFormula.parse(parts[1]);
                formulas.add(formula);
            }
        }
        final FormulaConstraints constraints = new FormulaConstraints("CHNOPSClBrBSeIF");
        final Iterator<MolecularFormula> fiter = formulas.iterator();
        while (fiter.hasNext()) {
            final MolecularFormula f = fiter.next();
            if (f.getMass() > 2000)
                fiter.remove();
            else if (constraints.isViolated(f))
                fiter.remove();
            else if (f.numberOfCarbons() == 0 || f.numberOfHydrogens() == 0)
                fiter.remove();
            else if (f.rdbe() <= -1)
                fiter.remove();
        }
        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("crossvalidation.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] parts = line.split("\t", 4);
                final MolecularFormula formula = InChIs.newInChI(parts[1], parts[2]).extractFormula();
                formulas.add(formula);
            }
        }

        try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("independent.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] parts = line.split("\t", 4);
                final MolecularFormula formula = InChIs.newInChI(parts[1], parts[2]).extractFormula();
                formulas.add(formula);
            }
        }
        final PeriodicTable t = PeriodicTable.getInstance();
        final Element[] elements = new Element[]{
                t.getByName("C"),
                t.getByName("H"),
                t.getByName("N"),
                t.getByName("O"),
                t.getByName("P"),
                t.getByName("S"),
                t.getByName("Br"),
                t.getByName("Cl"),
                t.getByName("F"),
                t.getByName("I"),
                t.getByName("B"),
                t.getByName("Se")
        };
        final ArrayList<double[]> values = new ArrayList<>();
        try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("formula_features.csv"))) {
            for (MolecularFormula f : formulas) {
                final double[] val = new double[elements.length + 4];
                int K = 0;
                bw.write(f.toString());
                for (Element e : elements) {
                    bw.write('\t');
                    bw.write(String.valueOf(f.numberOf(e)));
                    val[K++] = f.numberOf(e);
                }
                bw.write('\t');
                bw.write(String.valueOf(f.rdbe()));
                val[K++] = f.rdbe();
                bw.write('\t');
                bw.write(String.valueOf(f.getMass()));
                val[K++] = f.getMass();

                // add RDBE==0
                bw.write('\t');
                val[K++] = f.rdbe() == 0 ? 1 : -1;
                bw.write(String.valueOf(val[K - 1]));
                // add  RDBE/(mass/100.0)^(2/3)
                bw.write('\t');
                val[K++] = (Math.min(0, f.rdbe()) + 1) / Math.pow(f.getMass() / 100.0, 2d / 3d);
                bw.write(String.valueOf(val[K - 1]));
                bw.newLine();
                values.add(val);
            }
        }
        final double[][] MATRIX = values.toArray(new double[values.size()][]);
        final double[][] scaling = FormulaFeatureVector.normalizeAndCenter(MATRIX);
        new KernelToNumpyConverter().writeToFile(new File("formula_normalized.txt"), scaling);

    }


    private static void filterCsvFiles(TrainedCSIFingerId fingerid, File from, File to) {
        if (to.exists()) return;
        final Mask m;
        try {
            m = Mask.fromString(com.google.common.io.Files.readFirstLine(new File("fingerprints.mask"), Charset.forName("UTF-8")).split("\t"));
            final FingerprintVersion version = fingerid.getMaskedFingerprintVersion().getMaskedFingerprintVersion();
            final MaskedFingerprintVersion masked = fingerid.getMaskedFingerprintVersion();

            final int expectedSizeAfterRemovingDuplicates = m.usedIndizes().length;
            final int expectedSizeAfterMask = masked.size();

            if (from.getName().endsWith(".csv")) {
                try (BufferedWriter bw = KernelToNumpyConverter.getWriter(to)) {
                    for (String line : com.google.common.io.Files.readLines(from, Charset.forName("UTF-8"))) {
                        String[] tabs = line.split("\t");

                        bw.write(tabs[0]);
                        bw.write('\t');
                        bw.write(tabs[1]);
                        bw.write('\t');
                        bw.write(tabs[2]);
                        bw.write('\t');
                        {
                            String fingerprint = tabs[3];
                            boolean[] fp = new boolean[fingerprint.length()];
                            for (int i = 0; i < fp.length; ++i)
                                if (fingerprint.charAt(i) == '1')
                                    fp[i] = true;


                            if (fp.length == expectedSizeAfterMask) {
                                final BooleanFingerprint maskedFp = new BooleanFingerprint(masked, fp);
                                bw.write(maskedFp.toOneZeroString());
                            } else if (fp.length == expectedSizeAfterRemovingDuplicates) {
                                boolean[] fp2 = m.unapply(fp);
                                final BooleanFingerprint fp3 = new BooleanFingerprint(version, fp2);
                                final BooleanFingerprint maskedFp = masked.mask(fp3);
                                bw.write(maskedFp.toOneZeroString());
                            } else {
                                throw new IllegalArgumentException("Unexpected size of fingerprint: " + fp.length + " given, but should be either " + expectedSizeAfterRemovingDuplicates + " after removing duplicates or " + expectedSizeAfterMask + " after removing unpredictable fingerprints");
                            }
                        }

                        {
                            double[] fp = new double[tabs.length - 4];
                            for (int k = 4; k < tabs.length; ++k) fp[k - 4] = Double.parseDouble(tabs[k]);

                            if (fp.length == expectedSizeAfterMask) {
                                ProbabilityFingerprint fp2 = new ProbabilityFingerprint(masked, fp);
                                bw.write('\t');
                                bw.write(fp2.toTabSeparatedString());
                                bw.write('\n');
                            } else if (fp.length == expectedSizeAfterRemovingDuplicates) {
                                fp = m.unapply(fp);
                                ProbabilityFingerprint fp2 = new ProbabilityFingerprint(version, fp);
                                fp2 = masked.mask(fp2);
                                bw.write('\t');
                                bw.write(fp2.toTabSeparatedString());
                                bw.write('\n');
                            } else {
                                throw new IllegalArgumentException("Unexpected size of fingerprint: " + fp.length + " given, but should be either " + expectedSizeAfterRemovingDuplicates + " after removing duplicates or " + expectedSizeAfterMask + " after removing unpredictable fingerprints");
                            }
                        }
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }*/

}
