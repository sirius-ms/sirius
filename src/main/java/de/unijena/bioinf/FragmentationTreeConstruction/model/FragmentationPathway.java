package de.unijena.bioinf.FragmentationTreeConstruction.model;

import java.util.Iterator;
import java.util.List;

public interface FragmentationPathway {

    public Fragment getRoot();

    public List<ProcessedPeak> getPeaks();

    public List<? extends Fragment> getFragments();

    public List<? extends Fragment> getFragmentsWithoutRoot();

    public Iterator<Loss> lossIterator();

    public int numberOfColors();

    public int numberOfVertices();

}
