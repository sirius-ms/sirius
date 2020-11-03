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

package de.unijena.bioinf.canopus.dnn;

import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FullyConnectedLayer {
    protected FMatrixRMaj W;
    protected float[] B;
    protected ActivationFunction activationFunction;

    public FullyConnectedLayer(float[][] w, float[] b, ActivationFunction activationFunction) {
        W = new FMatrixRMaj(w);
        B = b;
        this.activationFunction = activationFunction;
    }

    public FullyConnectedLayer(int nrows, int ncols, float[] w, float[] b, ActivationFunction activationFunction) {
        W = new FMatrixRMaj(nrows, ncols, true, w);
        B = b;
        this.activationFunction = activationFunction;
    }

    public float[] getWeightMatrixCopy() {
        return W.data;
    }

    public float[] getBiasVectorCopy() {
        return B;
    }

    public FMatrixRMaj weightMatrix() {
        return W;
    }

    public int getInputSize() {
        return W.numRows;
    }

    public int getOutputSize() {
        return B.length;
    }

    public void setActivationFunction(ActivationFunction f) {
        this.activationFunction = f;
    }

    public FMatrixRMaj eval(FMatrixRMaj input) {
        final float[] storage = new float[B.length*input.numRows];
        final FMatrixRMaj output;
        {
            int j=0;
            for (int i=0; i < input.numRows; ++i) {
                System.arraycopy(B, 0, storage, j, B.length);
                j += B.length;
            }
            output = FMatrixRMaj.wrap(input.numRows, B.length, storage);
        }
        CommonOps_FDRM.multAdd(input, W, output);
        // apply activation function
        activationFunction.eval(storage);
        return output;
    }

    public void dump(ObjectOutputStream stream) throws IOException {
        stream.writeInt(activationFunction instanceof ActivationFunction.Identity ? 0 : (activationFunction instanceof ActivationFunction.Tanh ? 1 : (activationFunction instanceof ActivationFunction.ReLu ? 2 : (activationFunction instanceof ActivationFunction.SELU ? 3 : 1000))));
        stream.writeInt(W.numCols);
        stream.writeInt(W.numRows);
        for (int i=0, n = W.numCols*W.numRows; i < n; ++i)
            stream.writeFloat(W.data[i]);
        for (int i=0; i < B.length; ++i)
            stream.writeFloat(B[i]);
    }

    public static FullyConnectedLayer load(ObjectInputStream stream) throws IOException {

        final int activ = stream.readInt();
        final ActivationFunction function;
        if (activ == 0) function = new ActivationFunction.Identity();
        else if (activ == 1) function = new ActivationFunction.Tanh();
        else if (activ == 2) function = new ActivationFunction.ReLu();
        else if (activ == 3) function = new ActivationFunction.SELU();
        else throw new IllegalArgumentException("Unknown activation function with code " + activ);

        final int ncols = stream.readInt();
        final int nrows = stream.readInt();

        final float[] w = new float[ncols*nrows];
        for (int i=0, n = ncols*nrows; i < n; ++i) {
            w[i] = stream.readFloat();
        }

        final float[] b = new float[ncols];
        for (int i=0, n = ncols; i < n; ++i) {
            b[i] = stream.readFloat();
        }

        return new FullyConnectedLayer(nrows, ncols, w, b, function);
    }

    public String toString() {
        String p = "W[" + W.numRows + "," + W.numCols + "]x + B[" + B.length + "]";;
        if (activationFunction instanceof ActivationFunction.Identity) {
            return p;
        } else if (activationFunction instanceof ActivationFunction.Tanh) {
            return "tanh( " + p + " )";
        } else return "f( " + activationFunction.getClass().getName() + ", " + p + ")";
    }
}
