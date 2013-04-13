package de.unijena.bioinf.MassDecomposer;

class Weight<T> implements Comparable<Weight<T>> {

    private final T owner;
    private final double mass;
    private long integerMass;
    private long l;
    private long lcm;

    public Weight(T owner, double mass) {
        this.owner = owner;
        this.mass = mass;
    }

    public T getOwner() {
        return owner;
    }

    public double getMass() {
        return mass;
    }

    public long getIntegerMass() {
        return integerMass;
    }

    public void setIntegerMass(long integerMass) {
        this.integerMass = integerMass;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public long getLcm() {
        return lcm;
    }

    public void setLcm(long lcm) {
        this.lcm = lcm;
    }

    @Override
    public int compareTo(Weight<T> tWeight) {
        return (int)Math.signum(mass - tWeight.mass);
    }
}
