package de.unijena.bioinf.sirius.elementdetection.transformer;

import java.nio.*;

public class IOUtils {

    public static void writeMatrix(ByteBuffer buffer, float[][] matrix) {
        int rows = matrix.length;
        int cols = rows>0 ? matrix[0].length : 0;
        buffer.putInt(rows);
        buffer.putInt(cols);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        for (int k=0; k < rows; ++k) {
            if (matrix[k].length!=cols) {
                throw new IllegalArgumentException("Method expects proper matrix with all rows have same size.");
            }
            floatBuffer.put(matrix[k], 0, cols);
        }
        moveFloat(buffer,floatBuffer);
    }
    public static float[][] readMatrix(ByteBuffer buffer) {
        int rows = buffer.getInt();
        int cols = buffer.getInt();
        float[][] matrix = new float[rows][cols];
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        for (int k=0; k < rows; ++k) {
            floatBuffer.get(matrix[k]);
        }
        moveFloat(buffer,floatBuffer);
        return matrix;
    }

    public static void writeVector(ByteBuffer buffer, float[] vector) {
        buffer.putInt(vector.length);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(vector,0,vector.length);
        moveFloat(buffer,floatBuffer);
    }
    public static float[] readVector(ByteBuffer buffer) {
        int size = buffer.getInt();
        float[] vec = new float[size];
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.get(vec);
        moveFloat(buffer,floatBuffer);
        return vec;
    }
    public static void writeVector(ByteBuffer buffer, double[] vector) {
        buffer.putInt(vector.length);
        DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
        doubleBuffer.put(vector,0,vector.length);
        moveDouble(buffer,doubleBuffer);
    }
    public static double[] readDoubleVector(ByteBuffer buffer) {
        int size = buffer.getInt();
        double[] vec = new double[size];
        DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
        doubleBuffer.get(vec);
        moveDouble(buffer,doubleBuffer);
        return vec;
    }

    public static void moveDouble(ByteBuffer buffer, DoubleBuffer otherBuffer) {
        buffer.position(buffer.position() + otherBuffer.position()*Double.BYTES);
    }
    public static void moveShort(ByteBuffer buffer, ShortBuffer otherBuffer) {
        buffer.position(buffer.position() + otherBuffer.position()*Short.BYTES);
    }
    public static void moveInt(ByteBuffer buffer, IntBuffer otherBuffer) {
        buffer.position(buffer.position() + otherBuffer.position()*Integer.BYTES);
    }
    public static void moveFloat(ByteBuffer buffer, FloatBuffer otherBuffer) {
        buffer.position(buffer.position() + otherBuffer.position()*Float.BYTES);
    }

}
