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

package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import gurobi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public final class ALIGNF {

    private double[][][] kernelMatrices;
    private final double[][] targetMatrix;
    private final Fingerprint[] fingerprints;
    private final boolean matricesAreAlreadyCentered;

    private double[] upperbounds;
    private double[] lowerbounds;

    // EUCLIDEAN=0, JACCARD=1, Gaussian-Jaccard
    private int matrixType = 0;

    private double[] weights;

    private double[][] M;
    private double[] a;

    public ALIGNF(double[][][] kernelMatrices, Fingerprint[] fingerprints) {
        this(kernelMatrices, fingerprints, false);
    }
    public ALIGNF(double[][][] kernelMatrices, Fingerprint[] fingerprints, boolean matricesAreAlreadyCentered) {
        this.kernelMatrices = kernelMatrices;
        this.targetMatrix = new double[kernelMatrices[0].length][kernelMatrices[0].length];
        this.fingerprints = fingerprints;
        this.weights = null;
        this.matricesAreAlreadyCentered=matricesAreAlreadyCentered;
        this.upperbounds = new double[kernelMatrices.length];
        this.lowerbounds = new double[kernelMatrices.length];
        Arrays.fill(upperbounds, Double.POSITIVE_INFINITY);
    }
    public ALIGNF(double[][][] kernelMatrices, double[][] targetMatrix, boolean matricesAreAlreadyCentered) {
        this.kernelMatrices = kernelMatrices;
        this.targetMatrix = targetMatrix;
        this.fingerprints=null;
        this.weights = null;
        this.matricesAreAlreadyCentered=matricesAreAlreadyCentered;
        this.upperbounds = new double[kernelMatrices.length];
        this.lowerbounds = new double[kernelMatrices.length];
        Arrays.fill(upperbounds, Double.POSITIVE_INFINITY);
    }

    public double[] getUpperbounds() {
        return upperbounds;
    }

    public double[] getLowerbounds() {
        return lowerbounds;
    }

    public void setUpperbound(int k, double up) {
        upperbounds[k] = up;
    }

    public void setUpperbound(double up) {
        Arrays.fill(upperbounds, up);
    }

    public void setLowerbound(int k, double up) {
        lowerbounds[k] = up;
    }

    public void setLowerbound(double up) {
        Arrays.fill(lowerbounds, up);
    }



    public int getMatrixType() {
        return matrixType;
    }

    public void setMatrixType(int matrixType) {
        this.matrixType = matrixType;
    }

    public void run() {
        run(false);
    }

    public void run(boolean memorize) {

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // center kernels
        final Future[] centerKernels;
        if (matricesAreAlreadyCentered) {
            centerKernels=new Future[0];
        } else {
            throw new IllegalArgumentException("Centering not supported.");
        }
        if (fingerprints!=null) {
            generateTargetMatrix(executor, fingerprints);
        } else if (targetMatrix==null)
            throw new IllegalArgumentException();
        for (Future f : centerKernels)
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        for (int k=0; k < kernelMatrices.length; ++k) {
            if (isNaN(kernelMatrices[k])) throw new RuntimeException(k + "th matrix contains a NaN");
        }

        // we dont have to center the targetMatrix, but we should normalize it
        //new KernelCentering(targetMatrix, true).applyToTrainMatrix(targetMatrix);

        // just normalize the target matrix
        //MatrixUtils.normalize(targetMatrix);

        // calculate M matrix
        final ArrayList<Future> mfutures = new ArrayList<>();
        final int N = kernelMatrices[0].length;
        final int K = kernelMatrices.length;
        final double[][] M = new double[K][K];
        for (int i=0; i < kernelMatrices.length/2; ++i) {
            final int I = i;
            mfutures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j=I; j < K; ++j) {
                        M[I][j] = frobeniusProduct(kernelMatrices[I], kernelMatrices[j]);
                    }
                    final int i2 = K-I-1;
                    for (int j=i2; j < K; ++j) {
                        M[i2][j] = frobeniusProduct(kernelMatrices[i2], kernelMatrices[j]);
                    }
                }
            }));
        }
        // middle row is missing
        if (kernelMatrices.length%2 != 0) {
            final int I = kernelMatrices.length/2;
            mfutures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j=I; j < K; ++j) {
                        M[I][j] = frobeniusProduct(kernelMatrices[I], kernelMatrices[j]);
                    }
                }
            }));
        }
        for (Future f : mfutures) try {
            f.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        for (int i=0; i < K; ++i) {
            for (int j=i+1; j < K; ++j) {
                M[j][i] = M[i][j];
            }
        }

        final double[] A = new double[K];
        final Future<Double>[] afutures = new Future[K];
        for (int i=0; i < K; ++i) {
            final double[][] kernel = kernelMatrices[i];
            afutures[i] = executor.submit(new Callable<Double>() {
                @Override
                public Double call() throws Exception {
                    return frobeniusProduct(kernel, targetMatrix);
                }
            });
        }
        for (int i=0; i < K; ++i) {
            try {
                A[i] = afutures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        try {
            if (memorize) {
                this.M = M;
                this.a = A;
            }
            this.weights = formulateQuadraticProgramming(M, A);

            //////////////////
            // normalize weights such that they sum up to 1
            /////////////////

            double sum = 0d;
            for (double w : weights) sum += w;
            for (int k=0; k < weights.length; ++k) weights[k] /= sum;

        } catch (GRBException e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }

    public double[] rerun() {
        try {
            this.weights = formulateQuadraticProgramming(M, a);
            double sum = 0d;
            for (double w : weights) sum += w;
            for (int k=0; k < weights.length; ++k) weights[k] /= sum;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
        return weights;
    }

    public double[][] getALIGNFMatrix() {
        if (weights==null) throw new IllegalStateException("First call #run to start the ALIGNF computation!");
        final int N = kernelMatrices[0].length;
        final double[][] alignf = new double[N][N];
        for (int k=0; k < kernelMatrices.length; ++k) {
            final double weight = weights[k];
            final double[][] kernel = kernelMatrices[k];
            for (int i=0; i < N; ++i) {
                for (int j=0; j < N; ++j) {
                    alignf[i][j] += kernel[i][j] * weight;
                }
            }
        }
        return alignf;
    }

    public double[] getWeights() {
        if (weights==null) throw new IllegalStateException("First call #run to start the ALIGNF computation!");
        return weights.clone();
    }

    public double[] formulateQuadraticProgramming(double[][] M, double[] a) throws GRBException {
        final GRBModel model = new GRBModel(new GRBEnv());
        assert isSymetric(M);
        final int K = M.length;
        GRBVar[] variables = new GRBVar[K];
        for (int i=0; i < K; ++i)
            variables[i] = model.addVar(lowerbounds[i], upperbounds[i], 0d, GRB.CONTINUOUS, null);

        model.update();
        final GRBQuadExpr expr = new GRBQuadExpr();
        // quadratic term
        for (int i=0; i < K; ++i) {
            for (int j=0; j < K; ++j) {
                expr.addTerm(M[i][j], variables[i], variables[j]);
            }
        }
        // linear term
        for (int i=0; i < K; ++i) {
            expr.addTerm(-2d * a[i], variables[i]);
        }
        model.setObjective(expr, GRB.MINIMIZE);
        model.update();
        model.optimize();
        final double[] solution = new double[K];
        for (int i=0; i < K; ++i) {
            solution[i] = variables[i].get(GRB.DoubleAttr.X);
            System.out.println(i + ": " + solution[i]);
        }
        double norm = 0d;
        for (double w : solution) norm += w*w;
        norm = Math.sqrt(norm);
        for (int i=0; i < K; ++i) {
            solution[i] /= norm;
        }
        model.dispose();
        return solution;
    }

    private boolean isNaN(double[][] m) {
        for (int i=0; i < m.length; ++i) {
            for (int j=0; j < m.length; ++j) {
                if (Double.isNaN(m[i][j])) return true;
            }
        }
        return false;
    }

    private boolean isSymetric(double[][] m) {
        for (int i=0; i < m.length; ++i) {
            for (int j=i+1; j < m.length; ++j) {
                final double delta = m[i][j]-m[j][i];
                if (Math.abs(delta) > 1e-12) return false;
            }
        }
        return true;
    }


    private double frobeniusProduct(double[][] A, double[][] B) {
        final int N = A.length;
        double sum=0d;
        for (int i=0; i < N; ++i) {
            for (int j=i+1; j < N; ++j) {
                sum += A[i][j]*B[i][j];
            }
        }
        sum *= 2;
        for (int i=0; i < N; ++i) sum += A[i][i]*B[i][i];
        return sum;
    }
    private double frobeniusProduct(double[][] A, long[][] B) {
        final int N = A.length;
        double sum=0d;
        for (int i=0; i < N; ++i) {
            for (int j=i+1; j < N; ++j) {
                sum += A[i][j]*B[i][j];
            }
        }
        sum *= 2;
        for (int i=0; i < N; ++i) sum += A[i][i]*B[i][i];
        return sum;
    }


    private void generateTargetMatrix(ExecutorService service, final Fingerprint[] F) {
        final List<Future> futures = new ArrayList<>();

            for (int i=0; i < F.length; ++i) {
                final int I = i;
                targetMatrix[i][i] = F[i].getFingerprintVersion().size();
                futures.add(service.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int j=0; j < I; ++j) {
                            final double dot = matrixType>=1 ? F[I].tanimoto(F[j]) : F[I].plusMinusdotProduct(F[j]);
                            targetMatrix[I][j] = targetMatrix[j][I] = dot;
                        }
                    }
                }));
            }
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        if (matrixType==2) {
            for (int i=0; i < targetMatrix.length; ++i) {
                targetMatrix[i][i] = 1d;
                for (int j=0; j < i; ++j) {
                    targetMatrix[i][j] = targetMatrix[j][i] = Math.exp(-(1d-targetMatrix[i][j])*4);
                }
            }
        }
    }












    /*
    LEGACY
     */

    private void makeFingerprintMatrix(ExecutorService service, final int[][] fingerprintIndizes, final int totalLength) {
        final double[] weights;
        weights=null;
        final ArrayList<Future> futures = new ArrayList<>();

        for (int i = 0; i < targetMatrix.length / 2; ++i) {
            final int I = i;
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    calcFpTargetRow(I, fingerprintIndizes, totalLength, weights);
                    calcFpTargetRow(fingerprintIndizes.length - I - 1, fingerprintIndizes, totalLength, weights);
                }
            }));
        }
        if (targetMatrix.length % 2 != 0) {
            final int MIDDLE = targetMatrix.length / 2;

            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    calcFpTargetRow(MIDDLE, fingerprintIndizes, totalLength, weights);
                }
            }));
        }

        for (Future f : futures)
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }


        // make matrix symetric
        for (int i = 0; i < targetMatrix.length; ++i) {
            targetMatrix[i][i] = (matrixType==0) ? totalLength : 1d;
            for (int j = i + 1; j < targetMatrix.length; ++j) {
                targetMatrix[j][i] = targetMatrix[i][j];
            }
        }
        /*
        try {
            new KernelToNumpyConverter().writeToFile(new File("tanimoto" + matrixType +".txt"), targetMatrix);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

    }
    private void calcFpTargetRow(final int i, final int[][] fingerprintIndizes, final int totalLength, double[] weights) {
        if (matrixType>0) {
            calcFpTargetRowJaccard(i,fingerprintIndizes,totalLength);
            return;
        }
        final int[] as = fingerprintIndizes[i];
        for (int j=i+1; j < targetMatrix.length; ++j) {
            final int[] bs = fingerprintIndizes[j];
            // count how many bits are equal
            int a=0, b=0, count=0;
            while(a < as.length && b < bs.length) {
                if (as[a]==bs[b]) {
                    ++count;
                    ++a; ++b;
                } else if (as[a] > bs[b]) {
                    ++b;
                } else {
                    ++a;
                }
            }
            // number of 1-bits which are equal = count
            // number of 0-bits which are equal is: |AuB| - (|A|+|B|) + count
            targetMatrix[i][j] += 2*count + totalLength - (as.length + bs.length);
            // each diff reduce count by one
            targetMatrix[i][j] -= totalLength - targetMatrix[i][j];
        }
    }

    private void calcFpTargetRowJaccard(final int i, final int[][] fingerprintIndizes, final int totalLength) {
        final int[] as = fingerprintIndizes[i];
        for (int j=i+1; j < targetMatrix.length; ++j) {
            final int[] bs = fingerprintIndizes[j];
            // count how many bits are equal
            int a = 0, b = 0, count = 0;
            while (a < as.length && b < bs.length) {
                if (as[a] == bs[b]) {
                    ++count;
                    ++a;
                    ++b;
                } else if (as[a] > bs[b]) {
                    ++b;
                } else {
                    ++a;
                }
            }
            // count = A&B

            targetMatrix[i][j] += ((double)count) / (as.length+bs.length-count);
            if (matrixType>=2) {
                // make matrix square
                targetMatrix[i][j] = Math.pow(targetMatrix[i][j], matrixType);
            }
        }
    }




    private double[][] dividebytreesizes(double[][] kernelMatrice, int[] treeSizes) {
        kernelMatrice = MatrixUtils.clone(kernelMatrice);
        for (int i=0; i < kernelMatrice.length; ++i) {
            for (int j=0; j <= i; ++j) {
                kernelMatrice[i][j] = kernelMatrice[j][i] = kernelMatrice[i][j]/(treeSizes[i]*treeSizes[j]);
            }
        }
        return kernelMatrice;
    }

}
