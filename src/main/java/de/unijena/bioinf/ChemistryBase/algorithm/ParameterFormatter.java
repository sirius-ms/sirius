package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;

public interface ParameterFormatter {

    public <G, D, L> G format(DataDocument<G, D, L> document, Object value);

}
