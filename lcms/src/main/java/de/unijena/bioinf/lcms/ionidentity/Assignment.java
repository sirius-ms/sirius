package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.align.AlignedFeatures;

public interface Assignment {

    public void assignment(AlignedFeatures features, PrecursorIonType[] ionTypes, double[] probabilities);

}
