package de.unijena.bioinf.sirius.elementdetection.transformer;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.DetectedElements;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class TransformerBasedPredictor {
    private EmbeddingLayer positionalEmbedding, orderedEmbedding, centeredEmbedding;
    private FourierEncoder fourierMassDefect, fourierIntensity;
    private FullyConnectedLayer projIn1, projIn2;
    private Transformer[] transformers;
    private FullyConnectedLayer projOut, monoisotopicOut;
    private Transformer basePeakPredictor;

    public static TransformerBasedPredictor read(ByteBuffer buffer) {
        EmbeddingLayer positionalEmbedding, orderedEmbedding, centeredEmbedding;
        FourierEncoder fourierMassDefect, fourierPrecursor, fourierIntensity;
        FullyConnectedLayer projIn1, projIn2;
        Transformer[] transformers;
        FullyConnectedLayer projOut, monoisotopicOut;
        positionalEmbedding=EmbeddingLayer.read(buffer);
        orderedEmbedding=EmbeddingLayer.read(buffer);
        centeredEmbedding=EmbeddingLayer.read(buffer);
        fourierMassDefect=FourierEncoder.read(buffer);
        fourierIntensity = FourierEncoder.read(buffer);
        projIn1=FullyConnectedLayer.read(buffer);
        projIn2=FullyConnectedLayer.read(buffer);
        projOut=FullyConnectedLayer.read(buffer);
        monoisotopicOut=FullyConnectedLayer.read(buffer);
        byte magic = buffer.get();
        if (magic!=42) throw new RuntimeException("Misalignment.");
        int transf = buffer.getInt();
        transformers = new Transformer[transf];
        for (int i=0; i < transformers.length; ++i) {
            transformers[i] = Transformer.read(buffer);
        }
        Transformer basePeakPredictor = Transformer.read(buffer);
        return new TransformerBasedPredictor(positionalEmbedding,orderedEmbedding,centeredEmbedding,fourierMassDefect,
                fourierIntensity,projIn1,projIn2,transformers,basePeakPredictor, projOut,monoisotopicOut);
    }
    public void write(ByteBuffer buffer) {
        positionalEmbedding.write(buffer);
        orderedEmbedding.write(buffer);
        centeredEmbedding.write(buffer);
        fourierMassDefect.write(buffer);
        fourierIntensity.write(buffer);
        projIn1.write(buffer);
        projIn2.write(buffer);
        projOut.write(buffer);
        monoisotopicOut.write(buffer);
        buffer.put((byte)42);
        buffer.putInt(transformers.length);
        for (int k=0; k < transformers.length; ++k) {
            transformers[k].write(buffer);
        }
        basePeakPredictor.write(buffer);
    }

    public static TransformerBasedPredictor readFromBinary(File file) throws IOException {
        try (final FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
            channel.read(buffer);
            buffer.flip();
            return read(buffer);
        }
    }

    public static TransformerBasedPredictor readFromTxt(File dir) throws IOException {
        // read embedding layers
        EmbeddingLayer pos = new EmbeddingLayer(readFloatMatrixTxt(dir, "positional_embedding.weight").vectors.toArray(float[][]::new));
        EmbeddingLayer order = new EmbeddingLayer(readFloatMatrixTxt(dir, "ordered_embedding.weight").vectors.toArray(float[][]::new));
        EmbeddingLayer center = new EmbeddingLayer(readFloatMatrixTxt(dir, "centered_embedding.weight").vectors.toArray(float[][]::new));
        // read fourier frequencies
        FourierEncoder massDefect = new FourierEncoder(readDoubles(dir, "mzfourier"));
        FourierEncoder intens = new FourierEncoder(readDoubles(dir, "intfourier"));
        // read fully connected layers
        FullyConnectedLayer projIn1 = new FullyConnectedLayer(
            readFloatMatrixTxt(dir, "proj_in.0.weight").data,
                readFloatMatrixTxt(dir, "proj_in.0.bias").data,
                Activation.RELU
        );
        FullyConnectedLayer projIn2 = new FullyConnectedLayer(
                readFloatMatrixTxt(dir, "proj_in.3.weight").data,
                readFloatMatrixTxt(dir, "proj_in.3.bias").data,
                Activation.IDENTITY
        );
        FullyConnectedLayer projOut = new FullyConnectedLayer(
                readFloatMatrixTxt(dir, "proj_out.weight").data,
                readFloatMatrixTxt(dir, "proj_out.bias").data,
                Activation.IDENTITY
        );
        FullyConnectedLayer monoOut = new FullyConnectedLayer(
                readFloatMatrixTxt(dir, "monoisotopic.weight").data,
                readFloatMatrixTxt(dir, "monoisotopic.bias").data,
                Activation.IDENTITY
        );
        // read transformers
        final int[] heads = FileUtils.readAsIntVector(FileUtils.getReader(new File(dir, "heads.txt")));
        List<Transformer> transformers = new ArrayList<>();
        for (int t = 0; t < heads.length; ++t) {
            String T = (t < heads.length-1) ? "transformers.seq." + t + "." : "basePeakBlock.";
            if (!new File(dir, T + "K.bias.txt").exists()) break;

            FullyConnectedLayer Q = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "Q.weight").data,
                    readFloatMatrixTxt(dir, T + "Q.bias").data,
                    Activation.IDENTITY
            );
            FullyConnectedLayer K = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "K.weight").data,
                    readFloatMatrixTxt(dir, T + "K.bias").data,
                    Activation.IDENTITY
            );
            FullyConnectedLayer V = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "V.weight").data,
                    readFloatMatrixTxt(dir, T + "V.bias").data,
                    Activation.IDENTITY
            );
            FullyConnectedLayer PROJ = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "out.weight").data,
                    readFloatMatrixTxt(dir, T + "out.bias").data,
                    Activation.IDENTITY
            );
            FullyConnectedLayer MLP1 = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "mlp.0.weight").data,
                    readFloatMatrixTxt(dir, T + "mlp.0.bias").data,
                    Activation.RELU
            );
            FullyConnectedLayer MLP2 = new FullyConnectedLayer(
                    readFloatMatrixTxt(dir, T + "mlp.3.weight").data,
                    readFloatMatrixTxt(dir, T + "mlp.3.bias").data,
                    Activation.IDENTITY
            );
            LayerNorm NORM1 = new LayerNorm(
                    readFloatMatrixTxt(dir, T+"norm1.weight").data,
                    readFloatMatrixTxt(dir, T+"norm1.bias").data
            );

            transformers.add(new Transformer(
                    Q, K, V, PROJ, heads[t], MLP1, MLP2,NORM1
            ));
        }
        Transformer basePeakPredictor = transformers.removeLast();
        return new TransformerBasedPredictor(pos, order, center, massDefect, intens, projIn1, projIn2,
                transformers.toArray(Transformer[]::new), basePeakPredictor, projOut, monoOut);
    }

    private record FloatMatrix (int rows, int cols, float[] data, List<float[]> vectors) {

    };

    private static double[] readDoubles(File dir, String name) throws IOException {
        return Arrays.stream(FileUtils.readLines(new File(dir,name+".txt"))).mapToDouble(Double::parseDouble).toArray();
    }

    private static FloatMatrix readFloatMatrixTxt(File dir, String name) throws IOException {
        final List<float[]> vecs = new ArrayList<>();
        int len=0;
        for (String line : FileUtils.readLines(new File(dir,name+".txt"))) {
            String[] split = line.split(" ");
            float[] vec = new float[split.length];
            for (int i=0; i < split.length; ++i) vec[i] = Float.parseFloat(split[i]);
            vecs.add(vec);
            len += vec.length;
        }
        final float[] matrix = new float[len];
        int pos=0;
        for (float[] v : vecs) {
            System.arraycopy(v, 0, matrix, pos, v.length);
            pos += v.length;
        }
        return new FloatMatrix(vecs.size(), vecs.get(0).length, matrix, vecs);
    }


    public TransformerBasedPredictor(EmbeddingLayer positionalEmbedding, EmbeddingLayer orderedEmbedding, EmbeddingLayer centeredEmbedding, FourierEncoder fourierMassDefect,
                                     FourierEncoder fourierIntensity, FullyConnectedLayer projIn1, FullyConnectedLayer projIn2,
                                     Transformer[] transformers, Transformer basePeakPredictor, FullyConnectedLayer projOut, FullyConnectedLayer monoisotopicOut) {
        this.positionalEmbedding = positionalEmbedding;
        this.orderedEmbedding = orderedEmbedding;
        this.centeredEmbedding = centeredEmbedding;
        this.fourierMassDefect = fourierMassDefect;
        this.fourierIntensity = fourierIntensity;
        this.projIn1 = projIn1;
        this.projIn2 = projIn2;
        this.transformers = transformers;
        this.projOut = projOut;
        this.monoisotopicOut = monoisotopicOut;
        this.basePeakPredictor = basePeakPredictor;
    }

    /**
     * Predict the elemental composition of an isotope pattern assuming the monoisotopic peak equals to the given parameter.
     */
    public Optional<TransformerPrediction> predict(SimpleSpectrum pattern, int peak) {
        final float[][] transformed = transform(pattern, 1);
        final float[] peakType = monoisotopicOut.compute(transformed[peak]);
        final float mono = peakType[peakType.length-1];
        if (mono < MONOISOTOPIC_THRESHOLD) return Optional.empty();
        float[] prediction = projOut.compute(transformed[peak]);
        return Optional.of(builtPredictionResult(peak, pattern.size(), mono, prediction));
    }

    /**
     * Predict the elemental composition of an isotope pattern. The pattern consists of consecutive peaks that
     * may be noise or isotope peaks belonging to possibly overlapping patterns. The method returns a list of predictions
     * where each prediction assumes a different monoisopic peak. Only the most likely predictions (usually just one)
     * are returned. May return an empty list of the whole pattern is detected as noise.
     */
    public TransformerPrediction[] predict(SimpleSpectrum pattern) {
        final float[][] transformed = transform(pattern,1);

        // predict monoisotopics
        float[] mono = new float[pattern.size()];
        for (int i=0; i < mono.length; ++i) {
            float[] compute = monoisotopicOut.compute(transformed[i]);
            mono[i] = compute[compute.length-1];
        }
        // predict elements
        float[][] elements = new float[pattern.size()][];
        for (int i=0; i < elements.length; ++i) {
            if (mono[i] >= MONOISOTOPIC_THRESHOLD) {
                elements[i] = projOut.compute(transformed[i]);
            }
        }
        return builtPredictionResults(mono, elements, pattern);
    }

    public float[][] transform(SimpleSpectrum pattern, int polarity) {
        final double[] mz = new double[pattern.size()];
        final double[] intensity = new double[pattern.size()];
        final int[] positions = new int[pattern.size()];
        final int[] order = new int[pattern.size()];
        final int[] centeredPositions = new int[pattern.size()];
        {
            double center = 0d;
            double intsum = 0d;
            double[] squaredInt = new double[intensity.length];
            double squaredSum = 0d;
            for (int i=0;i<pattern.size(); ++i) {
                mz[i] = pattern.getMzAt(i);
                intensity[i] = pattern.getIntensityAt(i);
                squaredInt[i] = intensity[i]*intensity[i];
                squaredSum += squaredInt[i];
                positions[i] = (int)Math.round(mz[i]-mz[0]);
                intsum += intensity[i];
            }
            for (int i=0;i<intensity.length; ++i) {
                intensity[i] /= intsum;
                center += squaredInt[i]*positions[i]/squaredSum;
            }
            int centerPos = (int)Math.round(center);
            for (int i=0;i<centeredPositions.length; ++i) {
                centeredPositions[i] = (positions[i]-centerPos)+(centeredEmbedding.size()/2);
            }
            final Integer[] order_ = new Integer[order.length];
            for (int i=0; i < order_.length; ++i) order_[i] = i;
            Arrays.sort(order_, Comparator.comparingDouble(x->-intensity[x]));
            for (int i=0; i < order_.length; ++i) order[i] = order_[i];
        }
        final float[][] peaks = new float[mz.length][];
        final float[][] fourierFeatures = new float[mz.length][fourierMassDefect.size() + fourierIntensity.size()];
        double baseInt = 0d;
        for (int i=0; i < peaks.length; ++i) baseInt = Math.max(baseInt, intensity[i]);
        // encode peaks
        for (int peak = 0; peak < mz.length; ++peak) {
            int pos=0;
            final float[] peakvec = new float[projIn1.inputSize()];
            {
                final float[] massDefects = fourierMassDefect.compute(mz[peak]);
                System.arraycopy(massDefects, 0, peakvec, pos, massDefects.length);
                System.arraycopy(massDefects, 0, fourierFeatures[peak], pos, massDefects.length);
                pos += massDefects.length;
            }
            {
                final float[] ints = fourierIntensity.compute(intensity[peak]);
                System.arraycopy(ints, 0, peakvec, pos, ints.length);
                System.arraycopy(ints, 0, fourierFeatures[peak], pos, ints.length);
                pos += ints.length;
            }
            {
                final float[] ps = positionalEmbedding.lookup(positions[peak]);
                System.arraycopy(ps, 0, peakvec, pos, ps.length);
                pos += ps.length;
            }
            {
                final float[] ps = centeredEmbedding.lookup(centeredPositions[peak]);
                System.arraycopy(ps, 0, peakvec, pos, ps.length);
                pos += ps.length;
            }
            {
                final float[] ps = orderedEmbedding.lookup(order[peak]);
                System.arraycopy(ps, 0, peakvec, pos, ps.length);
                pos += ps.length;
            }
            peakvec[pos++] = (float)(intensity[peak]/baseInt);
            peakvec[pos++] = (float)Math.log((intensity[peak]+0.005)/baseInt);
            peakvec[pos] = polarity*5;
            peaks[peak] = projIn2.compute(projIn1.compute(peakvec));
        }
        // run transformers
        float[][] transformed = peaks;
        for (int tr=0; tr < transformers.length; ++tr) {
            transformed = transformers[tr].compute(transformed, fourierFeatures);
        }
        return transformed;
    }

    private static String[] labels = new String[]{"S","Cl","Br","B","F","Se","Fe","Zn","Mg", "Si"};
    private static Element[] predictableElements = Arrays.asList("S","Cl", "Br", "B", "Se", "Fe", "Zn", "Mg", "Si").stream().map(x-> PeriodicTable.getInstance().getByName(x)).toArray(Element[]::new);
    protected static float[] PREDICTOR_THRESHOLDS = MatrixUtils.double2float(Arrays.stream(predictableElements).mapToDouble(x->x.getSymbol().equals("S") || x.getSymbol().equals("F") ? 0.2 : 0.33).toArray());
    protected static float FLUOR_THRESHOLD = 0.2f;
    private static Set<Element> predictableElementSet = Set.of(predictableElements);
    private static int[] labelPos = new int[]{0,1,2,3,5,6,7,8,9};
    private static int fluorPos = 4, sulfurPos = 0;

    private static float MONOISOTOPIC_THRESHOLD = 0f;

    public Element[] getPredictableElements() {
        return predictableElements;
    }

    private TransformerPrediction builtPredictionResult(int k, int patternLen, float mono, float[] elements) {
        final float[] probs = new float[labelPos.length];
        float maxProb = 0f;
        for (int j=0; j < probs.length; ++j) {
            probs[j] = elements[labelPos[j]];
            maxProb = Math.max(probs[j],maxProb);
        }

        // special rule: sulfur predictions requires 3 peaks to be reliable. Even though the predictor might be
        // confident some time with two peaks, it is not worth the hustle. We rather enable sulfur ALWAYS for
        // pattern with < 3 peaks as it does not do much damage anyways.
        // in theory we could check for the REAL pattern length (excluding noise), but again, it's not so important.
        DetectedElements.patternsWithLessThan3PeaksAlwaysIncludeSulfur(probs,patternLen,sulfurPos);

        return new TransformerPrediction(
                k, patternLen, mono, predictableElements, probs,
                elements[fluorPos]
        );
    }

    private TransformerPrediction[] builtPredictionResults(float[] mono, float[][] elements, SimpleSpectrum originalSpectrum) {
        List<TransformerPrediction> predictions = new ArrayList<>();
        int largest = -1;
        for (int k=0; k < mono.length; ++k) {
            if (mono[k] >= MONOISOTOPIC_THRESHOLD) {
                if (largest<0 || originalSpectrum.getIntensityAt(k)>originalSpectrum.getIntensityAt(largest)) {
                    largest=k;
                }
            }
        }

        for (int k=0; k < mono.length; ++k) {
            if (mono[k]>=MONOISOTOPIC_THRESHOLD) {
                predictions.add(builtPredictionResult(k, originalSpectrum.size(), mono[k], elements[k]));
            }
        }
        return predictions.toArray(TransformerPrediction[]::new);
    }

}
