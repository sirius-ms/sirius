package de.unijena.bioinf.canopus.dnn;

public interface ActivationFunction {

    void eval(float[] values);

    class Identity implements ActivationFunction {

        @Override
        public void eval(float[] values) {

        }
    }

    class SELU implements ActivationFunction {
        private final static double alpha = 1.6732632423543772848170429916717;
        private final static double lambda = 1.0507009873554804934193349852946;
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                final double x = values[i];
                values[i] = (float)((x > 0) ? lambda*x : lambda*(alpha*Math.exp(x) - alpha));
            }
        }
    }

    class Tanh implements ActivationFunction {
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                values[i] = (float)Math.tanh(values[i]);
            }
        }
    }

    class ReLu implements ActivationFunction {
        @Override
        public void eval(float[] values) {
            for (int i=0; i < values.length; ++i) {
                values[i] = Math.max(0, values[i]);
            }
        }
    }

}
