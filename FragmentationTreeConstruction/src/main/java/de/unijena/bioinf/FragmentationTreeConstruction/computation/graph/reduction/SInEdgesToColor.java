package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 24.01.14
 * Time: 01:47
 * *
 */

public class SInEdgesToColor {

	/*
	 * to keep up the current performance, i do only use this for the strengthened version of seb-vertex-ubs
	 */
	final static Fragment zeroVertex = createNullVertex();
	final static Loss zeroEdge = new Loss( zeroVertex, zeroVertex );
	final static Loss infinityEdge = new Loss( zeroVertex, zeroVertex );

	Loss besLoss = SInEdgesToColor.zeroEdge;
	Loss secondBesLoss = SInEdgesToColor.infinityEdge;
	double maxInEdge = Double.NEGATIVE_INFINITY;

	protected static Fragment createNullVertex() {

		Fragment f = new Fragment( -1 ); // || creates vertex with no formula?
		f.setColor( -1 );

		return f;
	}

	protected static void init() {
		zeroEdge.setWeight( 0.0 );
		infinityEdge.setWeight( Double.NEGATIVE_INFINITY );
	}

	/**
	 * color-array-merge function for (strengthend) seb-vertex-ubs calculation function
	 * @param bestInEdgeToColor
	 * @param newEdge
	 */
	public final static void sebUBMergeEdge( SInEdgesToColor bestInEdgeToColor, Loss newEdge, double newEdgeMaxInEdge ) {

		if ( newEdge.getWeight() >= bestInEdgeToColor.besLoss.getWeight() ) {  // Important to make this >=, so 2 edges of equal, max weight edges will become 1st and 2nd best
			// IMPORTANT: We need to check that the edge being added is not already here!  This can happen when the same edge is recorded as the best in-edge
			// for a colour via 2 different children, because the ">=" (as opposed to ">") will allow it!  This fixes a longstanding bug.
			if ( newEdge == bestInEdgeToColor.besLoss ) {
				bestInEdgeToColor.maxInEdge = Math.max( bestInEdgeToColor.maxInEdge, newEdgeMaxInEdge );
				return;
			}

			bestInEdgeToColor.secondBesLoss = bestInEdgeToColor.besLoss;
			bestInEdgeToColor.besLoss = newEdge;
			bestInEdgeToColor.maxInEdge = newEdgeMaxInEdge;
		} else if ( newEdge.getWeight() > bestInEdgeToColor.secondBesLoss.getWeight() ) {
			bestInEdgeToColor.secondBesLoss = newEdge; // We don't record any maxInEdge for the 2nd-best edge
		}

		assert( bestInEdgeToColor.besLoss == SInEdgesToColor.zeroEdge || bestInEdgeToColor.besLoss != bestInEdgeToColor.secondBesLoss ) : "Assert-ERROR! Seb vertex ubs merge allocated the same edge as best and second best!";
	}
}
