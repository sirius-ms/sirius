package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 4/24/13
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Preprocessor {

    public Ms2Experiment process(Ms2Experiment experiment);

}
