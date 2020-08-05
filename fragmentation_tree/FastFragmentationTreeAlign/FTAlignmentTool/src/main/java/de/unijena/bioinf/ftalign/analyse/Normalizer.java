
package de.unijena.bioinf.ftalign.analyse;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.treealign.scoring.Scoring;

/**
 * @author Kai Dührkop
 */
public interface Normalizer {

    public double normalize(FTree left, FTree right, Scoring<Fragment> scoring,
                            float score);

}
