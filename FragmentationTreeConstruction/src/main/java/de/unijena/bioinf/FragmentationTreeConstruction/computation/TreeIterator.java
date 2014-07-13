package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 24.06.13
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public interface TreeIterator extends Iterator<FTree> {

    public double getLowerbound();

    public void setLowerbound(double lowerbound);

    public FGraph lastGraph();

}
