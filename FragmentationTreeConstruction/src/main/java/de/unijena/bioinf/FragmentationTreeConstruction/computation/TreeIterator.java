package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 24.06.13
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public interface TreeIterator extends Iterator<FragmentationTree> {

    public void setLowerbound(double lowerbound);
    public double getLowerbound();
    public FragmentationGraph lastGraph();

}
