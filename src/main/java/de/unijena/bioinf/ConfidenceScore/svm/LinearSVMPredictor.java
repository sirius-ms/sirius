package de.unijena.bioinf.ConfidenceScore.svm;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ConfidenceScore.Predictor;

import java.io.*;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class LinearSVMPredictor implements Predictor {

    private double[] weight;
    private double bias;

    private double probA, probB;

    public LinearSVMPredictor(){}

    public LinearSVMPredictor(double[] weight, double bias, double probA, double probB){
        this.weight = weight;
        this.bias = bias;
        this.probA = probA;
        this.probB = probB;
    }

    public LinearSVMPredictor(double[] weight, double bias){
        this(weight,bias,Double.NaN, Double.NaN);
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

    public double estimateProbability(double[] kernel) {
        if (Double.isNaN(probA)) throw new UnsupportedOperationException("This predictor cannot estimate probabilities");
        return sigmoid_predict(score(kernel), probA, probB);
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
        out.writeDouble(probA);
        out.writeDouble(probB);
    }

    private void loadFromStream(InputStream inputStream) throws IOException {
        final DataInputStream in = new DataInputStream(inputStream);
        int length = in.readInt();
        this.weight = new double[length];
        readDoubleArray(in, weight);
        this.bias = in.readDouble();
        this.probA = in.readDouble();
        this.probB = in.readDouble();
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

        try {
            this.probA = document.getDoubleFromDictionary(dictionary, "probA");
        } catch (Exception e){
            if (!e.getMessage().contains("JSONObject[\"probA\"] not found") && !(e instanceof NullPointerException)) throw e;
            this.probA = Double.NaN;
        }
        try {
            this.probB = document.getDoubleFromDictionary(dictionary, "probB");
        } catch (Exception e){
            if (!e.getMessage().contains("JSONObject[\"probB\"] not found") && !(e instanceof NullPointerException)) throw e;
            this.probB = Double.NaN;
        }


    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (double d : weight) document.addToList(list, d);
        document.addListToDictionary(dictionary, "weight", list);
        document.addToDictionary(dictionary, "bias", bias);
        if (!Double.isNaN(probA)) document.addToDictionary(dictionary, "probA", probA);
        if (!Double.isNaN(probB)) document.addToDictionary(dictionary, "probB", probB);
    }
}
