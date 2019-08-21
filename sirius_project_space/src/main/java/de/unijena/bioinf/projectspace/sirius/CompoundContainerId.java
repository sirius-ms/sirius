package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.ProjectSpaceContainerId;

public class CompoundContainerId extends ProjectSpaceContainerId {

    private String directoryName;
    private String compoundName;
    private int compoundIndex;

    public CompoundContainerId(String directoryName, String compoundName, int compoundIndex) {
        this.directoryName = directoryName;
        this.compoundName = compoundName;
        this.compoundIndex = compoundIndex;
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
}
