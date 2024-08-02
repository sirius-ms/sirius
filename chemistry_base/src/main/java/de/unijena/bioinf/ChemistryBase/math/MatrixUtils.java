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

package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Comparator;

public class MatrixUtils {

    public static double dot(double[] U, double[] V) {
        double sum=0d;
        for (int i=0; i < V.length; ++i) sum += U[i]*V[i];
        return sum;
    }
    public static float dot(float[] U, float[] V) {
        double sum=0d;
        for (int i=0; i < V.length; ++i) sum += U[i]*V[i];
        return (float)sum;
    }

    public static float[] matmul(float[][] M, float[] columnVector, float[] result) {
        final int cols = M[0].length;
        final int rows = M.length;
        final int nrows = columnVector.length;
        if (cols != nrows) throw new IllegalArgumentException("Cannot multiply an " + rows + " x " + cols + " matrix with a " + nrows +  " column vector.");

        for (int row=0; row < M.length; ++row) {
            final float[] rowvec = M[row];
            result[row] = dot(rowvec,columnVector);
        }
        return result;
    }

    public static float[] matmul(float[][] M, float[] columnVector) {
        return matmul(M,columnVector,columnVector.clone());
    }

    public static double[] matmul(double[][] M, double[] columnVector) {
        return matmul(M,columnVector,columnVector.clone());
    }

    public static double[] matmul(double[][] M, double[] columnVector, double[] result) {
        final int cols = M[0].length;
        final int rows = M.length;
        final int nrows = columnVector.length;
        if (cols != nrows) throw new IllegalArgumentException("Cannot multiply an " + rows + " x " + cols + " matrix with a " + nrows +  " column vector.");
        for (int row=0; row < M.length; ++row) {
            final double[] rowvec = M[row];
            result[row] = dot(rowvec,columnVector);
        }
        return result;
    }

    public static double[][] matmul(double[][] M, double[][] N) {
        final int cols = M[0].length;
        final int rows = M.length;
        final int ncols = N[0].length;
        if (N.length != cols) throw new IllegalArgumentException("Cannot multiply an " + rows + " x " + cols + " matrix with an " + N.length + " x " + ncols +  " matrix.");

        final double[][] transposedN = transpose(N);
        final double[][] target = new double[rows][N[0].length];
        for (int row=0; row < M.length; ++row) {
            final double[] rowvec = M[row];
            for (int col=0; col < ncols; ++col) {
                final double[] colvec = transposedN[col];
                target[row][col] = dot(rowvec,colvec);
            }
        }
        return target;
    }
    public static float[][] matmul(float[][] M, float[][] N) {
        final int cols = M[0].length;
        final int rows = M.length;
        final int ncols = N[0].length;
        if (N.length != cols) throw new IllegalArgumentException("Cannot multiply an " + rows + " x " + cols + " matrix with an " + N.length + " x " + ncols +  " matrix.");

        final float[][] transposedN = transpose(N);
        final float[][] target = new float[rows][N[0].length];
        for (int row=0; row < M.length; ++row) {
            final float[] rowvec = M[row];
            for (int col=0; col < ncols; ++col) {
                final float[] colvec = transposedN[col];
                target[row][col] = dot(rowvec,colvec);
            }
        }
        return target;
    }

    public static double frobeniusProduct(double[][] M, double[][] N) {
        double prod = 0d;
        for (int i=0; i < M.length; ++i) {
            for (int j=0; j < M.length; ++j) {
                prod += M[i][j]*N[i][j];
            }
        }
        return prod;
    }


    public static double frobeniusNorm(double[][] M) {
        double norm = 0d;
        for (int i=0; i < M.length; ++i) {
            for (int j=0; j < M.length; ++j) {
                norm += M[i][j]*M[i][j];
            }
        }
        return norm;
    }

    public static double[] encodeBoolean(boolean[] vector, double falses, double trues) {
        final double[] v = new double[vector.length];
        for (int i=0; i < vector.length; ++i) v[i] = vector[i] ? trues : falses;
        return v;
    }
    public static double[][] encodeBoolean(boolean[][] matrix, double falses, double trues) {
        if (matrix.length==0) return new double[0][];
        final double[][] v = new double[matrix.length][matrix[0].length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                v[i][j] = matrix[i][j] ? trues : falses;
            }
        }
        return v;
    }

    public static double[] float2double(float[] vec) {
        final double[] v = new double[vec.length];
        for (int k=0; k < vec.length; ++k)
            v[k] = vec[k];
        return v;
    }
    public static double[][] float2double(float[][] matrix) {
        final double[][] v = new double[matrix.length][];
        for (int k=0; k < matrix.length; ++k) {
            v[k] = new double[matrix[k].length];
            for (int i=0; i < matrix[k].length; ++i) v[k][i] = matrix[k][i];
        }
        return v;
    }
    public static float[] double2float(double[] vec) {
        final float[] v = new float[vec.length];
        for (int k=0; k < vec.length; ++k)
            v[k] = (float)vec[k];
        return v;
    }
    public static float[][] double2float(double[][] matrix) {
        final float[][] v = new float[matrix.length][];
        for (int k=0; k < matrix.length; ++k) {
            v[k] = new float[matrix[k].length];
            for (int i=0; i < matrix[k].length; ++i) v[k][i] = (float)matrix[k][i];
        }
        return v;
    }

    public static double vectorMean(double[] vector) {
        double s = 0d;
        for (double value : vector) s += value;
        return s / vector.length;
    }
    public static double vectorVariance(double[] vector) {
        double s = 0d;
        for (double value : vector) s += value;
        s /= vector.length;
        double v = 0d;
        for (double value : vector) {
            final double m = value-s;
            v += m*m;
        }
        v /= vector.length;
        return v;
    }
    public static double vectorStd(double[] vector) {
        return Math.sqrt(vectorVariance(vector));
    }

    public static double[][] unflatVector(double[] vector, double[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }

    public static double[][] unflatVector(double[] vector, int rows, int cols) {
        final double[][] M = new double[rows][cols];
        return unflatVector(vector, M);
    }

    public static long[] flatMatrix(long[][] matrix, long[] values) {
        int offset = 0;
        long[][] var3 = matrix;
        int var4 = matrix.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            long[] m = var3[var5];
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }

        return values;
    }
    public static long[] flatMatrix(long[][] matrix) {
        int count = 0;
        for (long[] m : matrix) count += m.length;
        final long[] values = new long[count];
        return flatMatrix(matrix, values);
    }

    public static double[] flatMatrix(double[][] matrix, double[] values) {
        int offset = 0;
        for (double[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static double[] flatMatrixSelectRows(double[][] matrix, int[] rowIds, double[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static double[] flatMatrix(double[][] matrix) {
        int count = 0;
        for (double[] m : matrix) count += m.length;
        final double[] values = new double[count];
        return flatMatrix(matrix, values);
    }

    public static double[][] normalized(double[][] matrix) {
        final double[][] copy = clone(matrix);
        normalize(copy);
        return copy;
    }

    public static double[][] transpose(double[][] matrix) {
        final double[][] transposed = new double[matrix[0].length][matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }
    public static float[][] transpose(float[][] matrix) {
        final float[][] transposed = new float[matrix[0].length][matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }
    public static boolean[][] transpose(boolean[][] matrix) {
        final boolean[][] transposed = new boolean[matrix[0].length][matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }
    public static int[][] transpose(int[][] matrix) {
        final int[][] transposed = new int[matrix[0].length][matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }
    public static long[][] transpose(long[][] matrix) {
        final long[][] transposed = new long[matrix[0].length][matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < matrix[i].length; ++j) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }

    public static void normalize(double[][] matrix) {
        if (matrix.length != matrix[0].length) throw new RuntimeException("matrix is not symetric: " + matrix.length + " rows and " + matrix[0].length + " cols");
        final double[] mainDiagonal = new double[matrix.length];
        for (int i=0; i < matrix.length; ++i) {
            mainDiagonal[i] = matrix[i][i];
        }
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j <= i; ++j) {
                double before = matrix[i][j];
                if (Double.isNaN(before)) System.err.println("NaN in matrix");
                matrix[i][j] = matrix[j][i] = MatrixUtils.norm(matrix[i][j], mainDiagonal[i], mainDiagonal[j]);
                if (Double.isNaN(matrix[i][j])) System.err.println(before + " becomes NaN with main diagonal is " + mainDiagonal[i] + " and " + mainDiagonal[j] );
            }
        }
    }

    public static double[][] clone(double[][] matrix) {
        final double[][] klon = matrix.clone();
        for (int i=0; i < klon.length; ++i) klon[i] = matrix[i].clone();
        return klon;
    }

    public static double[][] rbf(double[][] kernel, double gamma) {
        final double[][] matrix = new double[kernel.length][kernel.length];
        for (int i=0; i < kernel.length; ++i) {
            for (int j=0; j <= i; ++j) {
                matrix[i][j] = matrix[j][i] = Math.exp(-gamma*(kernel[i][i]+kernel[j][j]-2*kernel[i][j]));
            }
        }
        return matrix;
    }

    public static int[] seq(int start, int exclusiveEnd, int stepSize) {
        if (stepSize<=0) throw new IllegalArgumentException();
        final TIntArrayList indizes = new TIntArrayList(start+exclusiveEnd+stepSize);
        for (; start < exclusiveEnd; start += stepSize) indizes.add(start);
        return indizes.toArray();
    }

    public static double[][] selectGrid(double[][] matrix, int[] indizes) {
        return selectSubmatrix(matrix,indizes,indizes);
    }

    public static double[] selectGrid(double[] vector, int[] indizes) {
        final double[] vec = new double[indizes.length];
        int k=0;
        for (int index : indizes) vec[k++] = vector[index];
        return vec;
    }

    public static double[][] selectRows(double[][] matrix, int[] rowIndizes) {
        final double[][] submatrix = new double[rowIndizes.length][];
        int k=0;
        for (int rowIndex : rowIndizes) {
            submatrix[k++] = matrix[rowIndex];
        }
        return submatrix;
    }
    public static double[][] selectColumns(double[][] matrix, int[] colIndizes) {
        return Arrays.stream(matrix).map(x->MatrixUtils.selectGrid(x, colIndizes)).toArray(double[][]::new);
    }

    public static double[][] selectSubmatrix(double[][] matrix, int[] rowIndizes, int[] colIndizes) {
        final double[][] submatrix = new double[rowIndizes.length][colIndizes.length];
        int i=0,j=0;
        for (int rowIndex : rowIndizes) {
            j=0;
            for (int colIndex : colIndizes) {
                submatrix[i][j++] = matrix[rowIndex][colIndex];
            }
            ++i;
        }
        return submatrix;
    }


    public static float[][] selectGrid(float[][] matrix, int[] indizes) {
        return selectSubmatrix(matrix,indizes,indizes);
    }

    public static float[] selectGrid(float[] vector, int[] indizes) {
        final float[] vec = new float[indizes.length];
        int k=0;
        for (int index : indizes) vec[k++] = vector[index];
        return vec;
    }

    public static float[][] selectRows(float[][] matrix, int[] rowIndizes) {
        final float[][] submatrix = new float[rowIndizes.length][];
        int k=0;
        for (int rowIndex : rowIndizes) {
            submatrix[k++] = matrix[rowIndex];
        }
        return submatrix;
    }
    public static float[][] selectColumns(float[][] matrix, int[] colIndizes) {
        return Arrays.stream(matrix).map(x->MatrixUtils.selectGrid(x, colIndizes)).toArray(float[][]::new);
    }

    public static float[][] selectSubmatrix(float[][] matrix, int[] rowIndizes, int[] colIndizes) {
        final float[][] submatrix = new float[rowIndizes.length][colIndizes.length];
        int i=0,j=0;
        for (int rowIndex : rowIndizes) {
            j=0;
            for (int colIndex : colIndizes) {
                submatrix[i][j++] = matrix[rowIndex][colIndex];
            }
            ++i;
        }
        return submatrix;
    }

    public static void normalizeTest(double[] doubles, double norm, double[] normalizations) {
        for (int i=0; i < doubles.length; ++i) {
            doubles[i] = MatrixUtils.norm(doubles[i], norm, normalizations[i]);
        }
    }

    public static void applySum(double[][] kernel, double[][] other) {
        for (int i=0; i < kernel.length; ++i) {
            kernel[i][i] += other[i][i];
            for (int j=0; j < i; ++j) {
                kernel[i][j] += other[i][j];
                kernel[j][i] += other[i][j];
            }
        }
    }
    public static void applySum(double[] vector, double[] otherVector) {
        for (int i=0; i < vector.length; ++i) {
            vector[i] += otherVector[i];
        }
    }
    public static double[] sum(double[] vector, double[] other) {
        final double[] vec = vector.clone();
        applySum(vec,other);
        return vec;

    }
    public static void applyWeightedSum(double[][] kernel, double[][] other, double otherWeight) {
        for (int i=0; i < kernel.length; ++i) {
            kernel[i][i] += other[i][i]*otherWeight;
            for (int j=0; j < i; ++j) {
                kernel[i][j] += other[i][j]*otherWeight;
                kernel[j][i] += other[i][j]*otherWeight;
            }
        }
    }

    public static void applyWeightedSum(double[] kernel, double[] other, double otherWeight) {
        for (int i=0; i < kernel.length; ++i) {
            kernel[i] += other[i]*otherWeight;
        }
    }

    public static double[][] sum(double[][] kernel, double[][] other) {
        final double[][] matrix = new double[kernel.length][kernel.length];
        for (int i=0; i < kernel.length; ++i) {
            for (int j=0; j <= i; ++j) {
                matrix[i][j] = matrix[j][i] = kernel[i][j]+other[i][j];
            }
        }
        return matrix;
    }

    public static void applyScale(double[][] kernel, double scalar) {
        for (int i=0; i < kernel.length; ++i) {
            kernel[i][i] *= scalar;
            for (int j=0; j < i; ++j) {
                kernel[i][j] *= scalar;
                kernel[j][i] *= scalar;
            }
        }
    }
    public static double[][] scale(double[][] orig, double scalar) {
        final double[][] kernel = clone(orig);
        applyScale(kernel, scalar);
        return kernel;
    }


    public static double[] selectDiagonal(double[][] kernel) {
        final double[] vector = new double[kernel.length];
        for (int i=0; i < kernel.length; ++i)
            vector[i] = kernel[i][i];
        return vector;
    }

    public static double[] selectDiagonal(double[][] kernel, int[] indizes) {
        final double[] vector = new double[indizes.length];
        for (int i=0; i < indizes.length; ++i)
            vector[i] = kernel[indizes[i]][indizes[i]];
        return vector;
    }

    public static double[] concat(double[] A, double[] B) {
        final double[] C = Arrays.copyOf(A, A.length+B.length);
        System.arraycopy(B, 0,C,A.length,B.length);
        return C;
    }
    public static float[] concat(float[] A, float[] B) {
        final float[] C = Arrays.copyOf(A, A.length+B.length);
        System.arraycopy(B, 0,C,A.length,B.length);
        return C;
    }

    public static double[][] concatColumns(double[][] A, double[][] B) {
        if (A.length!=B.length) throw new IndexOutOfBoundsException("Incompatible shape: " + A.length + "rows vs " + B.length + " rows.");
        final double[][] C = new double[A.length][A[0].length+B[0].length];
        for (int r=0; r < A.length; ++r) {
            System.arraycopy(A[r], 0, C[r], 0, A[r].length);
            System.arraycopy(B[r], 0, C[r], A[r].length, B[r].length);
        }
        return C;
    }

    public static double norm(double uv, double uu, double vv) {
        if (uu==0 || vv==0) return 0;
        else {
            final double n = Math.sqrt(uu * vv);
            if (n == 0.0) return 0;
            else return uv/n;
        }
    }

    /**
     * This will return a master job that will calculate each position in the matrix with the given function, assuming that
     * the function is symmetric. A single subjob will always compute 2 rows and n entries (with n is the number of rows/columns).
     */
    public static BasicMasterJJob<double[][]> parallelizeSymmetricMatrixComputation(double[][] matrix, MatrixComputationFunction function) {
        return new BasicMasterJJob<double[][]>(JJob.JobType.CPU) {
            @Override
            protected double[][] compute() throws Exception {

                final int middle = matrix.length/2;
                for (int row=0; row < middle; ++row) {
                    final int ROW = row;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int i=0; i <= ROW; ++i) {
                                matrix[ROW][i] = matrix[i][ROW] = function.compute(ROW, i);
                            }
                            int row2 = matrix.length-ROW-1;
                            for (int i=0; i <= row2; ++i) {
                                matrix[row2][i] = matrix[i][row2] = function.compute(row2, i);
                            }
                            return true;
                        }
                    });
                }
                if (matrix.length % 2 != 0) {
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int k=0; k <= middle; ++k) {
                                matrix[middle][k] = matrix[k][middle] = function.compute(middle,k);
                            }
                            return true;
                        }
                    });
                }
                awaitAllSubJobs();
                return matrix;
            }
        };
    }
    public static BasicMasterJJob<float[][]> parallelizeSymmetricMatrixComputation(float[][] matrix, MatrixComputationFunction function) {
        return new BasicMasterJJob<float[][]>(JJob.JobType.CPU) {
            @Override
            protected float[][] compute() throws Exception {

                final int middle = matrix.length/2;
                for (int row=0; row < middle; ++row) {
                    final int ROW = row;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int i=0; i <= ROW; ++i) {
                                matrix[ROW][i] = matrix[i][ROW] = (float)function.compute(ROW, i);
                            }
                            int row2 = matrix.length-ROW-1;
                            for (int i=0; i <= row2; ++i) {
                                matrix[row2][i] = matrix[i][row2] = (float)function.compute(row2, i);
                            }
                            return true;
                        }
                    });
                }
                if (matrix.length % 2 != 0) {
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int k=0; k <= middle; ++k) {
                                matrix[middle][k] = matrix[k][middle] = (float)function.compute(middle,k);
                            }
                            return true;
                        }
                    });
                }
                awaitAllSubJobs();
                return matrix;
            }
        };
    }
    // I >= J
    public static BasicMasterJJob<Object> parallelizeSymmetricMatrixComputation(int size, GenericMatrixComputationFunction function) {
        return new BasicMasterJJob<Object>(JJob.JobType.CPU) {
            @Override
            protected Object compute() throws Exception {

                final int middle = size/2;
                for (int row=0; row < middle; ++row) {
                    final int ROW = row;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int j=0; j <= ROW; ++j) {
                                function.updateValue(ROW, j);
                            }
                            int row2 = size-ROW-1;
                            for (int j=0; j <= row2; ++j) {
                                function.updateValue(row2, j);
                            }
                            return true;
                        }
                    });
                }
                if (size % 2 != 0) {
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int k=0; k <= middle; ++k) {
                                function.updateValue(middle,k);
                            }
                            return true;
                        }
                    });
                }
                awaitAllSubJobs();
                return "";
            }
        };
    }

    public static float[] boolean2float(boolean[] detected) {
        final float[] xs = new float[detected.length];
        for (int k=0; k < detected.length; ++k) xs[k] = (detected[k] ? 1f : 0f);
        return xs;
    }

    public static float[] short2float(short[] distanceFromApex) {
        final float[] xs = new float[distanceFromApex.length];
        for (int k=0; k < distanceFromApex.length; ++k) xs[k] = (distanceFromApex[k]);
        return xs;
    }

    public interface GenericMatrixComputationFunction {
        /*
        updates the value in the matrix for A[I][j] and A[J][I]
         */
        public void updateValue(int i, int j);
    }


    /**
     * Functional interface for calculating the value for a matrix entry given the indizes i and j
     */
    public interface MatrixComputationFunction {
        public double compute(int i, int j);
    }


    ///////////////////// other primitives ///////////////////////

    public static int[][] unflatVector(int[] vector, int[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }

    public static int[][] unflatVector(int[] vector, int rows, int cols) {
        final int[][] M = new int[rows][cols];
        return unflatVector(vector, M);
    }

    public static int[] flatMatrix(int[][] matrix, int[] values) {
        int offset = 0;
        for (int[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static int[] flatMatrixSelectRows(int[][] matrix, int[] rowIds, int[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static int[] flatMatrix(int[][] matrix) {
        int count = 0;
        for (int[] m : matrix) count += m.length;
        final int[] values = new int[count];
        return flatMatrix(matrix, values);
    }

    public static byte[][] unflatVector(byte[] vector, byte[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }

    public static byte[][] unflatVector(byte[] vector, int rows, int cols) {
        final byte[][] M = new byte[rows][cols];
        return unflatVector(vector, M);
    }

    public static byte[] flatMatrix(byte[][] matrix, byte[] values) {
        int offset = 0;
        for (byte[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static byte[] flatMatrixSelectRows(byte[][] matrix, int[] rowIds, byte[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static byte[] flatMatrix(byte[][] matrix) {
        int count = 0;
        for (byte[] m : matrix) count += m.length;
        final byte[] values = new byte[count];
        return flatMatrix(matrix, values);
    }

    public static short[][] unflatVector(short[] vector, short[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }

    public static short[][] unflatVector(short[] vector, int rows, int cols) {
        final short[][] M = new short[rows][cols];
        return unflatVector(vector, M);
    }

    public static short[] flatMatrix(short[][] matrix, short[] values) {
        int offset = 0;
        for (short[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static short[] flatMatrixSelectRows(short[][] matrix, int[] rowIds, short[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static short[] flatMatrix(short[][] matrix) {
        int count = 0;
        for (short[] m : matrix) count += m.length;
        final short[] values = new short[count];
        return flatMatrix(matrix, values);
    }

    public static float[][] unflatVector(float[] vector, float[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }

    public static float[][] unflatVector(float[] vector, int rows, int cols) {
        final float[][] M = new float[rows][cols];
        return unflatVector(vector, M);
    }

    public static float[] flatMatrix(float[][] matrix, float[] values) {
        int offset = 0;
        for (float[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static float[] flatMatrixSelectRows(float[][] matrix, int[] rowIds, float[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static float[] flatMatrix(float[][] matrix) {
        int count = 0;
        for (float[] m : matrix) count += m.length;
        final float[] values = new float[count];
        return flatMatrix(matrix, values);
    }

    public static int[] flatTensor(int[][][] tensor) {
        int count = 0;
        for (int[][] m : tensor) {
            for (int [] n : m) {
                count += n.length;
            }
        }
        final int[] values = new int[count];
        return flatTensor(tensor, values);
    }
    public static int[] flatTensor(int[][][] tensor, int[] target) {
        int offset=0;
        for (int[][] submatrix : tensor) {
            for (int[] subvec : submatrix) {
                System.arraycopy(subvec, 0, target, offset, subvec.length);
                offset += subvec.length;
            }
        }
        return target;
    }

    public static boolean[][] unflatVector(boolean[] vector, int rows, int cols) {
        final boolean[][] M = new boolean[rows][cols];
        return unflatVector(vector, M);
    }


    public static boolean[][] unflatVector(boolean[] vector, boolean[][] M) {
        final int rows = M.length, cols = M.length>0 ? M[0].length : 0;
        if (vector.length != rows*cols)
            throw new IllegalArgumentException("matrix size differs from number of elements in vector: " + rows + " x " + cols + " != " + vector.length);
        int offset=0;
        for (int r=0; r < rows; ++r) {
            System.arraycopy(vector, offset, M[r], 0, cols);
            offset += cols;
        }
        return M;
    }


    public static boolean[] flatMatrix(boolean[][] matrix, boolean[] values) {
        int offset = 0;
        for (boolean[] m : matrix) {
            System.arraycopy(m, 0, values, offset, m.length);
            offset += m.length;
        }
        return values;
    }

    public static boolean[] flatMatrixSelectRows(boolean[][] matrix, int[] rowIds, boolean[] values) {
        int offset = 0;
        for (int rowId : rowIds) {
            System.arraycopy(matrix[rowId], 0, values, offset, matrix[rowId].length);
            offset += matrix[rowId].length;
        }
        if (offset != values.length) throw new RuntimeException("Vector too large: " + offset + " expected but vector has length " + values.length );
        return values;
    }

    public static boolean[] flatMatrix(boolean[][] matrix) {
        int count = 0;
        for (boolean[] m : matrix) count += m.length;
        final boolean[] values = new boolean[count];
        return flatMatrix(matrix, values);
    }



    public static interface IntComparator {
        public int compare(int a, int b);
    }

    public static <T> int[] argsort(int size, IntComparator comparator) {
        int[] indizes = new int[size];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->comparator.compare(i,j));
        return indizes;
    }

    public static <T> int[] argsort(T[] values, Comparator<T> comp) {
        int[] indizes = new int[values.length];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->comp.compare(values[i],values[j]));
        return indizes;
    }

    public static int[] argsort(int[] values) {
        int[] indizes = values.clone();
        fillIndizes(indizes);
        argsort(indizes, (i,j)->Integer.compare(values[i],values[j]));
        return indizes;
    }
    public static int[] argsort(float[] values) {
        int[] indizes = new int[values.length];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->Float.compare(values[i],values[j]));
        return indizes;
    }
    public static int[] argsort(double[] values) {
        int[] indizes = new int[values.length];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->Double.compare(values[i],values[j]));
        return indizes;
    }
    public static int[] argsort(long[] values) {
        int[] indizes = new int[values.length];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->Long.compare(values[i],values[j]));
        return indizes;
    }
    public static int[] argsort(short[] values) {
        int[] indizes = new int[values.length];
        fillIndizes(indizes);
        argsort(indizes, (i,j)->Short.compare(values[i],values[j]));
        return indizes;
    }

    private static void fillIndizes(int[] indizes) {
        for (int i=0; i < indizes.length; ++i) indizes[i] = i;
    }

    public static void argsort(int[] indizes, IntComparator compare) {
        final int n = indizes.length;
        // Insertion sort on smallest arrays
        if (n <= 20) {
            for (int i = 0; i < n; i++) {
                for (int j = i; j > 0 && compare.compare(indizes[j], indizes[j - 1]) < 0; j--) {
                    __swap(indizes, j, j - 1);
                }
            }
            return;
        }
        // quicksort on larger arrays
        {
            int i = 1;
            for (; i < n; ++i) {
                if (compare.compare(indizes[i], indizes[i - 1]) < 0) break;
            }
            if (i < n) __quickSort__(indizes, compare, 0, n - 1, 0);
        }

    }

    private static final short[] ALMOST_RANDOM = new short[]{9205, 23823, 4568, 17548, 15556, 31788, 3, 580, 17648, 22647, 17439, 24971, 10767, 9388, 6174, 21774, 4527, 19015, 22379, 12727, 23433, 11160, 15808, 27189, 17833, 7758, 32619, 12980, 31234, 31103, 5140, 571, 4439};

    /**
     * http://en.wikipedia.org/wiki/Quicksort#In-place_version
     *
     * @param low
     * @param high
     */
    private static void __quickSort__(int[] s, IntComparator comp, int low, int high, int depth) {
        int n = high - low + 1;
        if (n >= 20 && depth <= 32) {
            if (low < high) {
                int pivot = ALMOST_RANDOM[depth] % n + low;
                pivot = __partition__(s, comp, low, high, pivot);
                __quickSort__(s, comp, low, pivot - 1, depth + 1);
                __quickSort__(s, comp, pivot + 1, high, depth + 1);
            }
        } else if (n < 40) {
            for (int i = low; i <= high; i++) {
                for (int j = i; j > low && comp.compare(s[j], s[j - 1]) < 0; j--) {
                    __swap(s, j, j - 1);
                }
            }
            return;
        } else heap_sort(s, comp, low, n);
    }

    private static void heap_sort(int[] s, IntComparator comp, int offset, int length) {
        heap_build(s, comp, offset, length);
        int n = length;
        while (n > 1) {
            __swap(s, offset, offset + n - 1);
            heap_heapify(s, comp, offset, --n, 0);
        }

    }

    private static void heap_heapify(int[] s, IntComparator comp, int offset, int length, int i) {
        do {
            int max = i;
            final int right_i = 2 * i + 2;
            final int left_i = right_i - 1;
            if (left_i < length && comp.compare(s[offset + left_i], s[offset + max]) > 0)
                max = left_i;
            if (right_i < length && comp.compare(s[offset + right_i], s[offset + max]) > 0)
                max = right_i;
            if (max == i)
                break;
            __swap(s, offset + i, offset + max);
            i = max;
        } while (true);
    }

    private static void heap_build(int[] s, IntComparator comp, int offset, int length) {
        if (length == 0) return;
        for (int i = (length >> 1) - 1; i >= 0; --i)
            heap_heapify(s, comp, offset, length, i);
    }

    /**
     * http://en.wikipedia.org/wiki/Quicksort#In-place_version
     *
     * @param low
     * @param high
     * @param pivot
     * @return
     */
    private static <T extends Peak, S extends MutableSpectrum<T>>
    int __partition__(int[] s, IntComparator comp, int low, int high, int pivot) {
        __swap(s, high, pivot);
        int store = low;
        for (int i = low; i < high; i++) {
            if (comp.compare(s[i], s[high]) < 0) {
                if (i != store) __swap(s, i, store);
                store++;
            }
        }
        __swap(s, store, high);
        return store;
    }

    private static void __swap(int[] list, int a, int b) {
        final int z = list[a];
        list[a] = list[b];
        list[b] = z;
    }



}
