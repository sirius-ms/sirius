package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.ByteBuffer;

public class FourierEncoder {
    private double[] frequencies;

    public static FourierEncoder read(ByteBuffer buffer)  {
        return new FourierEncoder(IOUtils.readDoubleVector(buffer));
    }
    public void write(ByteBuffer buffer) {
        IOUtils.writeVector(buffer, frequencies);
    }

    public FourierEncoder(double[] frequencies) {
        this.frequencies = frequencies;
    }

    public float[] compute(double input) {
        final float[] outp = new float[frequencies.length*2];
        for (int i=0; i < frequencies.length; ++i) {
            final double x = frequencies[i]*input;
            outp[i] = (float)Math.sin(x);
            outp[i+frequencies.length] = (float)Math.cos(x);
        }
        return outp;
    }
}
