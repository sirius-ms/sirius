package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 22.03.14
 * Time: 01:11
 * *
 */

public class CEdgeColorEndpointComperator implements Comparator<Loss> {

	@Override
	public final int compare( final Loss A, final Loss B ) {

		// this is used in Edge-arrays of vertices; therefore, at some point, when b is null, every value a will be null too
		// result: null is higher weighted, than non-null values!

		/* older version, where i had null-entries inside the edge-arrays when using this

		if ( a == null )
			if ( b == null )
				return 0;
			else
				return 1;
		else if ( b == null ) {
			return -1; // a cannot be null here
		}

		if ( a.gTargetVertex.gColor == b.gTargetVertex.gColor )
			return 0;
		else
			return ( a.gTargetVertex.gColor < b.gTargetVertex.gColor ) ? -1 : 1;
		*/

		if ( A.getTarget().getColor() == B.getTarget().getColor() )
			return 0;
		else
			return ( A.getTarget().getColor() < B.getTarget().getColor() ) ? -1 : 1;
	}
}
