package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import gnu.trove.list.array.TIntArrayList;

public class MatrixUtils {

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
        return new BasicMasterJJob<double[][]>(JJob.JobType.SCHEDULER) {
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
}
