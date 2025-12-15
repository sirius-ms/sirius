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

    // these are things I like to outsource to gemini ^^
    public float[][] batchComputeConcatenated(float[][] Xa, float[][] Xb) {
            final int batchSize = Xa.length;
            // Assume Xa and Xb have consistent dimensions across the batch
            final int dimA = Xa[0].length;
            final int dimB = Xb[0].length;
            final int noutput = bias.length;

            // Validate weight matrix size covers both inputs (DimA + DimB)
            if (matrix.length != noutput * (dimA + dimB)) {
                throw new IllegalArgumentException("Weight matrix size mismatch");
            }

            // Allocate 2D result array
            final float[][] result = new float[batchSize][noutput];

            // 1. Outer Loop: Iterate through each batch item
            for (int b = 0; b < batchSize; b++) {

                // Cache current input vectors
                final float[] vecA = Xa[b];
                final float[] vecB = Xb[b];

                // Reference the specific output row for this batch item
                final float[] outRow = result[b];

                // Reset weight pointer for every new batch item.
                // We will traverse the entire weight matrix for every vector in the batch.
                int matrixPtr = 0;

                // 2. Inner Loop: Iterate through each output neuron
                for (int i = 0; i < noutput; i++) {

                    // Start accumulator with bias
                    float sum = bias[i];

                    // 3. Phase A: Dot product with Xa part
                    // matrixPtr reads sequentially from the start of the row
                    for (int j = 0; j < dimA; j++) {
                        sum += vecA[j] * matrix[matrixPtr++];
                    }

                    // 4. Phase B: Dot product with Xb part
                    // matrixPtr continues sequentially seamlessly
                    for (int j = 0; j < dimB; j++) {
                        sum += vecB[j] * matrix[matrixPtr++];
                    }

                    // Write result sequentially into the output row
                    outRow[i] = activation.apply(sum);
                }
            }

            return result;
        }

    public float[] flatBatchCompute(float[][] inputBatch) {
        final int batchSize = inputBatch.length;
        final int noutput = bias.length;
        final int ninput = inputBatch[0].length;
        final float[] result = new float[batchSize * noutput];
        int q = 0;
        for (int b = 0; b < batchSize; b++) {
            final float[] inputVector = inputBatch[b];
            int matrixPtr = 0;
            for (int i = 0; i < noutput; i++) {
                float sum = bias[i];
                for (int j = 0; j < ninput; j++) {
                    sum += inputVector[j] * matrix[matrixPtr++];
                }
                result[q++] = activation.apply(sum);
            }
        }

        return result;
    }

    public float[][] batchCompute(float[][] inputBatch) {
        final int batchSize = inputBatch.length;
        final int ninput = inputBatch[0].length;
        final int noutput = bias.length;
        final float[][] output = new float[batchSize][noutput];
        for (int i = 0; i < noutput; i++) {
            int rowStart = i * ninput;
            for (int b = 0; b < batchSize; b++) {
                float sum = 0.0f;
                float[] inputVector = inputBatch[b];
                for (int j = 0; j < ninput; j++) {
                    sum += inputVector[j] * matrix[rowStart + j];
                }
                output[b][i] += sum;
            }
        }
        // apply activation
        for (int i=0; i < output.length; ++i) {
            float[] vec = output[i];
            for (int j=0; j < vec.length; ++j) {
                vec[j] = activation.apply(vec[j] + bias[j]);
            }
        }
        return output;
    }

    public float[] compute(float[] input) {
        final int noutput = bias.length;
        final int ninput = input.length;
        final float[] out = new float[noutput];
        int p=0;
        for (int i=0; i < noutput; ++i) {
            float acum = 0f;
            for (int j=0; j < ninput; ++j) {
                acum += input[j]*matrix[p++];
            }
            out[i] = activation.apply(acum + bias[i]);

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
