package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTGraph;

import java.util.Iterator;
import java.util.List;

public interface FragmentationPathway extends FTGraph<Fragment> {

    public Fragment getRoot();

    public List<ProcessedPeak> getPeaks();

    public List<? extends Fragment> getFragments();

    public List<? extends Fragment> getFragmentsWithoutRoot();

    public Iterator<Loss> lossIterator();

    public int numberOfColors();

    public int numberOfVertices();

    public int numberOfEdges();

}
