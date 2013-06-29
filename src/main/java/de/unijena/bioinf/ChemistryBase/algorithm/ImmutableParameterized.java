package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;

public interface ImmutableParameterized<T> {

    public <G,D,L> T readFromParameters(ParameterHelper helper, DataDocument<G,D,L> document, D dictionary);

    public <G,D,L> void exportParameters(ParameterHelper helper, DataDocument<G,D,L> document, D dictionary);

}
