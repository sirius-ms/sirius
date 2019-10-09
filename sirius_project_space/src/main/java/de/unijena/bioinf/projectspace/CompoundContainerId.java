package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

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
    //todo switch that to annotations?
    private double ionMass;
    private PrecursorIonType ionType;


    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex) {
        this(directoryName, compoundName, compoundIndex, Double.NaN, null);
    }

    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex, double ionMass, PrecursorIonType ionType) {
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

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public void setIonType(PrecursorIonType ionType) {
        this.ionType = ionType;
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
        if (ionType != null)
            kv.put("ionType", ionType.toString());

        return kv;
    }
}
