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

package de.unijena.bioinf.sirius.elementdetection.prediction;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

class TrainedElementDetectionNetwork {

    public int numberOfPeaks() {
        return npeaks;
    }

    protected enum ActivationFunction {
        LINEAR, RELU, TANH
    }

    protected interface Layer {
        double[] activate(double[] vector);
    }

    protected static class PreprocessingLayer implements Layer {
        protected final double[] centering, scaling;

        public PreprocessingLayer(double[] centering, double[] scaling) {
            this.centering = centering;
            this.scaling = scaling;
        }

        @Override
        public double[] activate(double[] vector) {
            final double[] output = new double[vector.length];
            for (int i=0; i < centering.length; ++i) {
                output[i] = (vector[i]-centering[i])/scaling[i];
            }
            return output;
        }
    }

    protected static class PlattSigmoidLayer implements Layer {
        protected final double[] As, Bs;

        public PlattSigmoidLayer(double[] as, double[] bs) {
            As = as;
            Bs = bs;
        }

        @Override
        public double[] activate(double[] vector) {
            final double[] output = new double[vector.length];
            for (int i=0; i < As.length; ++i) {
                output[i] = sigmoid_predict(vector[i], As[i], Bs[i]);
            }
            return output;
        }


        private static double sigmoid_predict(double decision_value, double A, double B)
        {
            double fApB = decision_value*A+B;
            if (fApB >= 0)
                return Math.exp(-fApB)/(1.0+Math.exp(-fApB));
            else
                return 1.0/(1+Math.exp(fApB)) ;
        }
    }


    private static class ExponentialLayer implements Layer {
        public ExponentialLayer() {

        }


        @Override
        public double[] activate(double[] vector) {
            final double[] out = new double[vector.length];
            for (int i=0; i < vector.length; ++i) {
                out[i] = Math.exp(vector[i]);
            }
            return out;
        }
    }



    protected static class FullyConnectedLayer implements Layer {
        protected final double[][] W;
        protected final double[] b;
        protected final ActivationFunction function;

        public FullyConnectedLayer(double[][] w, double[] b, ActivationFunction f) {
            W = w;
            this.b = b;
            this.function = f;
        }

        public double[] activate(double[] vector) {
            final double[] result = new double[W.length];
            for (int row=0; row < W.length; ++row) {
                final double[] w = W[row];
                double score = b[row];
                for (int i=0; i < w.length; ++i) {
                    score += vector[i]*w[i];
                }
                switch (function) {
                    case RELU:
                        score = Math.max(0, score); break;
                    case TANH:
                        score = Math.tanh(score); break;
                    case LINEAR:
                }
                result[row] = score;
            }
            return result;
        }

    }

    private final static int INPUT_SIZE = 69;
    private final static int[] NEURONS = new int [] {48, 32, 5};
    private final static ActivationFunction[] ACTIVATION_FUNCTIONS = new ActivationFunction[]{
            ActivationFunction.TANH,
            ActivationFunction.TANH, ActivationFunction.LINEAR
    };


    public static TrainedElementDetectionNetwork readRegressionNetwork(InputStream inputStream) throws IOException {
        try (final DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream))) {
            final PeriodicTable T = PeriodicTable.getInstance();
            final int npeaks = stream.readInt();
            final int nfeatures = stream.readInt();
            final int npredictors = stream.readInt();
            final Element[] elems = new Element[npredictors];
            for (int i=0; i < elems.length; ++i)
                elems[i] = T.get(stream.readInt());
            final int nlayers = stream.readInt();
            final int[] neurons = new int[nlayers];
            for (int i=0; i < neurons.length; ++i)
                neurons[i] = stream.readInt();
            final int length = stream.readInt();
            final double[] vec = new double[length];
            for (int i=0; i < vec.length; ++i) {
                vec[i] = stream.readDouble();
            }

            final Layer[] layers = new Layer[nlayers+2];
            int k=0;
            final double[] centering = new double[nfeatures], normalization = new double[nfeatures];
            for (int i=0; i < nfeatures; ++i) centering[i] = vec[k++];
            for (int i=0; i < nfeatures; ++i) normalization[i] = vec[k++];
            layers[0] = new PreprocessingLayer(centering, normalization);
            int in = nfeatures;
            for (int l=0; l < nlayers; ++l) {
                final double[][] W = new double[neurons[l]][in];
                final double[] B = new double[neurons[l]];
                for (int i=0; i < W.length; ++i) {
                    final double[] row = W[i];
                    for (int j=0; j < row.length; ++j) {
                        row[j] = vec[k++];
                    }
                }
                for (int i=0; i < B.length; ++i) {
                    B[i] = vec[k++];
                }
                in = neurons[l];
                layers[l+1] = new FullyConnectedLayer(W, B, l < nlayers-1 ? ActivationFunction.TANH : ActivationFunction.LINEAR);
            }
            layers[nlayers+1] = new ExponentialLayer();
            return new TrainedElementDetectionNetwork(npeaks, layers);


        }
    }

    public static TrainedElementDetectionNetwork readNetwork(InputStream inputStream) throws IOException {
        try (final DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream))) {
            final PeriodicTable T = PeriodicTable.getInstance();
            final int npeaks = stream.readInt();
            final int nfeatures = stream.readInt();
            final int npredictors = stream.readInt();
            final Element[] elems = new Element[npredictors];
            final double[] As = new double[npredictors], Bs = new double[npredictors];
            for (int i=0; i < elems.length; ++i)
                elems[i] = T.get(stream.readInt());
            for (int i=0; i < elems.length; ++i)
                As[i] = stream.readDouble();
            for (int i=0; i < elems.length; ++i)
                Bs[i] = stream.readDouble();
            final int nlayers = stream.readInt();
            final int[] neurons = new int[nlayers];
            for (int i=0; i < neurons.length; ++i)
                neurons[i] = stream.readInt();
            final int length = stream.readInt();
            final double[] vec = new double[length];
            for (int i=0; i < vec.length; ++i) {
                vec[i] = stream.readDouble();
            }

            final Layer[] layers = new Layer[nlayers+2];
            int k=0;
            final double[] centering = new double[nfeatures], normalization = new double[nfeatures];
            for (int i=0; i < nfeatures; ++i) centering[i] = vec[k++];
            for (int i=0; i < nfeatures; ++i) normalization[i] = vec[k++];
            layers[0] = new PreprocessingLayer(centering, normalization);
            int in = nfeatures;
            for (int l=0; l < nlayers; ++l) {
                final double[][] W = new double[neurons[l]][in];
                final double[] B = new double[neurons[l]];
                for (int i=0; i < W.length; ++i) {
                    final double[] row = W[i];
                    for (int j=0; j < row.length; ++j) {
                        row[j] = vec[k++];
                    }
                }
                for (int i=0; i < B.length; ++i) {
                    B[i] = vec[k++];
                }
                in = neurons[l];
                layers[l+1] = new FullyConnectedLayer(W, B, l < nlayers-1 ? ActivationFunction.TANH : ActivationFunction.LINEAR);
            }
            layers[nlayers+1] = new PlattSigmoidLayer(As, Bs);
            return new TrainedElementDetectionNetwork(npeaks, layers);


        }
    }

    private final int npeaks;
    private final Layer[] layers;

    protected TrainedElementDetectionNetwork(int npeaks, Layer[] layers) {
        this.layers = layers;
        this.npeaks = npeaks;
    }

    public double[] predict(SimpleSpectrum spectrum) {
        double[] inputVector = new FeatureVector(spectrum, npeaks).getFeatureVector(npeaks);
        for (int i=0; i < layers.length; ++i) {
            inputVector = layers[i].activate(inputVector);
        }
        return inputVector;
    }



}
