package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Transformer {

    private FullyConnectedLayer Q, K, V, proj;
    private int nheads;

    private FullyConnectedLayer mlp1,mlp2;
    private LayerNorm norm1;

    public Transformer(FullyConnectedLayer q, FullyConnectedLayer k, FullyConnectedLayer v, FullyConnectedLayer proj, int nheads,
                       FullyConnectedLayer mlp1, FullyConnectedLayer mlp2, LayerNorm norm1) {
        Q = q;
        K = k;
        V = v;
        this.nheads = nheads;
        this.mlp1 = mlp1;
        this.mlp2 = mlp2;
        this.norm1 = norm1;
        this.proj = proj;
    }

    public void write(ByteBuffer buffer) {
        buffer.putInt(nheads);
        Q.write(buffer);
        K.write(buffer);
        V.write(buffer);
        proj.write(buffer);
        mlp1.write(buffer);
        mlp2.write(buffer);
        norm1.write(buffer);
    }
    public static Transformer read(ByteBuffer buffer) {
        int nheads = buffer.getInt();
        FullyConnectedLayer q,k,v,proj,mlp1,mlp2;
        LayerNorm n1;
        q = FullyConnectedLayer.read(buffer);
        k=FullyConnectedLayer.read(buffer);
        v=FullyConnectedLayer.read(buffer);
        proj =FullyConnectedLayer.read(buffer);
        mlp1=FullyConnectedLayer.read(buffer);
        mlp2=FullyConnectedLayer.read(buffer);
        n1=LayerNorm.read(buffer);
        return new Transformer(q,k,v,proj,nheads,mlp1,mlp2,n1);
    }

    public float[][] compute(float[][] input, float[][] fourierFeatures) {
        float[][] output = new float[input.length][];

        float[][] q,k,v;

        if (input.length>0 && input[0].length== Q.inputSize()) {
            q = new float[input.length][]; k = new float[input.length][]; v = new float[input.length][];
            for (int vec = 0; vec < input.length; vec++) {
                float[] embed = input[vec].clone();
                norm1.computeInplace(embed);
                q[vec] = Q.compute(embed);
                k[vec] = K.compute(embed);
                v[vec] = V.compute(embed);
            }
        } else {
            float[][] embed = Arrays.stream(input).map(x->norm1.compute(x)).toArray(float[][]::new);
            q = Q.batchComputeConcatenated(embed, fourierFeatures);
            k = K.batchComputeConcatenated(embed, fourierFeatures);
            v = V.batchCompute(embed);
        }
        final int headlen = v[0].length/nheads;
        for (int vec = 0; vec < input.length; ++vec) {
            final float[] x = input[vec];
            final float[] value = new float[v[0].length];
            final double[] attention = new double[input.length];
            int headstart = 0;
            for (int h=0; h < nheads; ++h) {
                double totalAttention = 0;
                double maxAttention = 0;
                final double sqrtdim = Math.sqrt(headlen);
                for (int i=0; i < attention.length; ++i) {
                    attention[i] = dot(q[vec], k[i], headstart, headlen)/sqrtdim;
                    maxAttention = Math.max(attention[i], maxAttention);
                }

                // softmax
                for (int i=0; i < attention.length; ++i) {
                    attention[i] = Math.exp(attention[i]-maxAttention);
                    totalAttention += attention[i];
                }
                for (int i=0; i < attention.length; ++i) {
                    attention[i] /= totalAttention;
                    for (int j=headstart, jn = headstart+headlen; j < jn; ++j) {
                        value[j] += (float)(v[i][j]*attention[i]);
                    }
                }
                // compute value vector
                headstart += headlen;
            }
            final float[] context = proj.compute(value);
            for (int i=0; i < x.length; ++i) {
                x[i] += context[i];
            }
            addInplace(x, mlp2.compute(mlp1.compute(x)));
            output[vec] = x;
        }
        return output;
    }

    protected float[] computeAttentionScores(float[][] input, double[] mz, double[] ints) {
        return null;
    }

    private static void addInplace(float[] left, float[] right){
        for (int i=0; i < left.length; ++i) {
            left[i]+=right[i];
        }
    }

    private static double dot(float[] query, float[] key, int startFrom, int len) {
        double d = 0d;
        for (int k=startFrom, n = startFrom+len; k < n; ++k) {
            d += query[k]*key[k];
        }
        return d;
    }
}