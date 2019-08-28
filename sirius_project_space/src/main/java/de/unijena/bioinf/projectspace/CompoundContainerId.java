package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.projectspace.ProjectSpaceContainerId;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class CompoundContainerId extends ProjectSpaceContainerId {

    private String directoryName;
    private String compoundName;
    private int compoundIndex;

    protected Lock containerLock;

    public CompoundContainerId(String directoryName, String compoundName, int compoundIndex) {
        this.directoryName = directoryName;
        this.compoundName = compoundName;
        this.compoundIndex = compoundIndex;
        this.containerLock = new ReentrantLock();
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

    /**
     * This operation is only allowed to be called with careful synchronization within the project space
     */
    void rename(String newName) {
        this.compoundName = newName;
    }

}
