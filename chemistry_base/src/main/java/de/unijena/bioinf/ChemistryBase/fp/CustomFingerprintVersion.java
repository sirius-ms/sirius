package de.unijena.bioinf.ChemistryBase.fp;

import java.util.List;

public class CustomFingerprintVersion extends FingerprintVersion{

    protected final String name;
    protected final List<MolecularProperty> properties;
    protected final int size;

    public CustomFingerprintVersion(String name, int size) {
        this.name = name;
        this.size = size;
        this.properties = null;
    }

    public CustomFingerprintVersion(String name, List<MolecularProperty> properties) {
        this.name = name;
        this.properties = properties;
        this.size = properties.size();
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return properties==null ? new SpecialMolecularProperty("?") : properties.get(index);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (fingerprintVersion instanceof CustomFingerprintVersion) {
            return ((CustomFingerprintVersion) fingerprintVersion).name == this.name;
        } else return false;
    }
}
