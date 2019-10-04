package de.unijena.bioinf.projectspace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class CompoundContainerId extends ProjectSpaceContainerId {
    protected Lock containerLock;

    // ID defining fields
    private String directoryName;
    private String compoundName;
    private int compoundIndex;

    // fields for fast compound filtering
    //todo we may want to use annotations here in future if we want to allow
    // for arbitrary filtering of compounds
    private double ionMass;


    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex) {
        this(directoryName, compoundName, compoundIndex, Double.NaN);
    }

    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex, double ionMass) {
        this.directoryName = directoryName;
        this.compoundName = compoundName;
        this.compoundIndex = compoundIndex;
        this.containerLock = new ReentrantLock();
        this.ionMass = ionMass;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public int getCompoundIndex() {
        return compoundIndex;
    }

    public double getIonMass() {
        return ionMass;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    /**
     * This operation is only allowed to be called with careful synchronization within the project space
     */
    void rename(String newName) {
        this.compoundName = newName;
    }

    @Override
    public String toString() {
        return compoundIndex
                + "_" + compoundName + "_" + Math.round(ionMass) + "m/z";
    }

    public Map<String, String> asKeyValuePairs() {
        Map<String, String> kv = new LinkedHashMap<>(3);
        kv.put("index", String.valueOf(getCompoundIndex()));
        kv.put("name", getCompoundName());
        kv.put("ionMass", String.valueOf(ionMass));
        return kv;
    }
}
