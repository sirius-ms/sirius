package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 06.09.13
 * Time: 01:42
 * To change this template use File | Settings | File Templates.
 */
public class SiriusProfile extends Profile {
    public SiriusProfile()  {
        super(IsotopePatternAnalysis.defaultAnalyzer(), FragmentationPatternAnalysis.oldSiriusAnalyzer());
    }
}
