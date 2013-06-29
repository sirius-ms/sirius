package de.unijena.bioinf.ChemistryBase.data;

public abstract class ParameterHelper {

    public String getKeyName(Class<?> klass) {
        return klass.getCanonicalName();
    }

}
