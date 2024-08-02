/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.dnn.FullyConnectedLayer;
import de.unijena.bioinf.canopus.dnn.PlattLayer;
import org.ejml.data.FMatrixRMaj;

import java.io.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Canopus {

    public static enum Predictable {
        ClassyFire, NPC, Fingerprint; // order is important
    };

    public static final boolean BINARIZE = false, CLIPPING = false;

    protected FullyConnectedLayer[] formulaLayers;
    protected FullyConnectedLayer[] fingerprintLayers;
    protected FullyConnectedLayer[] innerLayers;
    protected FullyConnectedLayer outputLayer;
    protected PlattLayer plattLayer, npcPlattLayer;
    protected FullyConnectedLayer npcLayer;

    protected double[] formulaScaling, formulaCentering, plattScaling, plattCentering;
    protected ClassyFireFingerprintVersion classyFireFingerprintVersion;
    protected MaskedFingerprintVersion classyFireMask;
    protected NPCFingerprintVersion npcFingerprintVersion;
    protected MaskedFingerprintVersion npcMask;
    protected CdkFingerprintVersion cdkFingerprintVersion;
    protected MaskedFingerprintVersion cdkMask;

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

    protected Canopus(FullyConnectedLayer[] formulaLayers, FullyConnectedLayer[] fingerprintLayers, FullyConnectedLayer[] innerLayers, FullyConnectedLayer outputLayer, PlattLayer plattLayer, double[] formulaCentering, double[] formulaScaling, double[] plattCentering, double[] plattScaling, MaskedFingerprintVersion classyFireMask, MaskedFingerprintVersion cdkMask, MaskedFingerprintVersion npcMask, FullyConnectedLayer npcLayer, PlattLayer npcPlatt) {
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
        this.npcLayer = npcLayer;
        this.npcPlattLayer = npcPlatt;
        this.npcMask = npcMask;
        this.npcFingerprintVersion = npcMask==null ? null : (NPCFingerprintVersion)npcMask.getMaskedFingerprintVersion();
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

    public MaskedFingerprintVersion getNPCMask() {
        return npcMask;
    }

    public NPCFingerprintVersion getNpcFingerprintVersion() {
        return npcFingerprintVersion;
    }

    public MaskedFingerprintVersion getCanopusMask() {
        return classyFireMask;
    }

    public ClassyFireFingerprintVersion getClassyFireFingerprintVersion() {
        return classyFireFingerprintVersion;
    }

    /**
     * Predicts the latent vector - this is the non-linear transformation of the input from which all
     * classifications are predicted in a strictly linear fashion.
     */
    public float[] predictLatentVector(MolecularFormula formula, ProbabilityFingerprint fingerprint) {
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
            if (BINARIZE) {
                fp[i] = Math.round(fp[i]);
            } else if (CLIPPING) {
                fp[i] = (Math.min(Math.max(fp[i],0.2d),1.0d)-0.2d)/0.6d;
            }
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
        return combinedVector.data;
    }

    public float[][] predictDecisionValues(MolecularFormula formula, ProbabilityFingerprint fingerprint, EnumSet<Predictable> topredict) {
        float[] vec = predictLatentVector(formula, fingerprint);
        final FMatrixRMaj latent = FMatrixRMaj.wrap(1, vec.length, vec);
        float[][] buffs = new float[topredict.size()][];
        int k=0;
        if (topredict.contains(Predictable.ClassyFire)) {
            buffs[k] = this.outputLayer.eval(latent).data;
            ++k;
        }
        if (topredict.contains(Predictable.Fingerprint)) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        if (topredict.contains(Predictable.NPC)) {
            buffs[k] = this.npcLayer.eval(latent).data;
            ++k;
        }
        return buffs;
    }
    public float[][] predictProbabilities(MolecularFormula formula, ProbabilityFingerprint fingerprint, EnumSet<Predictable> topredict) {
        final float[][] values = predictDecisionValues(formula, fingerprint, topredict);
        int k=0;
        if (topredict.contains(Predictable.ClassyFire)) {
            values[k] = this.plattLayer.eval(FMatrixRMaj.wrap(1, values[k].length, values[k])).data;
            ++k;
        }
        if (topredict.contains(Predictable.Fingerprint)) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        if (topredict.contains(Predictable.NPC)) {
            values[k] = this.npcPlattLayer.eval(FMatrixRMaj.wrap(1, values[k].length, values[k])).data;
            ++k;
        }
        return values;
    }
    public ProbabilityFingerprint predictFingerprint(MolecularFormula formula, ProbabilityFingerprint fingerprint, Predictable topredict) {
        return predictFingerprints(formula,fingerprint,EnumSet.of(topredict))[0];
    }
    public ProbabilityFingerprint[] predictFingerprints(MolecularFormula formula, ProbabilityFingerprint fingerprint, EnumSet<Predictable> topredict) {
        final float[][] values = predictProbabilities(formula, fingerprint, topredict);
        final ProbabilityFingerprint[] fps = new ProbabilityFingerprint[topredict.size()];
        int k=0;
        if (topredict.contains(Predictable.ClassyFire)) {
            fps[k] = new ProbabilityFingerprint(classyFireMask, values[k]);
            ++k;
        }
        if (topredict.contains(Predictable.Fingerprint)) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        if (topredict.contains(Predictable.NPC)) {
            fps[k] = npcMask.mask(new ProbabilityFingerprint(npcFingerprintVersion, values[k]));
            ++k;
        }
        return fps;

    }



    public double[] getNormalizedFormulaVector(MolecularFormula formula) {
        final double[] ff = getFormulaFeatures(formula);
        // normalize/center
        for (int i=0; i < ff.length; ++i) {
            ff[i] -= formulaCentering[i];
            ff[i] /= formulaScaling[i];
        }
        return ff;
    }

    public double[] getNormalizedFingerprintVector(ProbabilityFingerprint fingerprint) {
        final double[] fp = fingerprint.toProbabilityArray();
        for (int i=0; i < fp.length; ++i) {
            fp[i] -= plattCentering[i];
            fp[i] /= plattScaling[i];
            if (BINARIZE) {
                fp[i] = Math.round(fp[i]);
            } else if (CLIPPING) {
                fp[i] = (Math.min(Math.max(fp[i],0.2d),1.0d)-0.2d)/0.6d;
            }
        }
        return fp;
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

        // CHECK FOR npc Layer
        if (npcLayer != null) {
            bstream.writeInt(2887);
        } else {
            bstream.writeInt(4887);
        }

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

        if (npcLayer!=null) {
            npcLayer.dump(bstream);
            npcPlattLayer.dump(bstream);
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

            final boolean hasNPCLayer;
            final int anotherMagicNumber = b.readInt();
            if (anotherMagicNumber == 4887) {
                hasNPCLayer = false;
            } else if (anotherMagicNumber == 2887) {
                hasNPCLayer = true;
            } else {
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
                klasses[i] = new ClassyfireProperty(id, name, desc, parent, 0, 0f); // TODO: fix priority
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

            FullyConnectedLayer npcLayer = null;
            PlattLayer npcPlatt = null;
            if (hasNPCLayer) {
                npcLayer = FullyConnectedLayer.load(b);
                npcPlatt = PlattLayer.load(b);
            }

            final NPCFingerprintVersion npc = NPCFingerprintVersion.get();

            return new Canopus(formulaLayers, fingerprintLayers, innerLayers, outputLayer, plattLayer, formulaCentering, formulaScaling, plattCentering, plattScaling, v, cdkMask, MaskedFingerprintVersion.allowAll(npc),  npcLayer, npcPlatt);
        }
    }

    public void setPlattCalibration(double[] As, double[] Bs) {
        this.plattLayer = new PlattLayer(As, Bs);
    }

    /*
    public InputGradient getInputGradientForClassyFireProperty(ProbabilityFingerprint fingerprint, MolecularFormula formula, ClassyfireProperty property) {
        final int indexOfMolecularProperty = classyFireFingerprintVersion.getIndexOfMolecularProperty(property);
        return getInputGradientForClassyFireProperty(fingerprint,formula,indexOfMolecularProperty);

    }


    public InputGradient getInputGradientForClassyFireProperty(ProbabilityFingerprint fingerprint, MolecularFormula formula, int absoluteIndex) {
        final int relativeIndex = classyFireMask.getRelativeIndexOf(absoluteIndex);
        final ArrayList<FMatrixRMaj> intermediatesFormula = new ArrayList<>(),
                intermediatesFP = new ArrayList<>(), intermediates = new ArrayList<>();

        final double[] normalizedFormulaVector = getNormalizedFormulaVector(formula);
        final double[] normalizedFingerprintVector = getNormalizedFingerprintVector(fingerprint);
        final double[] formulaCoefficients = new double[normalizedFormulaVector.length];
        final double[] fingerprintCoefficients = new double[normalizedFingerprintVector.length];

        // start with formula layer
        FMatrixRMaj formulaInput = FMatrixRMaj.wrap(1, normalizedFingerprintVector.length, MatrixUtils.double2float(normalizedFormulaVector));
        FMatrixRMaj lastFormula = formulaInput;
        for (FullyConnectedLayer l : formulaLayers) {
            lastFormula = l.eval(lastFormula);
            intermediatesFormula.add(lastFormula);
        }
        // now fingerprint layers
        FMatrixRMaj fpInput = FMatrixRMaj.wrap(1, normalizedFingerprintVector.length, MatrixUtils.double2float(normalizedFingerprintVector));
        FMatrixRMaj lastFp = fpInput;
        for (FullyConnectedLayer l : fingerprintLayers) {
            lastFp = l.eval(lastFp);
            intermediatesFP.add(lastFp);
        }


        // combine both
        final FMatrixRMaj concatenated = FMatrixRMaj.wrap(1, lastFormula.numCols+lastFp.numCols, MatrixUtils.concat(lastFp.data,lastFormula.data));

        // now combined layers
        FMatrixRMaj lastCombined = concatenated;
        for (FullyConnectedLayer l : innerLayers) {
            lastCombined = l.eval(lastCombined);
            intermediates.add(lastCombined);
        }

        final double output = lastCombined.get(0, relativeIndex);

        // now we remove all zero elements from our weight matrices
        // those are the points where relu'(x) is zero.
        // we can ignore the other points as relu'(x) is just one for them.
        final TIntArrayList zeros = new TIntArrayList();

        // formulas
        FMatrixRMaj m = intermediatesFormula.get(0);
        FMatrixRMaj gradientFormula = formulaLayers[0].weightMatrix().copy();

        for (int k=1; k < formulaLayers.length; ++k) {
            zeros.clearQuick();
            // search for zero elements in M
            for (int i=0; i < m.data.length; ++i) {
                if (m.data[i]<=0) {
                    zeros.add(i);
                }
            }
            // replace with weight matrix
            m = intermediatesFormula.get(k);
            final FMatrixRMaj W = formulaLayers[k].weightMatrix().copy();
            intermediatesFormula.set(k, W);
            // set weight to zero
            for (int i=0; i < zeros.size(); ++i) {
                final int zero = zeros.getQuick(i);
                // delete the i-th row
                for (int j=0; j < W.numCols; ++j) {
                    W.unsafe_set(i, j, 0);
                }
            }
            CommonOps_FDRM.multInner(gradientFormula, W);
        }

        FMatrixRMaj conc = innerLayers[0].weightMatrix().copy();
        {
            zeros.clearQuick();
            for (int i=0; i < m.data.length; ++i) {
                if (m.data[i]<=0) {
                    zeros.add(i);
                }
            }
            m = concatenated;
            for ()
        }
    }

    public static final class InputGradient {

        public final double[] fingerprintGradient, formulaGradient;

        private InputGradient(double[] fingerprintGradient, double[] formulaGradient) {
            this.fingerprintGradient = fingerprintGradient;
            this.formulaGradient = formulaGradient;
        }
    }
*/

}
