package de.unijena.bioinf.ChemistryBase.ms;


import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public enum NormalizationMode implements ImmutableParameterized {
    SUM, MAX;

    @Override
    public Object readFromParameters(ParameterHelper helper, DataDocument document, Object dictionary) {
        String mode = document.getStringFromDictionary(dictionary, "mode");
        return NormalizationMode.valueOf(mode);
    }

    @Override
    public void exportParameters(ParameterHelper helper, DataDocument document, Object dictionary) {
        document.addToDictionary(dictionary, "mode", toString());
    }
}
