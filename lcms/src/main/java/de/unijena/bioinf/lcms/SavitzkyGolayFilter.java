package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;

import java.util.Arrays;

public class SavitzkyGolayFilter {

    // simple average smoothing
    public static final SavitzkyGolayFilter Window1Polynomial1 = new SavitzkyGolayFilter(
            new double[][]{
                    {1, -1}, {1, 0},{1,1}
            },
            new double[][]{{ 0.33333333,  0.33333333,  0.33333333},
            {-0.5       ,  0.        ,  0.5       }}
    );


    public static final SavitzkyGolayFilter Window2Polynomial2 = new SavitzkyGolayFilter(
            new double[][]{
                    { 1., -2.,  4.},
                    { 1., -1.,  1.},
                    { 1.,  0.,  0.},
                    { 1.,  1.,  1.},
                    { 1.,  2.,  4.}
            },
            new double[][]{
            {-0.08571429,  0.34285714,  0.48571429,  0.34285714, -0.08571429},
            {-0.2       , -0.1       ,  0.        ,  0.1       ,  0.2       },
            { 0.14285714, -0.07142857, -0.14285714, -0.07142857,  0.14285714}
        }
    );
    public static final SavitzkyGolayFilter Window3Polynomial2 = new SavitzkyGolayFilter(
            new double[][]{{ 1., -3.,  9.},
                    { 1., -2.,  4.},
                    { 1., -1.,  1.},
                    { 1.,  0.,  0.},
                    { 1.,  1.,  1.},
                    { 1.,  2.,  4.},
                    { 1.,  3.,  9.}},

    new double[][]{{-0.0952381 ,  0.14285714,  0.28571429,  0.33333333,  0.28571429,
            0.14285714, -0.0952381 },
        {-0.10714286, -0.07142857, -0.03571429,  0.        ,  0.03571429,
                0.07142857,  0.10714286},
        { 0.05952381,  0.        , -0.03571429, -0.04761905, -0.03571429,
                0.        ,  0.05952381}}
    );

    public static final SavitzkyGolayFilter Window3Polynomial3 = new SavitzkyGolayFilter(
            new double[][]{{  1.,  -3.,   9., -27.},
                    {  1.,  -2.,   4.,  -8.},
                    {  1.,  -1.,   1.,  -1.},
                    {  1.,   0.,   0.,   0.},
                    {  1.,   1.,   1.,   1.},
                    {  1.,   2.,   4.,   8.},
                    {  1.,   3.,   9.,  27.}},
            new double[][]{{-0.0952381 ,  0.14285714,  0.28571429,  0.33333333,  0.28571429,
                    0.14285714, -0.0952381 },
                    { 0.08730159, -0.26587302, -0.23015873,  0.        ,  0.23015873,
                            0.26587302, -0.08730159},
                    { 0.05952381,  0.        , -0.03571429, -0.04761905, -0.03571429,
                            0.        ,  0.05952381},
                    {-0.02777778,  0.02777778,  0.02777778,  0.        , -0.02777778,
                            -0.02777778,  0.02777778}}
    );

    public static final SavitzkyGolayFilter Window4Polynomial2 = new SavitzkyGolayFilter(new double[][]{{ 1., -4., 16.},
            { 1., -3.,  9.},
            { 1., -2.,  4.},
            { 1., -1.,  1.},
            { 1.,  0.,  0.},
            { 1.,  1.,  1.},
            { 1.,  2.,  4.},
            { 1.,  3.,  9.},
            { 1.,  4., 16.}},
            new double[][]{{-0.09090909,  0.06060606,  0.16883117,  0.23376623,  0.25541126,
                    0.23376623,  0.16883117,  0.06060606, -0.09090909},
                    {-0.06666667, -0.05      , -0.03333333, -0.01666667,  0.        ,
                            0.01666667,  0.03333333,  0.05      ,  0.06666667},
                    { 0.03030303,  0.00757576, -0.00865801, -0.01839827, -0.02164502,
                            -0.01839827, -0.00865801,  0.00757576,  0.03030303}}

    );

    public static final SavitzkyGolayFilter Window4Polynomial3 = new SavitzkyGolayFilter(new double[][]{{  1.,  -4.,  16., -64.},
            {  1.,  -3.,   9., -27.},
            {  1.,  -2.,   4.,  -8.},
            {  1.,  -1.,   1.,  -1.},
            {  1.,   0.,   0.,   0.},
            {  1.,   1.,   1.,   1.},
            {  1.,   2.,   4.,   8.},
            {  1.,   3.,   9.,  27.},
            {  1.,   4.,  16.,  64.}},
            new double[][]{{-0.09090909,  0.06060606,  0.16883117,  0.23376623,  0.25541126,
                    0.23376623,  0.16883117,  0.06060606, -0.09090909},
                    { 0.07239057, -0.11952862, -0.16245791, -0.10606061,  0.        ,
                            0.10606061,  0.16245791,  0.11952862, -0.07239057},
                    { 0.03030303,  0.00757576, -0.00865801, -0.01839827, -0.02164502,
                            -0.01839827, -0.00865801,  0.00757576,  0.03030303},
                    {-0.01178451,  0.00589226,  0.01094276,  0.00757576,  0.        ,
                            -0.00757576, -0.01094276, -0.00589226,  0.01178451}}
    );

    public static final SavitzkyGolayFilter Window8Polynomial2  = new SavitzkyGolayFilter(
        new double[][]{{ 1., -8., 64.},
                { 1., -7., 49.},
                { 1., -6., 36.},
                { 1., -5., 25.},
                { 1., -4., 16.},
                { 1., -3.,  9.},
                { 1., -2.,  4.},
                { 1., -1.,  1.},
                { 1.,  0.,  0.},
                { 1.,  1.,  1.},
                { 1.,  2.,  4.},
                { 1.,  3.,  9.},
                { 1.,  4., 16.},
                { 1.,  5., 25.},
                { 1.,  6., 36.},
                { 1.,  7., 49.},
                { 1.,  8., 64.}},
            new double[][]{{-6.50154799e-02, -1.85758514e-02,  2.16718266e-02,
                    5.57275542e-02,  8.35913313e-02,  1.05263158e-01,
                    1.20743034e-01,  1.30030960e-01,  1.33126935e-01,
                    1.30030960e-01,  1.20743034e-01,  1.05263158e-01,
                    8.35913313e-02,  5.57275542e-02,  2.16718266e-02,
                    -1.85758514e-02, -6.50154799e-02},
                    {-1.96078431e-02, -1.71568627e-02, -1.47058824e-02,
                            -1.22549020e-02, -9.80392157e-03, -7.35294118e-03,
                            -4.90196078e-03, -2.45098039e-03,  0.00000000e+00,
                            2.45098039e-03,  4.90196078e-03,  7.35294118e-03,
                            9.80392157e-03,  1.22549020e-02,  1.47058824e-02,
                            1.71568627e-02,  1.96078431e-02},
                    { 5.15995872e-03,  3.22497420e-03,  1.54798762e-03,
                            1.28998968e-04, -1.03199174e-03, -1.93498452e-03,
                            -2.57997936e-03, -2.96697626e-03, -3.09597523e-03,
                            -2.96697626e-03, -2.57997936e-03, -1.93498452e-03,
                            -1.03199174e-03,  1.28998968e-04,  1.54798762e-03,
                            3.22497420e-03,  5.15995872e-03}}
    );


    public static void main(String[] args) {
        final double[] signal = new double[]{ 0.32556523,  0.51598466,  0.72521671,  0.96011726,  0.88875221,
                0.44022408,  0.18625947, -0.21055245, -0.75693153, -0.86655758,
                -1.12279338, -0.68924353, -0.39634847,  0.13839553,  0.63508686,
                0.80043708,  0.83263882,  0.82852367,  0.2824472 , -0.1070761};
        final double[] filtered = Window2Polynomial2.apply(signal);
        System.out.println(Arrays.toString(filtered));
        final double[] filtered2 = Window3Polynomial2.apply(signal);
        System.out.println(Arrays.toString(filtered2));


    }


    protected final double[][] vandermondeMatrix, inverseMatrix;
    protected final int polynomialDegree, windowSize;

    public SavitzkyGolayFilter(double[][] vandermondeMatrix, double[][] inverseMatrix) {
        this.vandermondeMatrix = vandermondeMatrix;
        this.inverseMatrix = inverseMatrix;
        this.windowSize = vandermondeMatrix.length;
        this.polynomialDegree = vandermondeMatrix[0].length-1;
    }

    public double[] apply(double[] values) {
        final double[] result = new double[values.length];
        final int n = (windowSize-1)/2;
        final double[] bufferA = new double[polynomialDegree+1], bufferB = new double[windowSize];
        // apply to the first n datapoints
        matvecmul(inverseMatrix, values, 0, bufferA);
        MatrixUtils.matmul(vandermondeMatrix, bufferA, bufferB);
        System.arraycopy(bufferB,0,result,0,n+1);
        // apply to everything in between
        for (int j=0; j < values.length-windowSize; ++j) {
            result[j + n] = dot(inverseMatrix[0], values, j);
        }
        // apply to the last n datapoints
        int k = values.length-windowSize;
        matvecmul(inverseMatrix, values, k, bufferA);
        MatrixUtils.matmul(vandermondeMatrix, bufferA, bufferB);
        System.arraycopy(bufferB,n,result,k+n,n+1);
        return result;
    }


    // we cannot calculate derivatives for points outside the domain
    public double computeFirstOrderDerivative(double[] values, int offset) {
        offset -= (windowSize-1)/2;
        if (offset < windowSize || offset > values.length - windowSize) return 0d;
        // otherwise the derivative is equal to the second coefficient
        return dot(inverseMatrix[1], values, offset);
    }

    // we cannot calculate derivatives for points outside the domain
    public double computeSecondOrderDerivative(double[] values, int offset) {
        offset -= (windowSize-1)/2;
        if (offset < 0 || offset > values.length - windowSize) return 0d;
        // otherwise the derivative is equal to the third coefficient
        return 2*dot(inverseMatrix[2], values, offset);
    }

    // we cannot calculate derivatives for points outside the domain
    public double computeFirstOrderDerivative(float[] values, int offset) {
        offset -= (windowSize-1)/2;
        if (offset < 0 || offset > values.length - windowSize) return 0d;
        // otherwise the derivative is equal to the second coefficient
        return dot(inverseMatrix[1], values, offset);
    }

    // we cannot calculate derivatives for points outside the domain
    public double computeSecondOrderDerivative(float[] values, int offset) {
        offset -= (windowSize-1)/2;
        if (offset < 0 || offset > values.length - windowSize) return 0d;
        // otherwise the derivative is equal to the third coefficient
        return 2*dot(inverseMatrix[2], values, offset);
    }



    private static void matvecmul(double[][] M, double[] V, int offset, double[] buffer) {
        for (int row=0; row < M.length; ++row) {
            double buf = 0d;
            final double[] vec = M[row];
            for (int j=0; j < vec.length; ++j) {
                buf += vec[j]*V[j+offset];
            }
            buffer[row] = buf;
        }
    }
    private static double dot(double[] U, double[] V, int offset) {
        double x = 0d;
        for (int i=0; i < U.length; ++i) {
            x += U[i] * V[i+offset];
        }
        return x;
    }


    public float[] apply(float[] values) {
        final float[] result = new float[values.length];
        final int n = (windowSize-1)/2;
        final double[] bufferA = new double[polynomialDegree+1], bufferB = new double[windowSize];
        // apply to the first n datapoints
        matvecmul(inverseMatrix, values, 0, bufferA);
        MatrixUtils.matmul(vandermondeMatrix, bufferA, bufferB);
        for (int k=0; k < n+1; ++k) result[k] = (float)bufferB[k];
        // apply to everything in between
        for (int j=0; j < values.length-windowSize; ++j) {
            result[j + n] = dot(inverseMatrix[0], values, j);
        }
        // apply to the last n datapoints
        int k = values.length-windowSize;
        matvecmul(inverseMatrix, values, k, bufferA);
        MatrixUtils.matmul(vandermondeMatrix, bufferA, bufferB);
        for (int j=0; j < n+1; ++j) result[k+n+j] = (float)bufferB[n+j];
        return result;
    }
    private static void matvecmul(double[][] M, float[] V, int offset, double[] buffer) {
        for (int row=0; row < M.length; ++row) {
            double buf = 0d;
            final double[] vec = M[row];
            for (int j=0; j < vec.length; ++j) {
                buf += vec[j]*V[j+offset];
            }
            buffer[row] = buf;
        }
    }
    private static float dot(double[] U, float[] V, int offset) {
        double x = 0d;
        for (int i=0; i < U.length; ++i) {
            x += U[i] * V[i+offset];
        }
        return (float)x;
    }


    public int getWindowSize() {
        return windowSize;
    }

    public int getNumberOfDataPointsPerSide() {
        return (windowSize-1)/2;
    }

    public int getDegree() {
        return polynomialDegree;
    }


}
