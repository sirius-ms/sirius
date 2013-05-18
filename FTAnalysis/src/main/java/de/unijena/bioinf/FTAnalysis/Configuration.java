package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.Set;

public interface Configuration {

    public int filterBySimulatedMS1();

    public int numberOfTrees();

    public Set<Element> getBasicSet();

    public Deviation getFragmentDeviation();

    public Deviation getParentPeakDeviation();

    public double getGamma();

    public double getLambda();

    public Set<Methods> getMethods();

}
