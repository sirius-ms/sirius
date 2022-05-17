package de.unijena.bioinf.model.lcms;

/**
 * Connection between two compounds/features
 */
public class IonConnection<T> {

    public static enum ConnectionType {
        IN_SOURCE_OR_ADDUCT;
    }

    private T left, right;
    private float weight;
    private ConnectionType type;

    public IonConnection(T left, T right, float weight, ConnectionType type) {
        this.left = left;
        this.right = right;
        this.type = type;
        this.weight = weight;
    }

    public T getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }

    public ConnectionType getType() {
        return type;
    }

    public float getWeight() {
        return weight;
    }

    public IonConnection<T> invert() {
        return new IonConnection<T>(right,left,weight, type);
    }
}
