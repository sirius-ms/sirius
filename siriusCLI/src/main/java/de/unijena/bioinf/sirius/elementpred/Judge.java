package de.unijena.bioinf.sirius.elementpred;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Simple voting system: Each judge can give positive or negative hints for a certain element. If an element has positive
 * vote in the end, it will be added to the alphabet.
 *
 * This system should be replaced by a ML approach as soon as Marvin published his paper
 */
public interface Judge {

    public void vote(TObjectIntHashMap<Element> votes, Ms2Experiment experiment, MeasurementProfile profile);

}
