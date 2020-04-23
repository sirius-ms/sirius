package de.unijena.bioinf.canopus;

import com.google.common.io.Files;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.*;
import org.openscience.cdk.exception.CDKException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class Prepare {

    public static final MaskedFingerprintVersion CDK_MASK = new FormulaBits().removeFormulaBits(CdkFingerprintVersion.getComplete().getMaskFor(CdkFingerprintVersion.USED_FINGERPRINTS.SUBSTRUCTURE, CdkFingerprintVersion.USED_FINGERPRINTS.MACCS, CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM));

    public static void trainableFpPerformance(TrainedCSIFingerId fingerid, File csiPath) {
        try {

            final HashMap<Integer, PredictionPerformance.Modify> performances = new HashMap<>();
            final int[] indizes = CDK_MASK.allowedIndizes();
            for (int index : indizes) performances.put(index, new PredictionPerformance(0,0,0,0,0,false).modify());

            final MaskedFingerprintVersion masked = fingerid.getMaskedFingerprintVersion();

            final MaskedFingerprintVersion JOINED = masked.getIntersection(CDK_MASK);
            final int[] joinedIndizes = JOINED.allowedIndizes();

            {
                final File f = new File(csiPath, "prediction_prediction.csv");
                for (String line : Files.readLines(f, Charset.forName("UTF-8"))) {
                    String[] tabs = line.split("\t");
                    {
                        String fingerprint = tabs[3];
                        boolean[] fp = new boolean[fingerprint.length()];
                        for (int i = 0; i < fp.length; ++i)
                            if (fingerprint.charAt(i) == '1')
                                fp[i] = true;
                        final BooleanFingerprint maskedFp = new BooleanFingerprint(masked, fp);
                        final double[] probabilities = new double[fingerprint.length()];
                        for (int i=4; i < tabs.length; ++i)
                            probabilities[i-4] = Double.parseDouble(tabs[i]);
                        final ProbabilityFingerprint probs = new ProbabilityFingerprint(masked, probabilities);
                        for (int index : joinedIndizes) {
                            performances.get(index).update(maskedFp.isSet(index), probs.getProbability(index)>=0.5);
                        }
                    }
                }
            }
            try (final BufferedWriter bw = FileUtils.getWriter(new File("trainable_indizes.csv"))) {
                bw.write(PredictionPerformance.csvHeader());
                for (int index : indizes)  {
                    bw.write(String.valueOf(index));
                    bw.write('\t');
                    bw.write(performances.get(index).done().toCsvRow());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void prepare(File csiPath) throws IOException {
        // get classes
        try {
            final TrainedCSIFingerId trainedCSIFingerId = TrainedCSIFingerId.load(new File(csiPath, "fingerid.data"));


            try (BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("fingerprint_indizes.txt"))) {
                for (int index : trainedCSIFingerId.getFingerprintIndizes()) {
                    bw.write(String.valueOf(index));
                    bw.newLine();
                }
            }

            final List<FingerprintCandidate> knownOnes = filterCsvFiles(trainedCSIFingerId, csiPath);

            // now sample from classes
            final HashSet<String> allInchiKeys = new HashSet<>(3000000);
            try (final BufferedReader br = KernelToNumpyConverter.getReader(new File("compounds.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    allInchiKeys.add(line.substring(0, 14));
                }
            }

                final int numberOfEntriesSoFar = allInchiKeys.size();
                System.out.println("Using " + numberOfEntriesSoFar + " entries");

                // now download fingerprints
                System.out.println("Download Fingerprints");
                System.out.flush();
                final HashSet<MolecularFormula> formulas = new HashSet<>();


                for (FingerprintCandidate fpc : knownOnes)
                    allInchiKeys.remove(fpc.getInchiKey2D());


                if (!new File("fingerprints.csv").exists()) {
                    try (final ChemicalDatabase database = new ChemicalDatabase("fingerid1.bioinf.uni-jena.de:5432", "fingerid", "tV9QRQHn2THjq5HR")) {
                        try (final BufferedWriter bwFP = FileUtils.getWriter(new File("trainable_fingerprints.csv"))) {
                            try (final BufferedWriter bw = FileUtils.getWriter(new File("fingerprints.csv"))) {
                                for (FingerprintCandidate fpc : knownOnes) {
                                    bw.write(fpc.getInchiKey2D());
                                    bw.write('\t');
                                    final MolecularFormula formula = fpc.getInchi().extractFormulaOrThrow();
                                    formulas.add(formula);
                                    bw.write(formula.toString());
                                    bw.write('\t');
                                    bw.write(fpc.getFingerprint().asArray().toTabSeparatedString());
                                    bw.newLine();

                                    bwFP.write(fpc.getInchiKey2D());
                                    bwFP.write('\t');
                                    bwFP.write(tryToFindFingerprint(database, fpc).toTabSeparatedString());
                                    bwFP.newLine();

                                }
                                bw.flush();
                                for (FingerprintCandidate fpc : database.lookupFingerprintsByInchis(allInchiKeys)) {
                                    bw.write(fpc.getInchiKey2D());
                                    bw.write('\t');
                                    final MolecularFormula formula = fpc.getInchi().extractFormulaOrThrow();
                                    formulas.add(formula);
                                    bw.write(formula.toString());
                                    bw.write('\t');
                                    bw.write(trainedCSIFingerId.getMaskedFingerprintVersion().mask(fpc.getFingerprint().asArray()).asArray().toTabSeparatedString());
                                    bw.newLine();

                                    bwFP.write(fpc.getInchiKey2D());
                                    bwFP.write('\t');
                                    bwFP.write(CDK_MASK.mask(fpc.getFingerprint()).toTabSeparatedString());
                                    bwFP.newLine();
                                }
                            }
                        }

                    } catch (ChemicalDatabaseException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (String[] col : FileUtils.readTable(new File("fingerprints.csv")))
                        MolecularFormula.parseAndExecute(col[1], formulas::add);
                }

                writeFormulaFeatures(formulas);
                trainableFpPerformance(trainedCSIFingerId, csiPath);

        } finally {

        }
    }

    private static ArrayFingerprint tryToFindFingerprint(ChemicalDatabase database, FingerprintCandidate fpc) throws IOException {
        final List<FingerprintCandidate> c = database.lookupFingerprintsByInchi(Arrays.asList((CompoundCandidate) fpc));
        if (c.size()>0) {
            return CDK_MASK.mask(c.get(0).getFingerprint()).asArray();
        } else {
            try {
                Fingerprinter fingerprinter = Fingerprinter.getForVersion(TrainingData.VERSION);
                final boolean[] fp = fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(fingerprinter.convertInchi2Mol(fpc.getInchi().in2D)));
                return CDK_MASK.mask(new BooleanFingerprint(TrainingData.VERSION,fp)).asArray();
            } catch (CDKException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private static void writeFormulaFeatures(HashSet<MolecularFormula> formulas) throws IOException {
        final FormulaConstraints constraints = new FormulaConstraints("CHNOPSClBrBSeIF");
        final Iterator<MolecularFormula> fiter = formulas.iterator();
        while (fiter.hasNext()) {
            final MolecularFormula f = fiter.next();
            if (f.getMass() > 2000)
                fiter.remove();
            else if (constraints.isViolated(f, PeriodicTable.getInstance().neutralIonization()))
                fiter.remove();
            else if (f.numberOfCarbons() == 0 || f.numberOfHydrogens() == 0)
                fiter.remove();
            else if (f.rdbe() <= -1)
                fiter.remove();
        }
        final ArrayList<double[]> values = new ArrayList<>();
        try (final BufferedWriter bw = KernelToNumpyConverter.getWriter(new File("formula_features.csv"))) {
            for (MolecularFormula f : formulas) {
                double[] formulaFeatures = Canopus.getFormulaFeatures(f);
                values.add(formulaFeatures);
                bw.write(f.toString());
                for (double val : formulaFeatures) {
                    bw.write('\t');
                    bw.write(String.valueOf(val));
                }
                bw.newLine();
            }
        }
        final double[][] MATRIX = values.toArray(new double[values.size()][]);
        final double[][] scaling = FormulaFeatureVector.normalizeAndCenter(MATRIX);
        new KernelToNumpyConverter().writeToFile(new File("formula_normalized.txt"), scaling);
    }


    private static List<FingerprintCandidate> filterCsvFiles(TrainedCSIFingerId fingerid, File csiPath) {
        final Mask m;
        final ArrayList<FingerprintCandidate> fingerprintCandidates = new ArrayList<>();
        final HashSet<String> inchiKeys = new HashSet<>();
        try {
            final MaskedFingerprintVersion masked = fingerid.getMaskedFingerprintVersion();
            {
                final File f = new File(csiPath, "prediction_prediction.csv");
                for (String line : Files.readLines(f, Charset.forName("UTF-8"))) {
                    String[] tabs = line.split("\t");
                    {
                        String fingerprint = tabs[3];
                        boolean[] fp = new boolean[fingerprint.length()];
                        for (int i = 0; i < fp.length; ++i)
                            if (fingerprint.charAt(i) == '1')
                                fp[i] = true;
                        final BooleanFingerprint maskedFp = new BooleanFingerprint(masked, fp);
                        // add fingerprint candidate
                        final String inchikey = tabs[1].substring(0, 14);
                        if (!inchiKeys.contains(inchikey)) {
                            inchiKeys.add(inchikey);
                            fingerprintCandidates.add(new FingerprintCandidate(InChIs.newInChI(inchikey, tabs[2]), maskedFp.asArray()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return fingerprintCandidates;
    }

}
