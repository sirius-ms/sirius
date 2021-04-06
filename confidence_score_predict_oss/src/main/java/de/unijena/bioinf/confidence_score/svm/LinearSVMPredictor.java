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

package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.io.*;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class LinearSVMPredictor implements Predictor {

    private double[] weight;
    private double bias;



    public LinearSVMPredictor(){}

    public LinearSVMPredictor(double[] weight, double bias){
        this.weight = weight;
        this.bias = bias;

    }


    public boolean predict(double[] features){
        if (features.length!= weight.length){
            System.out.println("weight "+ weight.length+" | features: "+features.length);
        }
        assert features.length== weight.length;
        double y = 0;
        for (int i = 0; i < features.length; i++) {
            y += weight[i]*features[i];
        }
//            if (DEBUG) System.out.println("y: "+y);
        return y+ bias >0;
    }

    public double score(double[] features) {
        assert features.length == weight.length;
        double y = 0;
        for (int i = 0; i < features.length; i++) {
            y += weight[i] * features[i];
        }
        return y+ bias;
    }

    @Override
    public double estimateProbability(double[] kernel) {
        return 0;
    }


    public double[] getWeight() {
        return weight;
    }

    public double getBias() {
        return bias;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Predictor{");
        sb.append("weight=");
        if (weight == null) sb.append("null");
        else {
            sb.append('[');
            for (int i = 0; i < weight.length; ++i)
                sb.append(i == 0 ? "" : ", ").append(weight[i]);
            sb.append(']');
        }
        sb.append(", bias=").append(bias);
        sb.append('}');
        return sb.toString();
    }

    private double sigmoid_predict(double decision_value, double A, double B) {
        double fApB = decision_value*A+B;
        if (fApB >= 0)
            return Math.exp(-fApB)/(1.0+Math.exp(-fApB));
        else
            return 1.0/(1+Math.exp(fApB)) ;
    }



    public static LinearSVMPredictor load(InputStream in) throws IOException {
        LinearSVMPredictor linearSVMPredictor = new LinearSVMPredictor();
        linearSVMPredictor.loadFromStream(in);
        return linearSVMPredictor;
    }

    public void dump(OutputStream outputStream) throws IOException {
        final DataOutputStream out = new DataOutputStream(outputStream);
        out.writeInt(weight.length);
        writeDoubleArray(out, weight);
        out.writeDouble(bias);

    }

    private void loadFromStream(InputStream inputStream) throws IOException {
        final DataInputStream in = new DataInputStream(inputStream);
        int length = in.readInt();
        this.weight = new double[length];
        readDoubleArray(in, weight);
        this.bias = in.readDouble();

    }

    private void writeDoubleArray(DataOutputStream out, double[] ary) throws IOException {
        for (int i=0; i < ary.length; ++i)
            out.writeDouble(ary[i]);
    }

    private void readDoubleArray(DataInputStream in, double[] ary) throws IOException {
        for (int i=0; i < ary.length; ++i)
            ary[i] = in.readDouble();
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.getListFromDictionary(dictionary, "weight");
        int size = document.sizeOfList(list);
        double[] w = new double[size];
        for (int i = 0; i < size; i++) w[i] = document.getDoubleFromList(list, i);
        this.weight = w;
        this.bias = document.getDoubleFromDictionary(dictionary, "bias");



    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (double d : weight) document.addToList(list, d);
        document.addListToDictionary(dictionary, "weight", list);
        document.addToDictionary(dictionary, "bias", bias);

    }
}
