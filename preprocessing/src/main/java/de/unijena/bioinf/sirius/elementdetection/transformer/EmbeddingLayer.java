package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class EmbeddingLayer {

    private final float[][] embeddings;

    public static EmbeddingLayer read(ByteBuffer buffer) {
        return new EmbeddingLayer(IOUtils.readMatrix(buffer));
    }
    public void write(ByteBuffer buffer) {
        IOUtils.writeMatrix(buffer, embeddings);
    }

    public EmbeddingLayer(float[][] embeddings) {
        this.embeddings = embeddings;
    }

    public float[] lookup(int index) {
        return embeddings[index];
    }

    public float[][] lookup(int[] indices) {
        float[][] mx = new float[indices.length][];
        for (int i=0; i < indices.length; ++i) {
            mx[i] = embeddings[indices[i]];
        }
        return mx;
    }

    public int hiddenSize() {
        return embeddings[0].length;
    }

    public int size() {
        return embeddings.length;
    }

}
