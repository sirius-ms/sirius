package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 05.03.14
 * Time: 00:54
 * *
 */

public class CEdgeUBComperator implements Comparator<Loss> {

	/**
	 * This will sort edges by the upper bound values of their target vertices
	 * - they will be sorted in descending order ( from high to lower value )
	 */
	double[] ub;

	public CEdgeUBComperator( double[] UB ) {
		if( UB == null )
			throw new NullPointerException( "Edge Comperator should be created with valid upper bounds array!" );

		this.ub = UB;
	}

	@Override
	public int compare( Loss o1, Loss o2 ) {

		if( o1.getWeight() + ub[o1.getTarget().getVertexId()] > o2.getWeight() + ub[o2.getTarget().getVertexId()] )
			return -1;
		else if( o1.getWeight() + ub[o1.getTarget().getVertexId()] < o2.getWeight() + ub[o2.getTarget().getVertexId()] )
			return 1;
		else
			return 0;
	}
}
