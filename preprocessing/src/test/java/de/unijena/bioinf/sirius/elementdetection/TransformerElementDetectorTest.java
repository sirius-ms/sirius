package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.babelms.cef.P;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.deisotope.IsotopePatternDetection;
import de.unijena.bioinf.sirius.elementdetection.transformer.FourierEncoder;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerBasedPredictor;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerPrediction;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static de.unijena.bioinf.ChemistryBase.math.MatrixUtils.dot;
import static de.unijena.bioinf.sirius.elementdetection.transformer.TransformerBasedPredictor.readFromTxt;

public class TransformerElementDetectorTest {


    public static void testFreq() {
        MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPSClBr").elementArray()));
        MolecularFormula[] L, R;
        {
            List<MolecularFormula> molecularFormulas = decomposer.decomposeToFormulas(420.094546, PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization(), new Deviation(10));
            List<MolecularFormula> cls = molecularFormulas.stream().filter(x -> x.numberOf("Cl") > 0).toList();
            List<MolecularFormula> brs = molecularFormulas.stream().filter(x -> x.numberOf("Br") > 0).toList();
            List<MolecularFormula> ss = molecularFormulas.stream().filter(x -> x.numberOf("S") > 0).toList();
            List<MolecularFormula> chnops = molecularFormulas.stream().filter(x -> x.isCHNOPS()).toList();
            MolecularFormulaScorer scorer = ChemicalCompoundScorer.createDefaultCompoundScorer(false);
            MolecularFormula cl = cls.stream().filter(x -> x.numberOfHydrogens() < 60).max(Comparator.comparingDouble(scorer::score)).get();
            MolecularFormula br = brs.stream().max(Comparator.comparingDouble(scorer::score)).get();
            MolecularFormula s = ss.stream().max(Comparator.comparingDouble(scorer::score)).get();
            MolecularFormula ch = chnops.stream().max(Comparator.comparingDouble(scorer::score)).get();
            L = new MolecularFormula[]{cl,br,s,ch};
        }
        final MolecularFormula[] formulas = Arrays.stream(L).map(x->MolecularFormula.parseOrThrow("C40H80").add(x)).toArray(MolecularFormula[]::new);
        final SimpleSpectrum[] isos = new SimpleSpectrum[formulas.length];
        for (int i=0; i < formulas.length; ++i) {
            isos[i] = new FastIsotopePatternGenerator(Normalization.Sum(1d)).simulatePattern(formulas[i], PrecursorIonType.fromString("[M+H]+").getIonization());
        }
        final double[][] vectors = new double[formulas.length*3][];
        final double[] numbers = new double[formulas.length*3];
        final double[] massDefects = new double[]{
                1.145354425458380865e+04,
                4.480852147778600738e+03,
                2.520998462159892370e+03,
                8.699580897180419470e+02,
                4.335020862529584065e+02,
                2.234568305971408506e+02,
                1.896933923685029413e+02,
                9.787563800602511321e+01,
                8.547769927946161772e+01,
                7.799065973248721662e+01,
                7.493946228352623962e+01,
                6.539835936644449532e+01,
                5.026548245743668986e+01,
                3.490658503988658623e+01,
                2.513274122871834493e+01
        };
        final FourierEncoder encoder = new FourierEncoder(massDefects);
        for (int i=0; i < formulas.length; ++i) {
            numbers[i] = isos[i].getMzAt(0)-Math.round(isos[i].getMzAt(0));
            vectors[i] = MatrixUtils.float2double(encoder.compute(numbers[i]));
            numbers[i+formulas.length] = isos[i].getMzAt(1)-Math.round(isos[i].getMzAt(1));
            vectors[i+formulas.length] = MatrixUtils.float2double(encoder.compute(numbers[i+formulas.length]));
            numbers[i+2*formulas.length] = isos[i].getMzAt(2)-Math.round(isos[i].getMzAt(2));
            vectors[i+2*formulas.length] = MatrixUtils.float2double(encoder.compute(numbers[i+2*formulas.length]));
        }
        // compute dot products
        float[][] M = new float[vectors.length][vectors.length];
        for (int i=0; i < vectors.length; ++i) {
            for (int j=0; j < vectors.length; ++j) {
                M[i][j] = (float)dot(vectors[i], vectors[j]);
            }
        }

        System.out.printf("XXXX");
        for (int c=0; c < numbers.length; ++c) {
            System.out.printf("\t%.4f",numbers[c]);
        }
        System.out.println();

        for (int row=0; row < M.length; ++row) {
            System.out.printf("%.4f",numbers[row]);
            for (int col=0; col < M.length; ++col) {
                System.out.printf("\t%.4f",M[row][col]);
            }
            System.out.println();
        }




    }

    public static void testData() throws IOException {
        final File out = new File("//home/kaidu/github/sirius-frontend/preprocessing/src/main/resources/transformer.bin");
        final TransformerBasedPredictor predictor = TransformerBasedPredictor.readFromBinary(out);
        //final TransformerBasedPredictor predictor = readFromTxt(new File("/home/kaidu/work/data/iso/parameters"));
        int correctPredicted = 0, wrongPredicted = 0;
        List<MolecularFormula> wrong = new ArrayList<>();
        int legacyCorrect=0, legacyWrong=0;
        int legacyFP=0, fp=0;
        int legacyFPNOS=0, fpnoS = 0;
        PeriodicTable T = PeriodicTable.getInstance();
        Element[] predicted = new Element[]{
                T.getByName("Cl"),T.getByName("Br"),
                        T.getByName("S"),T.getByName("B"),
                                T.getByName("Se"),T.getByName("Mg"),
                                        T.getByName("Fe"),T.getByName("Zn")
        };

        // statistics
        FloatArrayList[] massDeviationsInPPMPerPeak = new FloatArrayList[7];
        for (int i=0; i < massDeviationsInPPMPerPeak.length; ++i) massDeviationsInPPMPerPeak[i] = new FloatArrayList();

        FloatArrayList[] absoluteIntensityDeviationsPerPeak = new FloatArrayList[7];
        for (int i=0; i < absoluteIntensityDeviationsPerPeak.length; ++i) absoluteIntensityDeviationsPerPeak[i] = new FloatArrayList();

        FloatArrayList[] relativeMassDeviations = new FloatArrayList[7];
        for (int i=0; i < relativeMassDeviations.length; ++i) relativeMassDeviations[i] = new FloatArrayList();

        FloatArrayList[] relativeMassDeviationsLM = new FloatArrayList[7];
        for (int i=0; i < relativeMassDeviationsLM.length; ++i) relativeMassDeviationsLM[i] = new FloatArrayList();
        FloatArrayList[] relativeMassDeviationsSM = new FloatArrayList[7];
        for (int i=0; i < relativeMassDeviationsSM.length; ++i) relativeMassDeviationsSM[i] = new FloatArrayList();

        FloatArrayList[] relativeIntensityDeviationsPerPeak = new FloatArrayList[7];
        for (int i=0; i < relativeIntensityDeviationsPerPeak.length; ++i) relativeIntensityDeviationsPerPeak[i] = new FloatArrayList();

        FastIsotopePatternGenerator fastIsotopePatternGenerator = new FastIsotopePatternGenerator(Normalization.Sum(1));
        DeepNeuralNetworkElementDetector legacy = new DeepNeuralNetworkElementDetector();
        final Ms1Preprocessor preprocessor = new Ms1Preprocessor();
        for (File f : new File("/home/kaidu/work/data/iso/test/ms1").listFiles()) {
            if (f.getName().endsWith(".ms")) {
                try {
                    Ms2Experiment exp = MsIO.readExperimentFromFile(f).next();
                    ProcessedInput preprocess = preprocessor.preprocess(exp);
                    final SimpleSpectrum isotopes = preprocess.getAnnotation(Ms1IsotopePattern.class).get().getSpectrum();
                    if (isotopes.isEmpty()) continue;
                    System.out.println("-------------------------------------------");
                    System.out.println(exp.getMolecularFormula() + " \t" + f.getName());
                    SimpleSpectrum measuredData = Spectrums.getNormalizedSpectrum(isotopes, Normalization.Sum(1d));
                    for (Peak p : measuredData) {
                        System.out.println(p.getMass() + "\t" + p.getIntensity());
                    }
                    fastIsotopePatternGenerator.setMaximalNumberOfPeaks(measuredData.size());
                    PrecursorIonType precursorIonType = preprocess.getExperimentInformation().getPrecursorIonType();
                    Ionization ionization = precursorIonType.getIonization();
                    SimpleSpectrum theoreticalSpectrum = fastIsotopePatternGenerator.simulatePattern(precursorIonType.neutralMoleculeToMeasuredNeutralMolecule(exp.getMolecularFormula()),
                            precursorIonType.isIntrinsicalCharged() ? new Charge(ionization.getCharge()) : ionization);

                    for (int j=0; j < theoreticalSpectrum.size(); ++j) {
                        if (j < measuredData.size()) {
                            final Peak m = measuredData.getPeakAt(j);
                            final Peak t = theoreticalSpectrum.getPeakAt(j);

                            relativeMassDeviations[j].add((float)Math.abs((m.getMass() - measuredData.getMzAt(0)) - (t.getMass() - theoreticalSpectrum.getMzAt(0)) ));
                            if (theoreticalSpectrum.getMzAt(0) > 500) {
                                relativeMassDeviationsLM[j].add((float)Math.abs((m.getMass() - measuredData.getMzAt(0)) - (t.getMass() - theoreticalSpectrum.getMzAt(0)) ));
                            } else {
                                relativeMassDeviationsSM[j].add((float)Math.abs((m.getMass() - measuredData.getMzAt(0)) - (t.getMass() - theoreticalSpectrum.getMzAt(0)) ));

                            }
                            massDeviationsInPPMPerPeak[j].add((float)Math.abs(Deviation.fromMeasurementAndReference(m.getMass(), t.getMass()).getPpm()));
                            absoluteIntensityDeviationsPerPeak[j].add((float)Math.abs(m.getIntensity()-t.getIntensity()));
                            relativeIntensityDeviationsPerPeak[j].add((float)Math.exp(Math.abs(Math.log(m.getIntensity())-Math.log(t.getIntensity())))-1f);
                        }
                    }

                    System.out.println();
                    boolean found=false;
                    final FormulaConstraints defaultfc  = isotopes.size()>2 ? new FormulaConstraints("CHNOPFI") : new FormulaConstraints("CHNOPSFI");
                    TransformerPrediction monoPred = null;

                    if (f.getName().endsWith("agilent_859.ms")) {
                        System.err.println("Debug.");
                    }

                    for (TransformerPrediction p : predictor.predict(isotopes)) {
                        System.out.println(p);
                        if (p.getMonoisotopicPeak()==0) {
                            if (p.getConstraints().getExtendedConstraints(defaultfc).isSatisfied(exp.getMolecularFormula())) {
                                found=true;
                            }
                            monoPred=p;
                        }
                    }
                    if (found) ++correctPredicted;
                    else {
                        ++wrongPredicted;
                        wrong.add(exp.getMolecularFormula());
                    }
                    DetectedElements legacyDetection = legacy.detect(preprocess);
                    FormulaConstraints legdec = legacyDetection.getFormulaConstraints(new FormulaConstraints("CHNOPS")).constraints();
                    System.out.println("Legacy: " + legdec);
                    System.out.println();
                    if (legdec.isSatisfied(exp.getMolecularFormula())) {
                        ++legacyCorrect;
                    } else {
                        ++legacyWrong;
                    }
                    for (Element e : predicted) {
                        if (legdec.hasElement(e) && exp.getMolecularFormula().numberOf(e)<=0) {
                            ++legacyFP;
                            if (!e.getSymbol().equals("S")) ++legacyFPNOS;
                        }
                        if (monoPred.getConstraints().hasElement(e) && exp.getMolecularFormula().numberOf(e)<=0) {
                            ++fp;
                            if (!e.getSymbol().equals("S")) ++fpnoS;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println(correctPredicted + " / " + (correctPredicted+wrongPredicted) + " correctly predicted.");
        System.out.println(fp + " / " + (correctPredicted+wrongPredicted) + " instances have too many elements predicted.");
        System.out.println(fpnoS+ " / " + (correctPredicted+wrongPredicted) + " instances have too many elements beyond S predicted.");
        System.out.println("Legacy: ");
        System.out.println(legacyCorrect + " / " + (correctPredicted+wrongPredicted) + " correctly predicted.");
        System.out.println(legacyFP + " / " + (correctPredicted+wrongPredicted) + " instances have too many elements predicted.");
        System.out.println(legacyFPNOS+ " / " + (correctPredicted+wrongPredicted) + " instances have too many elements beyond S predicted.");
        System.out.println("\nFormulas that were wrong: ");
        System.out.println(wrong);
        /// ///////// stats

        System.out.println("\nMass Deviation (ppm)");
        for (int i=0; i < massDeviationsInPPMPerPeak.length; ++i) {
            if (massDeviationsInPPMPerPeak[i].isEmpty()) continue;
            massDeviationsInPPMPerPeak[i].sort(null);
            System.out.printf("%d-th peak\tavg = %.3f\tmedian = %.3f\t%d samples\n", i, massDeviationsInPPMPerPeak[i].doubleStream().average().orElse(0d),
                    massDeviationsInPPMPerPeak[i].getFloat((int)(massDeviationsInPPMPerPeak[i].size()/2)), massDeviationsInPPMPerPeak[i].size());
        }
        System.out.println("\nMass Difference Deviation (Da)");
        for (int i=0; i < relativeMassDeviations.length; ++i) {
            FloatArrayList fs = relativeMassDeviations[i];
            if (fs.isEmpty()) continue;
            fs.sort(null);
            System.out.printf("%d-th peak\tavg = %.8f\tmedian = %.8f\t%d samples\n", i, fs.doubleStream().average().orElse(0d),
                    fs.getFloat((int)(fs.size()/2)), fs.size());
            fs = relativeMassDeviationsLM[i];
            if (fs.isEmpty()) continue;
            fs.sort(null);
            System.out.printf("%d-th peak\tavg = %.8f\tmedian = %.8f\t%d samples\t mass above 500\n", i, fs.doubleStream().average().orElse(0d),
                    fs.getFloat((int)(fs.size()/2)), fs.size());
            fs = relativeMassDeviationsSM[i];
            if (fs.isEmpty()) continue;
            fs.sort(null);
            System.out.printf("%d-th peak\tavg = %.8f\tmedian = %.8f\t%d samples\t mass below 500\n", i, fs.doubleStream().average().orElse(0d),
                    fs.getFloat((int)(fs.size()/2)), fs.size());
        }
        System.out.println("\nAbsolute Intensity Deviation (%)");
        for (int i=0; i < absoluteIntensityDeviationsPerPeak.length; ++i) {
            if (absoluteIntensityDeviationsPerPeak[i].isEmpty()) continue;
            absoluteIntensityDeviationsPerPeak[i].sort(null);
            System.out.printf("%d-th peak\tavg = %.3f\tmedian = %.3f\t%d samples\n", i, absoluteIntensityDeviationsPerPeak[i].doubleStream().average().orElse(0d),
                    absoluteIntensityDeviationsPerPeak[i].getFloat((int)(absoluteIntensityDeviationsPerPeak[i].size()/2)), absoluteIntensityDeviationsPerPeak[i].size());
        }

        System.out.println("\nRelative Intensity Deviation");
        for (int i=0; i < relativeIntensityDeviationsPerPeak.length; ++i) {
            if (relativeIntensityDeviationsPerPeak[i].isEmpty()) continue;
            relativeIntensityDeviationsPerPeak[i].sort(null);
            System.out.printf("%d-th peak\tavg = %.3f\tmedian = %.3f\t%d samples\n", i, relativeIntensityDeviationsPerPeak[i].doubleStream().average().orElse(0d),
                    relativeIntensityDeviationsPerPeak[i].getFloat((int)(relativeIntensityDeviationsPerPeak[i].size()/2)), relativeIntensityDeviationsPerPeak[i].size());
        }

    }

    public static void main(String[] args) {
        try {
            testData();
            final File out = new File("/home/kaidu/github/sirius-frontend/preprocessing/src/main/resources/transformer.bin");
            final TransformerBasedPredictor predictor = readFromTxt(new File("/home/kaidu/work/data/iso/parameters"));

            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
            /*
288.059296	0.4307078580406275
289.062816	0.06283447182254577
290.057287	0.4393226113925398
291.060978	0.06101191506427363
292.060992	0.0058009246530899335
293.060991	3.222190269233817E-4
             */
            spec.addPeak(288.059296, 0.4307078580406275);
            spec.addPeak(289.062816,	0.06283447182254577);
            spec.addPeak(290.057287,	0.4393226113925398);
            spec.addPeak(291.060978,	0.06101191506427363);
            spec.addPeak(292.060992,	0.0058009246530899335);
            spec.addPeak(293.060991,	3.222190269233817E-4);
            /*
            spec.addPeak(201.04804547236688,  0.04786877088637881);
            spec.addPeak(202.04193422372143,  0.23261093461509075);
            spec.addPeak(203.0453318162227, 0.015810579533340148);
            spec.addPeak(204.0464506968809,  0.0027894032758852263 );
            */
            final SimpleSpectrum pattern = new SimpleSpectrum(spec);


            TransformerPrediction[] predict = predictor.predict(pattern);
            for (TransformerPrediction p : predict) {
                System.out.println(p);
            }
            if (true){//(!out.exists()) {
                try (final FileChannel channel = FileChannel.open(out.toPath(), StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
                    predictor.write(buffer);
                    buffer.flip();
                    channel.write(buffer);
                }
            }
            // validate
            TransformerBasedPredictor.readFromBinary(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
