package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 08.03.16.
 */
abstract public class Scaler implements Parameterized{
    public Scaler(double[][] matrix){
        if (matrix!=null) compute(matrix);
    };

    protected abstract void compute(double[][] matrix);

    public abstract double[][] scale(double[][] matrix);

    public abstract double[] scale(double[] matrix);

    public static class StandardScaler extends Scaler {
        private double[] mean;
        private double[] sd;

        public StandardScaler(){
            super(null);
        }

        public StandardScaler(double[][] matrix) {
            super(matrix);
        }

        @Override
        protected void compute(double[][] matrix) {
            final int instanceSize = matrix.length;
            final int featureSize = matrix[0].length;
            final double[] mean = new double[featureSize];
            for (double[] doubles : matrix) {
                for (int i = 0; i < doubles.length; i++) {
                    mean[i] += doubles[i] / instanceSize;
                }
            }

            final double[] sd = new double[featureSize];
            for (double[] doubles : matrix) {
                for (int i = 0; i < doubles.length; i++) {
                    sd[i] += Math.pow(doubles[i] - mean[i], 2);
                }
            }
            for (int i = 0; i < sd.length; i++) {
                sd[i] = Math.sqrt(sd[i]/instanceSize);
            }
            this.mean = mean;
            this.sd = sd;

        }

        @Override
        public double[][] scale(double[][] matrix) {
            double[][] newMatrix = new double[matrix.length][];
            for (int i = 0; i < matrix.length; i++) {
                final double[] doubles = matrix[i];
                newMatrix[i] = scale(doubles);
            }
            return newMatrix;
        }

        @Override
        public double[] scale(double[] matrix) {
            final double[] newDoubles = new double[matrix.length];
            for (int j = 0; j < matrix.length; j++) {
                if (sd[j]==0){
                    newDoubles[j] = (matrix[j]-mean[j]);
                } else {
                    newDoubles[j] = (matrix[j]-mean[j]) / sd[j];
                }

            }
            return newDoubles;
        }

        public double[] getSD(){return sd.clone();};
        public double[] getMean(){return mean.clone();};

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("StandardScaler{");
            sb.append("mean=");
            if (mean == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < mean.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(mean[i]);
                sb.append(']');
            }
            sb.append(", sd=");
            if (sd == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < sd.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(sd[i]);
                sb.append(']');
            }
            sb.append('}');
            return sb.toString();
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            L list = document.getListFromDictionary(dictionary, "mean");
            int size = document.sizeOfList(list);
            double[] mean = new double[size];
            for (int i = 0; i < size; i++) mean[i] = document.getDoubleFromList(list, i);
            this.mean = mean;
            list = document.getListFromDictionary(dictionary, "sd");
            size = document.sizeOfList(list);
            double[] sd = new double[size];
            for (int i = 0; i < size; i++) sd[i] = document.getDoubleFromList(list, i);
            this.sd = sd;
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            L list = document.newList();
            for (double m : mean) document.addToList(list, m);
            document.addListToDictionary(dictionary, "mean", list);
            list = document.newList();
            for (double s : sd) document.addToList(list, s);
            document.addListToDictionary(dictionary, "sd", list);
        }
    }

    public static class NoScaler extends Scaler {

        public NoScaler(){
            super(null);
        }

        public NoScaler(double[][] matrix) {
            super(matrix);
        }

        @Override
        protected void compute(double[][] matrix) {
        }

        @Override
        public double[][] scale(double[][] matrix) {
            double[][] newMatrix = new double[matrix.length][];
            for (int i = 0; i < matrix.length; i++) {
                newMatrix[i] = Arrays.copyOf(matrix[i], matrix[i].length);

            }
            return newMatrix;
        }

        @Override
        public double[] scale(double[] matrix) {
            return Arrays.copyOf(matrix, matrix.length);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("NoScaler{");
            sb.append('}');
            return sb.toString();
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            //nothing to do
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            //nothing to do
        }
    }


}
