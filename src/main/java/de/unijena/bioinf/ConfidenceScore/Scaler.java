package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

/**
 * Created by Marcus Ludwig on 08.03.16.
 */
abstract public class Scaler implements Parameterized{
    public Scaler(double[][] matrix){
        if (matrix!=null) compute(matrix);
    }

    protected abstract void compute(double[][] matrix);

    public abstract double[][] scale(double[][] matrix);

    public abstract double[] scale(double[] matrix);

    public abstract int[] divergingFeatures(double[] matrix);

    public abstract boolean hasDivergingFeatures(double[] matrix);
    /**
     * scale to mean 0 and standard deviation 1.
     */
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

        @Override
        public int[] divergingFeatures(double[] matrix) {
            TIntArrayList idx = new TIntArrayList();
            for (int i = 0; i < matrix.length; i++) {
                if (sd[i]==0) continue;
                if (Math.abs((matrix[i]-mean[i])/sd[i])>5) idx.add(i);
            }
            return idx.toArray();
        }

        @Override
        public boolean hasDivergingFeatures(double[] matrix) {
            return divergingFeatures(matrix).length>0;
        }

        public double[] getSD(){return sd.clone();}

        public double[] getMean(){return mean.clone();}

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
        public int[] divergingFeatures(double[] matrix) {
            return new int[0];
        }

        @Override
        public boolean hasDivergingFeatures(double[] matrix) {
            return false;
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


    /**
     * scale to mean 0 and standard deviation 1.
     */
    public static class MinMaxScaler extends Scaler {
        private double[] data_min;
        private double[] data_max;
        private double min = -1;
        private double max = 1;

        public MinMaxScaler(){
            super(null);
        }

        public MinMaxScaler(double[][] matrix) {
            super(matrix);
        }

        @Override
        protected void compute(double[][] matrix) {
            final int featureSize = matrix[0].length;
            final double[] min = new double[featureSize];
            Arrays.fill(min, Double.POSITIVE_INFINITY);
            final double[] max = new double[featureSize];
            Arrays.fill(max, Double.NEGATIVE_INFINITY);
            for (double[] doubles : matrix) {
                for (int i = 0; i < doubles.length; i++) {
                    double value = doubles[i];
                    min[i] =  Math.min(min[i], value);
                    max[i] =  Math.max(max[i], value);
                }
            }

            this.data_min = min;
            this.data_max = max;
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
                final double std = matrix[j]-data_min[j]/(data_max[j]-data_min[j]);
                newDoubles[j] = std*(max-min)+min;
            }
            return newDoubles;
        }

        @Override
        public int[] divergingFeatures(double[] matrix) {
            TIntArrayList idx = new TIntArrayList();
            for (int i = 0; i < matrix.length; i++) {
                final double std = matrix[i]-data_min[i]/(data_max[i]-data_min[i]);
                if (std>2||std<-1) idx.add(i);
            }
            return idx.toArray();
        }

        @Override
        public boolean hasDivergingFeatures(double[] matrix) {
            return divergingFeatures(matrix).length>0;
        }

        public double[] getMins(){return data_min.clone();}

        public double[] getMaxs(){return data_max.clone();}

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("MinMaxScaler{");
            sb.append("data_min=");
            if (data_min == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < data_min.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(data_min[i]);
                sb.append(']');
            }
            sb.append(", data_max=");
            if (data_max == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < data_max.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(data_max[i]);
                sb.append(']');
            }
            sb.append(", min=").append(min);
            sb.append(", max=").append(max);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            L list = document.getListFromDictionary(dictionary, "data_min");
            int size = document.sizeOfList(list);
            double[] data_min = new double[size];
            for (int i = 0; i < size; i++) data_min[i] = document.getDoubleFromList(list, i);
            this.data_min = data_min;
            list = document.getListFromDictionary(dictionary, "data_max");
            size = document.sizeOfList(list);
            double[] data_max = new double[size];
            for (int i = 0; i < size; i++) data_max[i] = document.getDoubleFromList(list, i);
            this.data_max = data_max;
            this.min = document.getDoubleFromDictionary(dictionary, "min");
            this.max = document.getDoubleFromDictionary(dictionary, "max");
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            L list = document.newList();
            for (double m : data_min) document.addToList(list, m);
            document.addListToDictionary(dictionary, "data_min", list);
            list = document.newList();
            for (double m : data_max) document.addToList(list, m);
            document.addListToDictionary(dictionary, "data_max", list);
            document.addToDictionary(dictionary, "min", min);
            document.addToDictionary(dictionary, "max", max);
        }
    }

}
