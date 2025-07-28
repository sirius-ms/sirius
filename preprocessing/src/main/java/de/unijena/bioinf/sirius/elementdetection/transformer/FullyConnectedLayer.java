package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.ByteBuffer;

public class FullyConnectedLayer {

    private final float[] matrix;
    private final float[] bias;
    private final Activation activation;

    public void write(ByteBuffer buffer) {
        buffer.putInt(activation.ordinal());
        IOUtils.writeVector(buffer, matrix);
        IOUtils.writeVector(buffer, bias);
    }
    public static FullyConnectedLayer read(ByteBuffer buffer) {
        int act = buffer.getInt();
        Activation activation = Activation.values()[act];
        float[] weights = IOUtils.readVector(buffer);
        float[] bias = IOUtils.readVector(buffer);
        return new FullyConnectedLayer(weights, bias, activation);
    }

    public FullyConnectedLayer(float[] matrix, float[] bias, Activation activation) {
        this.matrix = matrix;
        this.bias = bias;
        this.activation = activation;
    }

    public float[] compute(float[] input) {
        final int noutput = bias.length;
        final int ninput = input.length;
        final float[] out = new float[noutput];
        int p=0;
        for (int i=0; i < noutput; ++i) {
            for (int j=0; j < ninput; ++j) {
                out[i] += input[j]*matrix[p++];
            }
            out[i] = activation.apply(out[i] + bias[i]);

        }
        return out;
    }

    public int inputSize() {
        return matrix.length/bias.length;
    }
    public int outputSize() {
        return bias.length;
    }
}
