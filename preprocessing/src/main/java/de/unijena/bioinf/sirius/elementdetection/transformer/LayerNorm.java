package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.ByteBuffer;

public class LayerNorm {

    private final float[] bias, scale;

    public LayerNorm(float[] scale, float[] bias) {
        this.bias = bias;
        this.scale = scale;
    }

    public void write(ByteBuffer buffer) {
        IOUtils.writeVector(buffer, scale);
        IOUtils.writeVector(buffer, bias);
    }
    public static LayerNorm read(ByteBuffer buffer) {
        float[] scale = IOUtils.readVector(buffer);
        float[] bias = IOUtils.readVector(buffer);
        return new LayerNorm(scale, bias);
    }

    public void computeInplace(float[] input) {
        final double epsilon = 1e-5;
        double _mean = 0;
        for (int i=0; i < input.length; ++i) _mean += input[i];
        final double mean = (_mean / input.length);

        double _var = 0;
        for (int i=0; i < input.length; ++i) _var += (input[i]-mean)*(input[i]-mean);
        final double std = Math.sqrt((_var/input.length)+epsilon);
        for (int i=0; i < input.length; ++i) {
            input[i] = (float) (((input[i] - mean)/std) * scale[i] + bias[i]);
        }
    }


}
