package de.unijena.bioinf.sirius.elementdetection.transformer;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;

public enum Activation {

    IDENTITY(x->x),
    RELU(x->Math.max(x,0)),
    SIGMOID(Activation::sigm);

    private final Float2FloatFunction function;

    private Activation(Float2FloatFunction f) {
        this.function=f;
    }

    public float apply(float value) {
        return function.apply(value);
    }

    private static float sigm(double v) {
        if (v >= 0)
            return (float)(1.0/(1+Math.exp(-v))) ;
        else
            return (float)(Math.exp(v)/(1.0+Math.exp(v)));

    }

}
