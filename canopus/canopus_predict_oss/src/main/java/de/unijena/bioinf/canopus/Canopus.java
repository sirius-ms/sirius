package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.dnn.ActivationFunction;
import de.unijena.bioinf.canopus.dnn.FullyConnectedLayer;
import de.unijena.bioinf.canopus.dnn.PlattLayer;
import org.ejml.data.FMatrixRMaj;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Canopus {
    protected FullyConnectedLayer[] formulaLayers;
    protected FullyConnectedLayer[] fingerprintLayers;
    protected FullyConnectedLayer[] innerLayers;
    protected FullyConnectedLayer outputLayer;
    protected PlattLayer plattLayer;

    protected double[] formulaScaling, formulaCentering, plattScaling, plattCentering;
    protected ClassyFireFingerprintVersion classyFireFingerprintVersion;
    protected MaskedFingerprintVersion classyFireMask;

    protected CdkFingerprintVersion cdkFingerprintVersion;
    protected MaskedFingerprintVersion cdkMask;

    public static void main(String[] args) {
        try {
            final Canopus c = Canopus.loadFromFile(new File("/home/kaidu/temp/canopus_100.data.gz"));
            System.out.println(c.classyFireMask.allowedIndizes().length);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("CANOPUS\nFormula Layers: {");
        for (FullyConnectedLayer l : formulaLayers) {
            buf.append("\t" + l.toString());
        }
        buf.append("}\nFingerprint Layers: {");
        for (FullyConnectedLayer l : fingerprintLayers) {
            buf.append("\t" + l.toString());
        }
        buf.append("}\nInner Layers:");
        for (FullyConnectedLayer l : innerLayers) {
            buf.append("\t" + l.toString());
        }
        buf.append("}\nOutput Layer:");
        buf.append("\t" + outputLayer.toString());
        return buf.toString();
    }

    public double[] debug(MolecularFormula formula, ProbabilityFingerprint fingerprint, boolean plattTransform) {
        System.out.println("-------------------------------------");
        System.out.println("---------  PARAMETERS: CENTER PLATT --------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(plattCentering));
        System.out.println("-------------------------------------");
        System.out.println("---------  PARAMETERS: SCALE PLATT --------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(plattScaling));
        System.out.println("-------------------------------------");
        System.out.println("---------  PARAMETERS: CENTER FORMULA --------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(formulaCentering));
        System.out.println("-------------------------------------");
        System.out.println("---------  PARAMETERS: SCALE FORMULA --------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(formulaScaling));
        System.out.println("-------------------------------------");
        System.out.println("-----------  INPUT  -----------------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(fingerprint.toProbabilityArray()));
        final double[] ff = getFormulaFeatures(formula);
        System.out.println(Arrays.toString(ff));
        // normalize/center
        for (int i=0; i < ff.length; ++i) {
            ff[i] -= formulaCentering[i];
            ff[i] /= formulaScaling[i];
        }
        final double[] fp = fingerprint.toProbabilityArray();
        for (int i=0; i < fp.length; ++i) {
            fp[i] -= plattCentering[i];
            fp[i] /= plattScaling[i];
        }

        System.out.println("-------------------------------------");
        System.out.println("---- PROCESSED PLATTS   -------------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(fp));
        System.out.println("-------------------------------------");
        System.out.println("---- PROCESSED FORMULA  -------------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(ff));
        // for the DNN we have to convert our vectors into float
        final float[] formulaInput = new float[ff.length];
        for (int i=0; i < ff.length; ++i) formulaInput[i] = (float)ff[i];
        final float[] plattInput = new float[fp.length];
        for (int i=0; i < fp.length; ++i) plattInput[i] = (float)fp[i];
        // wrap into matrix
        FMatrixRMaj formulaInputVector = FMatrixRMaj.wrap(1, formulaInput.length, formulaInput);
        for (FullyConnectedLayer l : formulaLayers)
            formulaInputVector = l.eval(formulaInputVector);

        FMatrixRMaj fpInputVector = FMatrixRMaj.wrap(1, plattInput.length, plattInput);
        for (FullyConnectedLayer l : fingerprintLayers)
            fpInputVector = l.eval(fpInputVector);

        final float[] combined = new float[fpInputVector.numCols+formulaInputVector.numCols];
        System.arraycopy(formulaInputVector.data, 0, combined, 0, formulaInputVector.numCols);
        System.arraycopy(fpInputVector.data, 0, combined, formulaInputVector.numCols, fpInputVector.numCols);

        System.out.println("-------------------------------------");
        System.out.println("----    DEEP           -------------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(combined));

        FMatrixRMaj combinedVector = FMatrixRMaj.wrap(1, combined.length, combined);

        for (FullyConnectedLayer l : innerLayers)
            combinedVector = l.eval(combinedVector);

        FMatrixRMaj outputVector = outputLayer.eval(combinedVector);
        // convert back to double
        final double[] outputArray = new double[outputVector.numCols];
        for (int i=0; i < outputArray.length; ++i) outputArray[i] = outputVector.data[i];

        System.out.println("-------------------------------------");
        System.out.println("----    FINAL LAYER     -------------");
        System.out.println("-------------------------------------");
        System.out.println(Arrays.toString(outputArray));

        return outputArray;
    }

    protected Canopus(FullyConnectedLayer[] formulaLayers, FullyConnectedLayer[] fingerprintLayers, FullyConnectedLayer[] innerLayers, FullyConnectedLayer outputLayer, PlattLayer plattLayer, double[] formulaCentering, double[] formulaScaling, double[] plattCentering, double[] plattScaling, MaskedFingerprintVersion classyFireMask, MaskedFingerprintVersion cdkMask) {
        this.formulaLayers = formulaLayers;
        this.fingerprintLayers = fingerprintLayers;
        this.innerLayers = innerLayers;
        this.outputLayer = outputLayer;
        this.formulaScaling = formulaScaling;
        this.formulaCentering = formulaCentering;
        this.classyFireMask = classyFireMask;
        this.classyFireFingerprintVersion = (ClassyFireFingerprintVersion)classyFireMask.getMaskedFingerprintVersion();
        this.plattLayer = plattLayer;
        this.plattCentering = plattCentering;
        this.plattScaling = plattScaling;
        this.cdkMask = cdkMask;
        this.cdkFingerprintVersion = cdkMask==null ? null : (CdkFingerprintVersion) cdkMask.getMaskedFingerprintVersion();
    }


    public void bla(ProbabilityFingerprint fingerprint) {
        for (FPIter iter : fingerprint) {
            final ClassyfireProperty property = (ClassyfireProperty) iter.getMolecularProperty();
            final double probability = iter.getProbability();
        }
    }

    public boolean isPredictingFingerprints() {
        return cdkFingerprintVersion!=null;
    }

    public CdkFingerprintVersion getCdkFingerprintVersion() {
        return cdkFingerprintVersion;
    }

    public MaskedFingerprintVersion getCdkMask() {
        return cdkMask;
    }

    public MaskedFingerprintVersion getCanopusMask() {
        return classyFireMask;
    }

    public ClassyFireFingerprintVersion getClassyFireFingerprintVersion() {
        return classyFireFingerprintVersion;
    }

    public List<String> getKlassNames() {
        final List<String> names = new ArrayList<>(classyFireMask.size());
        for (int index : classyFireMask.allowedIndizes()) {
            names.add(((ClassyfireProperty)classyFireMask.getMolecularProperty(index)).getName());
        }
        return names;
    }

    public ProbabilityFingerprint predictClassificationFingerprint(MolecularFormula formula, ProbabilityFingerprint fingerprint) {
        double[] probs = predictProbability(formula, fingerprint);
        if (cdkFingerprintVersion==null) return new ProbabilityFingerprint(classyFireMask, probs);
        else {
            probs = Arrays.copyOf(probs, classyFireMask.size());
            return new ProbabilityFingerprint(classyFireMask, probs);
        }
    }
    public ProbabilityFingerprint predictFingerprintFromFingerprint(MolecularFormula formula, ProbabilityFingerprint fingerprint) {
        if (!isPredictingFingerprints())
            throw new RuntimeException("No predictor for fingerprints available in this model");
        return predict(formula, fingerprint)[1];
    }

    public ProbabilityFingerprint[] predict(MolecularFormula formula, ProbabilityFingerprint fingerprint) {
        final double[] probs = predictProbability(formula, fingerprint);
        final double[] buf1 = Arrays.copyOf(probs, classyFireMask.size());
        final double[] buf2 = new double[cdkMask.size()];
        System.arraycopy(probs, classyFireMask.size(), buf2, 0, buf2.length);
        return new ProbabilityFingerprint[]{new ProbabilityFingerprint(classyFireMask, buf1), new ProbabilityFingerprint(cdkMask, buf2)};
    }

    public double[] predictProbability(MolecularFormula formula, ProbabilityFingerprint fingerprint) {
        return predictDecisionValues(formula, fingerprint, true);
    }

    public double[] predictDecisionValues(MolecularFormula formula, ProbabilityFingerprint fingerprint, boolean plattTransform) {
        final double[] ff = getFormulaFeatures(formula);
        // normalize/center
        for (int i=0; i < ff.length; ++i) {
            ff[i] -= formulaCentering[i];
            ff[i] /= formulaScaling[i];
        }
        final double[] fp = fingerprint.toProbabilityArray();
        for (int i=0; i < fp.length; ++i) {
            fp[i] -= plattCentering[i];
            fp[i] /= plattScaling[i];
        }
        // for the DNN we have to convert our vectors into float
        final float[] formulaInput = new float[ff.length];
        for (int i=0; i < ff.length; ++i) formulaInput[i] = (float)ff[i];
        final float[] plattInput = new float[fp.length];
        for (int i=0; i < fp.length; ++i) plattInput[i] = (float)fp[i];
        // wrap into matrix
        FMatrixRMaj formulaInputVector = FMatrixRMaj.wrap(1, formulaInput.length, formulaInput);
        for (FullyConnectedLayer l : formulaLayers)
            formulaInputVector = l.eval(formulaInputVector);

        FMatrixRMaj fpInputVector = FMatrixRMaj.wrap(1, plattInput.length, plattInput);
        for (FullyConnectedLayer l : fingerprintLayers)
            fpInputVector = l.eval(fpInputVector);

        final float[] combined = new float[fpInputVector.numCols+formulaInputVector.numCols];
        System.arraycopy(formulaInputVector.data, 0, combined, 0, formulaInputVector.numCols);
        System.arraycopy(fpInputVector.data, 0, combined, formulaInputVector.numCols, fpInputVector.numCols);

        FMatrixRMaj combinedVector = FMatrixRMaj.wrap(1, combined.length, combined);

        for (FullyConnectedLayer l : innerLayers)
            combinedVector = l.eval(combinedVector);

        FMatrixRMaj outputVector = outputLayer.eval(combinedVector);
        if (plattTransform) outputVector = plattLayer.eval(outputVector);
        // convert back to double
        final double[] outputArray = new double[outputVector.numCols];
        for (int i=0; i < outputArray.length; ++i) outputArray[i] = outputVector.data[i];
        return outputArray;
    }

    public static double[] getFormulaFeatures(MolecularFormula f) {
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
                t.getByName("Se"),
                t.getByName("As")
        };
        final Element C = elements[0], H = elements[1], N = elements[2], O = elements[3];
        final double[] values = new double[elements.length+15];
        int K = 0;
        for (Element e : elements) {
            values[K++] = f.numberOf(e);
        }
        values[K++] = f.rdbe();
        values[K++] = f.getMass();
        // add RDBE==0
        values[K++] = f.rdbe()==0 ? 1 : -1;
        // add  RDBE/(mass/100.0)^(2/3)
        values[K++] = (Math.min(0, f.rdbe())+1)/Math.pow(f.getMass()/100.0, 2d/3d);

        // add other properties
        values[K++] = f.hetero2CarbonRatio();
        values[K++] = f.hetero2OxygenRatio();
        values[K++] = f.heteroWithoutOxygenToCarbonRatio();
        values[K++] = f.hydrogen2CarbonRatio();
        values[K++] = f.numberOfOxygens() / (0.5f+f.numberOfCarbons());
        values[K++] = f.numberOfNitrogens() / (0.5f+f.numberOfCarbons());
        // CHNOPS only
        values[K++] = f.isCHNOPS() ? 1d : -1d;
        values[K++] = f.isCHNO() ? 1d : -1d;
        // CHO only?
        values[K++] = f.isCHO() ? 1d : -1d;
        // logarithm of C
        values[K++] = Math.log(f.numberOfCarbons()+0.5f);
        values[K++] = Math.log(f.numberOfHydrogens()+0.5f);
        return values;
    }


    public void dump(OutputStream stream) throws IOException {
        final ObjectOutputStream bstream = new ObjectOutputStream(stream);
        bstream.writeInt(formulaLayers.length);
        for (FullyConnectedLayer l : formulaLayers) {
            l.dump(bstream);
        }

        bstream.writeInt(fingerprintLayers.length);
        for (FullyConnectedLayer l : fingerprintLayers) {
            l.dump(bstream);
        }

        bstream.writeInt(innerLayers.length);
        for (FullyConnectedLayer l : innerLayers) {
            l.dump(bstream);
        }

        outputLayer.dump(bstream);

        bstream.writeInt(4887);

        plattLayer.dump(bstream);

        // add magic number
        if (isPredictingFingerprints()) {
            bstream.writeInt(2338);
        } else {
            bstream.writeInt(1337);
        }

        bstream.writeInt(formulaCentering.length);
        for (double d : formulaCentering) bstream.writeDouble(d);
        bstream.writeInt(formulaScaling.length);
        for (double d : formulaScaling) bstream.writeDouble(d);

        {
            bstream.writeInt(plattCentering.length);
            for (double d : plattCentering) bstream.writeDouble(d);
            bstream.writeInt(plattCentering.length);
            for (double d : plattScaling) bstream.writeDouble(d);
        }

        bstream.writeInt(classyFireFingerprintVersion.size());
        for (int i=0, n = classyFireFingerprintVersion.size(); i<n; ++i) {
            final ClassyfireProperty p = (ClassyfireProperty) classyFireFingerprintVersion.getMolecularProperty(i);
            bstream.writeInt(p.getChemOntId());
            bstream.writeInt(p.getParentId());
            bstream.writeUTF(p.getName());
            bstream.writeUTF(p.getDescription());
        }
        bstream.writeInt(classyFireMask.size());
        for (int index : classyFireMask.allowedIndizes())
            bstream.writeInt(index);

        if (isPredictingFingerprints()) {

            bstream.writeLong(cdkFingerprintVersion.getBitsetIdentifier());
            final int[] indizes = cdkMask.allowedIndizes();
            bstream.writeInt(indizes.length);
            for (int i=0; i < indizes.length; ++i) {
                bstream.writeInt(indizes[i]);
            }

        }
        bstream.flush();
    }

    public void writeToFile(File f) throws IOException {
        if (f.getName().endsWith(".gz")) {
            try (final GZIPOutputStream fr = new GZIPOutputStream(new FileOutputStream(f))) {
                dump(fr);
            }
        } else {
            try (final BufferedOutputStream fr = new BufferedOutputStream(new FileOutputStream(f))) {
                dump(fr);
            }
        }
    }

    public static Canopus loadFromFile(File f) throws IOException {
        if (f.getName().endsWith(".gz")) {
            try (final GZIPInputStream fr = new GZIPInputStream(new FileInputStream(f))) {
                return load(fr);
            }
        } else {
            try (final BufferedInputStream fr = new BufferedInputStream(new FileInputStream(f))) {
                return load(fr);
            }
        }
    }

    public static Canopus load(InputStream stream) throws IOException {
        try (final ObjectInputStream b = new ObjectInputStream(stream)) {

            final FullyConnectedLayer[] formulaLayers = new FullyConnectedLayer[b.readInt()];
            for (int i = 0; i < formulaLayers.length; ++i) {
                formulaLayers[i] = FullyConnectedLayer.load(b);
//                System.out.println(formulaLayers[i]);
            }

            final FullyConnectedLayer[] fingerprintLayers = new FullyConnectedLayer[b.readInt()];
            for (int i = 0; i < fingerprintLayers.length; ++i) {
                fingerprintLayers[i] = FullyConnectedLayer.load(b);
//                System.out.println(fingerprintLayers[i]);
            }

            final FullyConnectedLayer[] innerLayers = new FullyConnectedLayer[b.readInt()];
            for (int i = 0; i < innerLayers.length; ++i) {
                innerLayers[i] = FullyConnectedLayer.load(b);
//                System.out.println(innerLayers[i]);
            }

            final FullyConnectedLayer outputLayer = FullyConnectedLayer.load(b);
//            System.out.println(outputLayer);
            if (b.readInt() != 4887) {
                throw new IOException("Missalignment happened between output and platt layer");
            }
            final PlattLayer plattLayer = PlattLayer.load(b);

            // read magic number
            final int magicNumber = b.readInt();
            final boolean additionalFingerprintsEnabled = magicNumber==2338;
            if (!additionalFingerprintsEnabled && magicNumber != 1337) {
                throw new IOException("Missalignment happened after platt layer");
            }

            final double[] formulaCentering = new double[b.readInt()];
            for (int i = 0; i < formulaCentering.length; ++i) formulaCentering[i] = b.readDouble();

            final double[] formulaScaling = new double[b.readInt()];
            for (int i = 0; i < formulaScaling.length; ++i) formulaScaling[i] = b.readDouble();

            final double[] plattCentering = new double[b.readInt()];
            for (int i = 0; i < plattCentering.length; ++i) plattCentering[i] = b.readDouble();

            final double[] plattScaling = new double[b.readInt()];
            for (int i = 0; i < plattScaling.length; ++i) plattScaling[i] = b.readDouble();

            final ClassyfireProperty[] klasses = new ClassyfireProperty[b.readInt()];
            for (int i = 0; i < klasses.length; ++i) {
                int id =  b.readInt();
                int parent = b.readInt();
                String name = b.readUTF();
                String desc = b.readUTF();
                klasses[i] = new ClassyfireProperty(id, name, desc, parent, 0); // TODO: fix priority
            }

            final ClassyFireFingerprintVersion version = new ClassyFireFingerprintVersion(klasses);
            final MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(version);
            int n = b.readInt();
            builder.disableAll();
            for (int i=0; i < n; ++i)
                builder.enable(b.readInt());
            final MaskedFingerprintVersion v = builder.toMask();
            CdkFingerprintVersion cdkFingerprintVersion = null;
            MaskedFingerprintVersion cdkMask = null;
            if (additionalFingerprintsEnabled) {
                final long id = b.readLong();
                final CdkFingerprintVersion allFingerprints = CdkFingerprintVersion.getFromBitsetIdentifier(id);
                final MaskedFingerprintVersion.Builder builder2 = MaskedFingerprintVersion.buildMaskFor(allFingerprints);
                builder2.disableAll();
                int size = b.readInt();
                for (int i=0; i < size; ++i)
                    builder2.enable(b.readInt());
                cdkFingerprintVersion = allFingerprints;
                cdkMask = builder2.toMask();
            }


            return new Canopus(formulaLayers, fingerprintLayers, innerLayers, outputLayer, plattLayer, formulaCentering, formulaScaling, plattCentering, plattScaling, v, cdkMask);
        }
    }

    public void replaceToRelu() {
        for (FullyConnectedLayer l : formulaLayers) {
            l.setActivationFunction(new ActivationFunction.ReLu());
        }
        for (FullyConnectedLayer l : fingerprintLayers) {
            l.setActivationFunction(new ActivationFunction.ReLu());
        }
        for (FullyConnectedLayer l : innerLayers) {
            l.setActivationFunction(new ActivationFunction.ReLu());
        }
    }




}
