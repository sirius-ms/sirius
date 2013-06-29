package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;

public interface Parameterized {

    public <G,D,L> void importParameters(ParameterHelper helper, DataDocument<G,D,L> document, String key);

    public <G,D,L> void exportParameters(ParameterHelper helper, DataDocument<G,D,L> document, String key);

}
