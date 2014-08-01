package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.lang.Double;import java.lang.IndexOutOfBoundsException;
import java.lang.Integer;
import java.lang.Math;
import java.lang.String;
import java.lang.System;
import java.util.*;

/**
 * Created by Xentrics on 30.03.14.
 */
public class TReduce {

    final FGraph gGraph;
	// the vertex array is now accessed by every reduction methods that needs it
	// it will be top sorted and it should allow us to keep up with the necessary reduction conditions, even if we do
	// not have direct access to the inner graph structure
    final protected ArrayList<Fragment> VERTS;

    // traverse related
    protected BitSet gTraversedVertex;

    // bounds
    protected double[] gUB; // global upper bounds for vertices
    protected double[][] gLB; // global lower bounds for vertices

    protected boolean gRememberVertexIDs = true;
    protected boolean shouldCheckPreconds = false;	// If true, various actions will perform tests of various preconditions (e.g. to check that vertices are numbered in topological order)
    protected boolean shouldMaximizeSpeed = false;

	protected boolean doDebug = true;

    ////////////////////////////
    /// --- CONSTRUCTORS --- ///

    public TReduce ( final FGraph G ) {

        gTraversedVertex = new BitSet( G.numberOfVertices() );
        gGraph = G;
		VERTS = new ArrayList<Fragment>( G.numberOfVertices() );
		calcNewVertexIds( doDebug );
    }


/////
	///////////////////////////////
///////--- RENUMBER VERTICES ---///////
	///////////////////////////////
////


	private ArrayList<Integer> newColFor;
	private ArrayList<Integer> newIdFor;
	private LinkedList<Fragment>[] topSortColorToVertex;  // HACK: This is used only by topSort(), though it could in fact be used elsewhere and this might save time.
	private BitSet[] topSortColourToColour;  // topSortColourEdgesToColour[i][j] is the jth colour (in no particular order) that has an edge to colour i.
	private int gNextColorID = 0;
	private int gNexFragmentID = 0;

	// The (only?) right way is to actually forget about topologically sorting vertices, and topologically sort colours instead.
	// To store these colour-edges, we *could* use a flat array plus indexes into it a la startOfEdgesTo[], but it's easier and still
	// fast enough to just use an array of set<>s.
	// Much simpler than the previous approach!  :)
	// TODO: check for correctness
	private void topSort( int col ) {

		if( newColFor.get( col ) == -1 ) {

			// Haven't visited this colour yet.
			// Find the colours of all its parents (a colour x is a parent of a colour y if there is an edge from any x-coloured vertex to any y-coloured vertex)
			// and recurse to process them first.

			BitSet B = topSortColourToColour[col];
			for ( int i=0; i<gGraph.maxColor()+1; i++ ) {
				if( B.get( i ) ) // this color
					topSort(i);
			}

			newColFor.set( col, gNextColorID-- );

			// Now renumber all vertices of this colour.  It's safe to just assign consecutive integers,
			// since we know that no two vertices of the same colour can have any edges between each other.
			for( Fragment v : topSortColorToVertex[col] ) {
				newIdFor.set( v.getVertexId(), gNexFragmentID-- );  // assign new array positions
			}
		}
	}

	/**
	 * Now both renames vertices using newIdFor[] AND (if calcInvert is true) turns newIdFor[] into its inverse (for a later call to revertVertices()).
	 * TODO: check for correctness
	 * TODO: IMPORTANT
	 */
	private void renameVertices( boolean calcInvert, boolean doReCheck, boolean rememberIDs ) {

		List<Fragment> vertices = gGraph.getFragments();
		ArrayList<Fragment> newVertexArray = new ArrayList<Fragment>(  gGraph.numberOfVertices() );

		// renumber by re-positioning the vertices in a new array, so that:
		// (u,v) => u < v  and (u,v) => c(u) < c(v)
		int p;
		int i=0;
		if ( rememberIDs ) {
			for ( Fragment v : gGraph.getFragments() ) {

				assert ( i == v.getVertexId() ) : "Asynchronous vertex indices in original graph!";

				p = newIdFor.get( v.getVertexId() );
				newVertexArray.set( p, v ); // getVertexId() and getColor() are not changed here yet!

				// remember the original values here, so we can use them later, eventually
				gOriginal_Colors_And_IDs[p][TReduce.POS] = v.getVertexId();
				gOriginal_Colors_And_IDs[p][TReduce.COL] = v.getColor();

				vertices[i].getVertexId() = p;
				i++;
			}
		} else {
			for (int i = 0; i < gGraph.numberOfVertices(); i++) {

				p = newIdFor.get(i);
				newVertexArray[p] = vertices[i]; // getVertexId() and getColor() are not changed here yet!

				// remember the original values here, so we can use them later, eventually
				gOriginal_Colors_And_IDs[p][TReduce.POS] = vertices[i].getVertexId();
				gOriginal_Colors_And_IDs[p][TReduce.COL] = vertices[i].getColor();

				vertices[i].getVertexId() = p;
			}
		}

		//fixing up colors
		for( int c=0; c<gGraph.maxColor()+1; c++ ) {
			int newCol = newColFor.get( c );
			for( Fragment v : topSortColorToVertex[c] ) {
				v.getColor() = newCol;
			}
		}

		//apply array
		vertices = newVertexArray;
		gGraph.setVertices( newVertexArray );

		// THAT SHOULD ONLY BE USED IN CASE OF DEBUGGING
		// HAPPENS WHEN TRYING TO RENUMBER A GRAPH WITH VERTICES, THAT DOESN'T HAVE ANY IN OR OUT-GOING EDGES
		if ( !newVertexArray[0].hasTarget() ) {

			for ( Fragment v : newVertexArray )
				if ( v.hasTarget() ) {
					gGraph.setRootByForce( v );
					break;
				}
		}

		// doReCheck is true, by default, unless you call it like this: 'renumber-verts false'
		if ( shouldCheckPreconds && doReCheck ) {
			DoCheckVerticesAreTopSorted("Somehow the vertices are not top-sorted, even though renameVertices(calcInvert=true) was called!");		//HACK: We only check this if we are forward-converting, which usually corresponds to calcInvert == true.
			System.out.println(" *~* Top sort check successfully finished.");
		}

		if ( calcInvert ) {
			calcFirstVertOfSameColor();
		}
	}

	// Actually renumber vertices.
	int[][] gOriginal_Colors_And_IDs;
	private final static int POS = 0;
	private final static int COL = 1;

	/**
	 * cmd: renumber-verts
	 * BASICALLY:
	 *  - gId is the original position a vertex has had in the original graph
	 *  - getVertexId() is the place inside vertices, where it is repositioned by renumber-verts
	 *  - knowing that
	 * @param doReCheck
	 * TODO: check for correctness
	 */
	public void calcNewVertexIds( boolean doReCheck ) {

		// remember original vertex entry IDs and colors
		gOriginal_Colors_And_IDs = new int[gGraph.numberOfVertices()][2];

		// initiate
		this.newColFor = new ArrayList<Integer>( gGraph.maxColor()+1 );
		for( int i=0; i<gGraph.maxColor()+1; i++ )
			newColFor.add( -1 );


		topSortColourToColour = new BitSet[gGraph.maxColor()+1];
		for( int x=0; x<gGraph.maxColor()+1; x++ )
			topSortColourToColour[x] = new BitSet( gGraph.maxColor()+1 );

		topSortColorToVertex = new LinkedList[gGraph.maxColor()+1];
		for( int x=0; x<gGraph.maxColor()+1; x++ )
			topSortColorToVertex[x] = new LinkedList<Fragment>();

		// calculate, which color x can be reached by color y
		// remember the color each vertex has

		for(Fragment v : gGraph.getFragments() ) {

			topSortColorToVertex[v.getColor()].add( v ); // remember vertex color

			for( Loss e : v.getOutgoingEdges() ) {
				if( e == null )
					break;
				topSortColourToColour[e.getSource().getColor()].set( e.getTarget().getColor(), true );
			}
		}

		gNextColorID = gGraph.maxColor();
		gNexFragmentID = gGraph.numberOfVertices()-1;

		//initiate
		newIdFor = new ArrayList<Integer>( gGraph.numberOfVertices() );
		for( int i=0; i<gGraph.numberOfVertices(); i++)
			newIdFor.add( -1 );

		// go in
		for ( int i=0; i<gGraph.maxColor()+1; i++ )
			topSort( i );

		assert( gNextColorID == -1 ) : "Assert-ERROR: renumber-vertices. nextColorID: " + gNextColorID + " is not " + (-1);
		assert( gNexFragmentID == -1 ) : "Assert-ERROR: renumber-vertices. nexFragmentID: " + gNexFragmentID + " is not " + (-1);

		renameVertices( true, doReCheck, gRememberVertexIDs );
	}

	/**
	 * - is called when --checkPreconds is enabled
	 * - this method will terminate this program when the is at least 1 edge, that doesn't confirm the following
	 *   conditions, even though renumbering has been applied on the graph:
	 *   (i) e = edge ( u, v ) => u.ArrayIndex < v.ArrayIndex
	 *   (ii) u < v => u.color <= v.color
	 * @param errMsg : massage, that will be printed if above conditions are not fulfilled
	 * TODO: check for correctness, but probably working
	 */
	private void DoCheckVerticesAreTopSorted( java.lang.String errMsg ) {

		gTraversedVertex = new BitSet( gGraph.numberOfVertices() );
		if ( !checkVerticesAreTopSorted( gGraph.getRoot() ) ) {
			System.err.println(errMsg);
			System.exit( 1 );
		}
	}


	/**
	 * recursion to check topological sort property of edges
	 * @param v
	 * @return
	 * TODO: check for correctness, but probably working
	 */
	private boolean checkVerticesAreTopSorted( Fragment v ) {

		if( !gTraversedVertex.get( v.getVertexId() ) ) {

			gTraversedVertex.set( v.getVertexId(), true );

			for ( Loss e : v.getOutgoingEdges() ) {

				if( e == null )
					break;

				if ( e.getSource().getVertexId() >= e.getTarget().getVertexId() ) {
					System.out.println( "Edge (" + e.getSource().getVertexId() + ", " + e.getTarget().getVertexId() + ") exists from a higher-numbered vertex to a lower-numbered vertex!\n");
					return false;
				}

				// Also check colours.
				if ( e.getSource().getColor() >= e.getTarget().getColor() ) {
					System.out.println( "Edge (" + e.getSource().getVertexId() + ", " + e.getTarget().getVertexId() + ") exists from a higher-numbered colour (" + e.getSource().getColor() + ") to a lower-numbered colour (" + e.getTarget().getColor() + ")!\n" );
					return false;
				}

				return checkVerticesAreTopSorted( e.getTarget() );
			}

		}

		return true;
	}


	/*
	// The (only?) right way is to actually forget about topologically sorting vertices, and topologically sort colours instead.
	// To store these colour-edges, we *could* use a flat array plus indexes into it a la startOfEdgesTo[], but it's easier and still
	// fast enough to just use an array of set<>s.
	// Much simpler than the previous approach!  :)
	// TODO: postponed
	private void topSort2( int col ) {

		if( newColFor.get( col ) == -1 ) {

			// Haven't visited this colour yet.
			// Find the colours of all its parents (a colour x is a parent of a colour y if there is an edge from any x-coloured vertex to any y-coloured vertex)
			// and recurse to process them first.

			BitSet B = topSortColourToColour[col];
			for ( int i=0; i<gGraph.maxColor()+1; i++ ) {
				if( B.get( i ) ) // this color
					topSort2( i );
			}

			newColFor.set( col, gNextColorID-- );

			// Now renumber all vertices of this colour.  It's safe to just assign consecutive integers,
			// since we know that no two vertices of the same colour can have any edges between each other.
			if ( topSortColorToVertex[col].size() <= 1 ) {

				if ( topSortColorToVertex[col].size() > 0 ) {
					newIdFor.set( topSortColorToVertex[col].getFirst().getVertexId(), gNexFragmentID-- );  // assign new array positions
				}
			} else {

				ListIterator<Fragment> it = topSortColorToVertex[col].listIterator( topSortColorToVertex[col].size()-1 );
				Fragment v = topSortColorToVertex[col].getLast();
				do {
					newIdFor.set( v.getVertexId(), gNexFragmentID-- );  // assign new array positions
					v = it.previous();
				} while( it.hasPrevious() );
				newIdFor.set( v.getVertexId(), gNexFragmentID-- );  // i will get the last one this way
			}
		}
	}

	*/

	/**
	 * CMD: renumber-verts-2
	 * - inverted recursion
	 * - still bottom up, but the arrays will be iterated from the last to the first entry
	 * TODO: posteponed
	 public void calcNewVertexIds2( boolean doReCheck ) {

	 // remember original vertex entry IDs and colors
	 gOriginal_Colors_And_IDs = new int[gGraph.numberOfVertices()][2];

	 // initiate
	 this.newColFor = new ArrayList<Integer>( gGraph.maxColor()+1 );
	 for( int i=0; i<gGraph.maxColor()+1; i++ )
	 newColFor.add( -1 );


	 topSortColourToColour = new BitSet[gGraph.maxColor()+1];
	 for( int x=0; x<gGraph.maxColor()+1; x++ )
	 topSortColourToColour[x] = new BitSet( gGraph.maxColor()+1 );

	 topSortColorToVertex = new LinkedList[gGraph.maxColor()+1];
	 for( int x=0; x<gGraph.maxColor()+1; x++ )
	 topSortColorToVertex[x] = new LinkedList<Fragment>(  );

	 // calculate, which color x can be reached by color y
	 // remember the color each vertex has

	 for(Fragment v : gGraph.getFragments() ) {

	 topSortColorToVertex[v.getColor()].add( v ); // remember vertex color

	 List<Loss> edges = v.getOutgoingEdges();
	 edge.ite
	 for( int i= v.gTELastEntry; i>=0; i-- ) {
	 topSortColourToColour[edges[i].getSource().getColor()].set( edges[i].gTargeFragment.getColor(), true );
	 }
	 }

	 gNextColorID = gGraph.maxColor()+1-1;
	 gNexFragmentID = gGraph.numberOfVertices()-1;

	 //initiate
	 newIdFor = new ArrayList<Integer>( gGraph.numberOfVertices() );
	 for( int i=0; i<gGraph.numberOfVertices(); i++)
	 newIdFor.add( -1 );

	 // go in
	 for ( int i=0; i<gGraph.maxColor()+1; i++ )
	 topSort2( i );

	 assert( gNextColorID == -1 ) : "Assert-ERROR: renumber-vertices. nextColorID: " + gNextColorID+" is not " + (-1);
	 assert( gNexFragmentID == -1 ) : "Assert-ERROR: renumber-vertices. nexFragmentID: " + gNexFragmentID+" is not " + (-1);

	 renameVertices( true, doReCheck, gRememberVertexIDs );
	 }

	 */


	//////////////////////////////
//////                          ///////
//  //////////////////////////////   //
//////--- REDUCTION METHODS ---////////
//  //////////////////////////////   //
//////                          ///////
	//////////////////////////////


	////////////////////////////////////////
	///--- colorful subtree advantage ---///
	////////////////////////////////////////

	int reduceColSubtreeAdvantageNEdgesIgnored;
	int reduceColSubtreeAdvantageNBetterEdgesNorth;
	int reduceColSubtreeAdvantageNPositiveEdgesNorth;
	int reduceColSubtreeAdvantageNEdgesDeleted;

	/**
     * CMD: reduce-colsubtree-adv
     * @return: TRUE, if at least 1 edge has been deleted within this call
	 * TODO: check for correctness
     */
    public boolean doReduceColorsubtreeAdvantage() {

		System.out.println("><>  Reducing edges using color-subtree-advantage...");
        /*
         * Adapted from seb-vertex-ubs.  When considering a particular edge uv, instead of using "raw" edge costs for an edge xy reachable from v,
         * we subtract the weight of the best edge that we know we could add to a vertex of colour c[y].
         * ( This means we have to do a full recursion through every reachable edge, like the original (very inefficient) version
         * of seb-vertex-ubs. )
         */

        if ( gLB == null || gLB.length == 0 || gLB.length != gGraph.numberOfVertices() ) {

            // || TMain.getCMDParser().terminateCommandChain( "You need to calculate 'gLB' lower bounds ( using DEBUG-calc-anchor-lbs ) before using 'reduce-dompath'! abort." );
            return false;
        }

        List<Fragment> vertices = gGraph.getFragments();

		int nCol = gGraph.maxColor()+1; // || max color is colorcount + 1 ?
        int deleted = 0;
        reduceColSubtreeAdvantageNEdgesIgnored = 0;
        reduceColSubtreeAdvantageNPositiveEdgesNorth = 0;
		HashSet<Loss> edgesToDelete = new HashSet<Loss>( gGraph.numberOfEdges()/10 );

        for ( int i=gGraph.numberOfVertices()-1; i>=0; i-- ) {
            ColSubtreeAdvantageReductionFor( vertices.get( i ), edgesToDelete, nCol );
        }

		deleted = edgesToDelete.size();

		for ( Loss e : edgesToDelete )
			gGraph.deleteLoss( e );

		System.out.println("  ... deleted " + deleted + " new edges. Edges deleted: " + gDeletedEdges );
        return deleted > 0 ;
    }

	/**
	 * - part of 'reduce-colsubtree-adv'
	 * TODO: check for correctness
	 * @param u
	 * @return
	 */
    private void ColSubtreeAdvantageReductionFor( Fragment u, HashSet<Loss> edgesToDelete, int nCol ) {

		System.out.println(" .. for " + u );
        for ( Loss ute : u.getOutgoingEdges() ) { //u-target-edge
			if ( ute == null )
				break;

            Fragment v = ute.getTarget();
            int vi = v.getVertexId();

            SInEdgesToColor[] besLosss = new SInEdgesToColor[gGraph.maxColor()+1];
            for (int j = 0; j < besLosss.length; j++)
                besLosss[j] = new SInEdgesToColor(); // they get initiated correctly in there

            boolean[] seen = new boolean[gGraph.numberOfVertices()];

            // First, because we don't store a table containing L_{anc'}(u, v), we need to calculate that
            // on-the-fly for (u, v).  This is a special case.
            double bestCon = Double.POSITIVE_INFINITY;
            for (Loss use : u.getOutgoingEdges() ) { // u-source-edge
                if (use == null)
                    break;

                bestCon = Math.min( bestCon, gLB[vi][use.getSource().getVertexId()] );
            }

            if ( u == gGraph.getRoot() )
                bestCon = Double.NEGATIVE_INFINITY;

            // Now gather up info about the best, second-best and maxInEdge-to-the-best for all edges reachable from v,
            // given that we know u is in the solution.  This is what makes the complexity O(E^2*C)...
            colSubtreeAdvantageGatherEdges( v, u, besLosss, seen, Double.NEGATIVE_INFINITY, bestCon, nCol );

            double total = 0.0;
            if ( gShouldStrengthenSebVertexUbs )
                total = calcSebVubStrengthFor( v, besLosss );
            else {
                for ( int j=0; j<besLosss.length; j++ )
                    total += besLosss[j].besLoss.getWeight();
            }

            if ( total + ute.getWeight() < 0.0 ) {
                edgesToDelete.add( ute );
            }
        }
    }

	/**
	 * - part of 'reduce-colsubtree-adv'
	 * TODO: check correctness
	 * @param x
	 * @param u
	 * @param besLosss
	 * @param seen
	 * @param inEdgeWeight
	 * @param bestCon
	 */
	private void colSubtreeAdvantageGatherEdges ( Fragment x, Fragment u, SInEdgesToColor[] besLosss, boolean[] seen, double inEdgeWeight, double bestCon, final int nCol ) {

		System.out.println(" .. gather ( " + x.getVertexId() + " , " + u.getVertexId() + " )");
		// Even if x has already been seen, we still need to update maxInEdge for all of its outgoing edges.
		for ( Loss ex : x.getOutgoingEdges() ) {
			if ( ex == null )
				break;

			Fragment y = ex.getTarget();

			if ( !seen[x.getVertexId()] ) {

				seen[x.getVertexId()] = true;
				if ( gLB[y.getVertexId()][u.getVertexId()] < ex.getWeight() ) {
					// It's possible that (x, y) is part of some optimal (u, v)-containing solution.

					// We can either delete the edge, for a cost of -w(x, y), or we can try attaching
					// x to an ancestor of u, for a cost of lb[x][u] -- but note that in the latter case,
					// if lb[x][u] is positive, then we need to divide by a lower bound on the number of
					// paths that might force this edge in to ensure that we never wind up
					// counting it more than once in total.  If we allowed this edge to be forced in by deeper
					// edges, then a suitable lower bound would be the number of remaining colours;
					// since we currently only allow x's immediate children to force this edge in,
					// we can use either that or the number of children of x, whichever is smaller.
					// Note that the sense of cost is reversed here.
					// Also note that x == v is a special case, which we handle by calculating bestConnUb in the caller.

					if ( bestCon > 0.0 ) {
						bestCon /= Math.min( nCol - x.getColor(), x.getOutgoingEdges().size()+1 ); // x.gTELastEntry+1
						reduceColSubtreeAdvantageNPositiveEdgesNorth++;
					}

					Loss temp = new Loss( ex.getSource(), ex.getTarget() );
					temp.setWeight( Math.min( ex.getWeight(), -bestCon ) );

					if ( -bestCon < temp.getWeight() )
						reduceColSubtreeAdvantageNBetterEdgesNorth++;

					SInEdgesToColor.sebUBMergeEdge( besLosss[y.getColor()], temp, inEdgeWeight );

					// recurese to gather rest of reachable edges
					// It's always safe to connect a non-child descendant of u directly to u
					colSubtreeAdvantageGatherEdges( y, u, besLosss, seen, temp.getWeight(), gLB[y.getVertexId()][u.getVertexId()], nCol );
				} else {
					reduceColSubtreeAdvantageNEdgesIgnored++;
				}
			}

			if ( besLosss[y.getColor()].besLoss.getSource() == x )
				besLosss[y.getColor()].maxInEdge = Math.max( besLosss[y.getColor()].maxInEdge, inEdgeWeight );

		}
	}

	/////////////////////////////////////////
	///--- reduce: vertex upper bounds ---///
	/////////////////////////////////////////


    /**
     * CMD: reduce-vub
     * initiator function
     * - reduce edges by using a previously calculated ub score
     * - it terminate the program in case of getting called before ubs have been calculated
     * return: TRUE, if new edges have been deleted ( compared to prior reduction )
	 * TODO: check for correctness, probably working
     */
    public boolean reduceEdgesByVertexUpperBound() {

        System.out.println("><>  Reducing edges using vertex upper bounds...");
        if( (this.gUB == null ) || (this.gUB.length == 0) || (this.gUB.length != gGraph.numberOfVertices() ) ) {
            // || TMain.getCMDParser().terminateCommandChain( "You need to calculate upper bounds ( using tim-vertex-ubs or seb-vertex-ubs ) before using 'reduce-vertex-ubs'! abort." );
            return false;
        }

		List<Fragment> vertices = gGraph.getFragments();
        int edgesDeleted = 0;

        for( Fragment v : vertices ) {

			/*
				i mustn't use an iterator here! using an iterator will cause edges to be overseen after deleting 1 edge!
				~ edge delete <=> swaping current edge with last ^ setting last edge to NULL
			 */
            List<Loss> edges = v.getOutgoingEdges();
			ListIterator<Loss> it = edges.listIterator( 0 );
            Loss e;
            int i=0;

			while ( it.hasNext() ) {

				e = it.next();

                // check if the edge should be deleted
                if( e.getWeight() + this.gUB[e.getTarget().getVertexId()] > 0 ) {
                    // this edge will survive
                    gScaredEdge = Math.min( gScaredEdge, e.getWeight() + this.gUB[e.getTarget().getVertexId()] );
                    i++; // proceed to next edge
                } else {
                    gGraph.deleteLoss( e ); // save & fast deletion procedure
					edgesDeleted++;
					// must do that to evade iterator-access violation
					// even i points onto the last element, it.hasNext() will return true, since it didn't return
					// anything yet
					it = edges.listIterator( i );
                }
            }
        }

		// update global attributes
		gDeletedEdges += edgesDeleted;
		// || gGraph.numberOfEdges() -= edgesDeleted;  // make sure this is true!

        System.out.println("  ... Deleted " + ( edgesDeleted ) + " new Edges. Edges deleted: " + gDeletedEdges);
        System.out.println("  ... The most scared edge survives by " + gScaredEdge + ".");

		return edgesDeleted > 0;
    }

	/**
	 * CMD: reduce-slide-strong
	 * Like reduce-slide, except we require lb[][] to be populated, and we allow edges to *any* vertex of the same colour as the
	 * endpoint, and it can begin at an ancestor of the start point.
	 * TODO: check for correctness, probably working when slide methods work
	 */
	public boolean reduceWithSlideStrong() {

		System.out.println("><>  Reduce using slide-strong...");
		if ( m_ == null || m_.length <= 0 ) {
			// || TMain.getCMDParser().terminateCommandChain(" You must calculate 'm_' ( using calc-rec-slide-lbs ) to use 'reduce-slide-strong'");
			return false;
		}

		if ( gLB == null || gLB.length <= 0 ) {
			// || TMain.getCMDParser().terminateCommandChain( " You must calculate 'gLB' ( using calc-anchor-lbs or DEBUGcalc-anchor-lbs ) to use 'reduce-slide-strong' " );
			return false;
		}

		List<Fragment> vertices = gGraph.getFragments();
		int edgesDeleted = 0;

		int startZ = 0;
		for ( Fragment vert : vertices ) {

			List<Loss> edges = vert.getOutgoingEdges();
			int i = 0;
			// iterate edges of 'vert'
			while ( i <= vert.getOutgoingEdges().size() ) { // TELastEntry is updated when an edge is deleted

				Loss e = edges.get( i );
				int ui = e.getSource().getVertexId();
				Fragment v = e.getTarget();

				// Find the start of v's colour (remember all vertices of the same colour now form a contiguous block
				// for ( ; startZ < gGraph.numberOfVertices() && vertices[startZ].getColor() < v.getColor(); startZ++ );
				startZ = firstVertIDOfSameColorAs[v.getVertexId()];

				boolean bDeleted = false;
				for ( int z = startZ; z < gGraph.numberOfVertices() && vertices.get( z ).getColor() == v.getColor(); z++ ) {
					if ( gLB[z][ui] + slideLb( v.getVertexId(), z ) > e.getWeight() ) {
						gGraph.deleteLoss( e );
						edgesDeleted++;
						bDeleted = true;
						break;
					}
				}

				if ( !bDeleted )
					i++;
			}
		}

		gDeletedEdges += edgesDeleted;
		// || gGraph.numberOfEdges() -= edgesDeleted; // make

		System.out.println("  ... reduce-Slide-Strong deleted " + edgesDeleted + " new edges. Deleted edges: " + gDeletedEdges );

		return edgesDeleted > 0;
	}

    /**
     * CMD: reduce-unreach
     * reduces edges by the following principle:
     * - if there is a vertex with no source edges (inedges) that is not the root, delete its edges
     * - we can do that, basically cause we can not build up a solution starting at the root
     * TODO: check for correctness
	 */
    public boolean reduceUnreachableEdges() {

        System.out.println("><>  Reducing edges using unreachable edges...");

        if ( shouldCheckPreconds ) {
            DoCheckVerticesAreTopSorted( "Somehow the vertices are not top-sorted, even though renameVertices(calcInvert=true) was called!" );
        }

		List<Fragment> vertices = gGraph.getFragments();

        // just calculate some stuff, additionally
        int nUnreachableVertices = 0;
        int nDeletedEdges = 0;

        for  ( int i = 1; i < gGraph.numberOfVertices(); i++ ) {

            if ( !( vertices.get( i ).getIncomingEdges().size() > 0 ) && ( vertices.get( i ).getOutgoingEdges().size() > 0 ) ) {

                List<Loss> edges = vertices.get( i ).getOutgoingEdges();
                Loss e = edges.get( 0 );
                while ( e != null  ) {

                    gGraph.deleteLoss( e );
					// delete will swap the last valid edge onto the position of the current edge ( which is at pos 0 )
					// therefore, edges[0] is either valid or NULL
                    e = edges.get( 0 );
                    nDeletedEdges++;
                }

                nUnreachableVertices++;
            }
        }

		gDeletedEdges += nDeletedEdges;
		// || gGraph.numberOfEdges() -= nDeletedEdges;

        System.out.println("  ... found " + nUnreachableVertices + " vertices and deleted " + nDeletedEdges + " new edges; Edges deleted: " + gDeletedEdges );

		return nDeletedEdges > 0;
    }

	/**
	 * CMD: reduce-negpend
	 * @return: true, if at least 1 edge has been deleted
	 * TODO: check for correctness
	 */
	public boolean reduceNegativePendantEdges() {

		System.out.println("><>  Reduce using negative pendant edges...");
		List<Fragment> vertices = gGraph.getFragments();
		int EdgesDeleted = 0;

		for ( int vi = gGraph.numberOfVertices()-1; vi >= 1; vi-- ) { // we can ignore the root.

			final Fragment V = vertices.get( vi );
			if ( V.isLeaf() ) {
				// since v is a leaf, there might be negative edges
				// in that case, those edges cannot be part of the maximum solution, since they decrease the result!
				List<Loss> edges = V.getOutgoingEdges();
				int i=0;
				while ( i < V.getIncomingEdges().size() ) {

					// zero edges to not decrease the maximum, but they so not increase it either
					if ( edges.get( i ).getWeight() <= 0 ) {
						// this edge dies. Remember: we sawp edges, so do not increase i here!
						gGraph.deleteLoss( edges.get( i ) );
						EdgesDeleted++;
					} else
						i++;
				}
			}
		}

		gDeletedEdges += EdgesDeleted;
		// || gGraph.numberOfEdges() -= EdgesDeleted;

		System.out.println("  ... NegPend deleted " + EdgesDeleted + " new edges. Deleted edges: " + gDeletedEdges );

		return EdgesDeleted > 0;
	}


	/**
	 * CMD: reduce-dompath
	 * TODO: delayed, not completely working!
	 * @return
	 *
	public boolean reduceDominatingPath() {

	// check pre-conditions

	System.out.println("><>  Reducing edges using dominating path...");
	if( (this.gUB == null ) || (this.gUB.length == 0) || (this.gUB.length != gGraph.numberOfVertices() ) ) {
	// || TMain.getCMDParser().terminateCommandChain( "You need to calculate upper bounds ( using tim-vertex-ubs or seb-vertex-ubs ) before using 'reduce-dompath'! abort." );
	return false;
	}

	if ( m_ == null || m_.length == 0 || m_.length != gGraph.numberOfVertices() ) {
	// || TMain.getCMDParser().terminateCommandChain( "You need to calculate 'm_' lower bounds ( using calc-rec-slide-lbs ) before using 'reduce-dompath'! abort." );
	return false;
	}

	if ( gLB == null || gLB.length == 0 || gLB.length != gGraph.numberOfVertices() ) {
	// || TMain.getCMDParser().terminateCommandChain( "You need to calculate 'gLB' lower bounds ( using DEBUG-calc-anchor-lbs ) before using 'reduce-dompath'! abort." );
	return false;
	}

	List<Fragment> vertices = gGraph.getFragments();

	// calculate maximum weight on a source edge to a specific vertex...
	System.out.println("... Calculate max source-edges to each vertex ...");
	double[] maxInEdgeWeightTo = new double[vertices.size()];
	for ( Fragment v : vertices ) {

	if ( v.getOutgoingEdges().size() > 0 ) {

	double wBuf = v.getOutgoingEdge(0).getWeight(); // we know that there is at least 1
	for ( Loss e : v.getOutgoingEdges() ) {
	if ( e == null )
	break;

	if ( e.getWeight() > wBuf )
	wBuf = e.getWeight();
	}

	maxInEdgeWeightTo[v.getVertexId()] = wBuf;
	} else
	maxInEdgeWeightTo[v.getVertexId()] = Double.NEGATIVE_INFINITY;
	}

	// severAndSlideTo[u][v - firstVertOfSameColourAs[v]] is the maximum it could cost to delete an in-edge to any other vertex u of that colour and slide the subtree below u to being below v.
	// The indexing system drops us from O(V^2) space to O(VD), where D is the maximum number of vertices of any colour.
	// (Note: we require u and v to be of the same colour, so firstVertOfSameColourAs[v] == firstVertOfSameColourAs[u].)
	double[] severAndSlideTo = new double[vertices.size()];  // "max cost" = "min score change", and we record the latter.
	System.out.println("... Calculating max sever-and-slide costs for each vertex ...");
	for ( int v=0; v < gGraph.numberOfVertices(); v++ )
	severAndSlideTo[v] = Double.POSITIVE_INFINITY;

	for ( int v=0; v < gGraph.numberOfVertices(); v++ ) {
	for ( int u=firstVertIDOfSameColorAs[v]; u < firstVertIDOfNextColor[v]; u++ ) {
	severAndSlideTo[v] = Math.min( severAndSlideTo[v], slideLb( u, v ) - maxInEdgeWeightTo[u] );
	}
	}


	System.out.println("... Reducing edges ...");
	int edgesDeleted = 0;
	for ( Fragment y : vertices ) {

	int yi = y.getVertexId();

	// Lazily calculate dplb[a] for all a, given that y is in the solution tree.
	double[] dplb = new double[gGraph.numberOfVertices()]; // dplb[i] is the minimum cost to force vertex i into the solution, given that y is already in it
	int nextVertToProcess = firstVertIDOfNextColor[y.getVertexId()];

	int iyex = 0; // index of e from x to y
	while ( iyex <= y.getOutgoingEdges().size() ) {
	// we only get valid edges ( no NULL entries ) here! :)
	Loss yex = y.getOutgoingEdge( iyex ); // edge from y to x :)

	int x = yex.getTarget().getVertexId();              // x-vertex-index

	// Lazily calculate dplb[] up to what we need to handle x.
	for ( ; nextVertToProcess < firstVertIDOfSameColorAs[x]; nextVertToProcess++ ) {
	// calculate best connection bestConn(a , y)
	// The next line gets executed O(v^2) times
	double bestConn = gLB[nextVertToProcess][yi];
	for ( Loss ep : vertices.get( nextVertToProcess ).getOutgoingEdges() ) {
	if ( ep == null )
	break;

	// We're only allowed to try forcing in vertices that could not force y out of the graph (e.g. by removing all paths from the root to it).
	// Vertices of colours greater than c[y] is a safe set of vertices to play with.
	// (Actually that's not true -- we could try forcing in vertices of colours <= c[y] if we calculated lb[v][u] for all pairs of vertices instead of
	// only for pairs with u < v.  In fact we could even try forcing in a different vertex of colour c[y], and this would be *cheaper*
	// in general because we know which c[y]-coloured vertex is already in the graph (it's y!) -- but in this case we can still hope that all in-edges
	// to y to get killed later on anyway, which will lead to (y, x) being deleted by reduce-unreach.)
	if ( ep.getSource().getColor() > y.getColor() ) {
	bestConn = Math.max( bestConn, dplb[ep.getSource().getVertexId()] + ep.getWeight() );
	}
	}

	dplb[nextVertToProcess] = Math.min( 0.0, Math.min( bestConn, bestConn + severAndSlideTo[nextVertToProcess] ) );
	assert ( dplb[nextVertToProcess] != Double.NaN ) : "Set value to NotANumber?!";

	}

	// For each vertex z of the same color as x:
	boolean yexDeleted = false;
	assert ( vertices.get(  firstVertIDOfSameColorAs[x] ).getColor() > vertices.get( firstVertIDOfSameColorAs[x]-1 ).getColor() ) : "FirstVertIDOfSameColorAs is not correctly managed! c[x] index color: " + vertices.get( firstVertIDOfSameColorAs[x] ).getColor() + " , c[x] index -1 color: " + vertices.get( firstVertIDOfSameColorAs[x]-1 ).getColor();
	OuterLoop:
	for ( int zi=firstVertIDOfSameColorAs[x]; zi < firstVertIDOfNextColor[x]; zi++ ) {

	for ( Loss vez : vertices.get( zi ).getOutgoingEdges() ) { // save to use here. If an edge is deleted, we will jump out
	if ( vez == null )
	break;

	Fragment v = vez.getSource();
	// We're only allowed to mess with vertices that could not force y out of the graph.
	if ( v.getColor() > y.getColor() ) {

	double oc = vez.getWeight() + dplb[v.getVertexId()];
	if ( oc + slideLb( x, zi ) > yex.getWeight() ) {
	// edge yex dies
	gGraph.deleteLoss( yex );
	yexDeleted = true;
	edgesDeleted++;

	break OuterLoop; // make sure we do not ONLY break the inner, but the first outer loop to!
	}
	}
	}
	}

	if ( !yexDeleted )
	iyex++;
	// else: yex is swapped duo to the deletion-process
	}
	}

	gDeletedEdges += edgesDeleted;
	// || gGraph.numberOfEdges() -= edgesDeleted; // I should check that for accuracy!

	System.out.println( "  ... deleted " + edgesDeleted + " new edges. Edges deleted " + gDeletedEdges );

	return edgesDeleted > 0;
	}
	 */


/////
    //////////////////////////////
///////--- Unrenumber Verts ---///////
	//////////////////////////////
/////


    /**
     * CMD: unrenumber-verts
     * -  restores the original positions and colors of vertices, when renumber-verts is used
     * TODO: check for correctness
	 * TODO: postponed?
	 */
    public void unrenumberVerts() {

        if( gOriginal_Colors_And_IDs != null ) {

			List<Fragment> vertices = gGraph.getFragments();
            List<Fragment> newArray = new Fragment[vertices.size()];

            for( int v=0; v< gGraph.numberOfVertices(); v++ ) {
                vertices[v].getVertexId() = gOriginal_Colors_And_IDs[v][TReduce.POS];
                vertices[v].getColor() = gOriginal_Colors_And_IDs[v][TReduce.COL];
                newArray[vertices[v].getVertexId()] = vertices[v];
            }

            if ( shouldCheckPreconds ) {
                System.out.println(" ... unrenumber-verts: checking for empty entries or wrong positioned entries ");
                for ( int v=0; v<newArray.size(); v++ ) {
                    if ( newArray[v] == null || newArray[v].getVertexId() != v ) {
                        System.err.println(" ... unrenumber-verts has errors! pos: ( " + v + " ) ,with entry: " + newArray[v] );
                        System.exit(1);
                    }
                }
            }

            vertices = newArray;
			gGraph.setVertices( newArray );
        } else {
            System.err.println(" Use unrenumber ONLY, when you renumbered the graph. ");
            return;
        }
    }


/////
    ////////////////////////////////
///////--- CALC-IMPLIED-EDGES ---///////
	////////////////////////////////
/////


    /**
     * CMD: calc-implied-edges
     * return TRUE, if at least 1 edge has been deleted
	 * TODO: check for correctness
	 * TODO: postponed
     */
    ArrayList<Loss>[][] impliedEdges;       // impliedEdges[i][j].get[k] is the k-th descendant edge implied by edge of edge-array[i] at [j] position. Edge array i is 'targeLosss' of vertex at entry i.  Edges are not stored in any particular order.
    ArrayList<Integer>[][] colorsImpliedByEdge; // coloursImpliedByEdge[i] is a list of colours (in ascending order) that are implied by edge edgesBySource[i].
    public boolean calcReduceImpliedEdges() {

        System.out.println("Reducing edges using calc-implied-edges...");

        if ( gUB == null || gUB.length == 0 ) {
            System.out.println(" You need to calculate upper bounds using tim and/or seb vertex upper bounds scoreing method first" +
                    " to use implied edge calculation/reduction.");
            System.err.println(" Errors using implied edges.");
            return false;
        }

		List<Fragment> vertices = gGraph.getFragments();
		TTypeDependentFunctions<Integer> TypeFunction = new TTypeDependentFunctions<Integer>(); // this may help :D...
        impliedEdges = new ArrayList[gGraph.numberOfVertices()][];

        // calling this "delete" instead of "keep" will prevent initialising this to be 'true' on every entry...
        boolean[][] delete = new boolean[gGraph.numberOfVertices()][];
        LinkedList<Loss> deleteList = new LinkedList<Loss>();

		/*
		 * Part 1 - Get implied color of each leaf vertex
		 */

        // initiate delete and implied edges to use the minimum space necessary
        // Need to do this to get the single colour "implied" by each leaf vertex!
        colorsImpliedByEdge = new ArrayList[gGraph.numberOfVertices()][];
        for( int i=0; i< gGraph.numberOfVertices(); i++ ) {

            int l = vertices.get( i ).getOutgoingEdges().size(); // length of 2. dimension
            delete[i] = new boolean[l]; // 2.initiation
            impliedEdges[i] = new ArrayList[l]; // 2. initiation

            colorsImpliedByEdge[i] = new ArrayList[l];
            List<Loss> edges = vertices.get( i ).getOutgoingEdges();
			ListIterator<Loss> it = edges.listIterator(0);
			Loss e = null;

            for( int j=0; j < l; j++ ) {

				e = it.next();
                colorsImpliedByEdge[i][j] = new ArrayList<Integer>();
                colorsImpliedByEdge[i][j].add( e.getTarget().getColor() );
                impliedEdges[i][j] = new ArrayList<Loss>(  );
            }
        }

        // define some statistical variables for later analysis
        int nEdgesDeletedDueToKnownForcedEdges = 0;
        int nEdgesDeletedDueToUnknownForcedEdges = 0;
        int nEdgesDeletedDueToBetterUBs = 0;

		/*
		 * Part 2 - process edges in reversed order
		 */

        // Process edges in modified "postorder" -- i.e. when an edge (u, v) is processed, all edges (v, w) will have already been processed.
        for( int uID =  gGraph.numberOfVertices()-1; uID>=0; uID-- ) {

            if( !vertices.get( uID ).isLeaf() ) { // if the vertex is a leaf, there is nothing to find :( :)

                // we iterate through the edges here as if there are sorted by their source ( basically they are )
                List<Loss> uEdgeArray = vertices.get( uID ).getOutgoingEdges();
                int uEdgeEntry = 0; // help iterator. uID + uEdgeEntry = i
                for( Loss ue : uEdgeArray ) {
                    if( ue == null )
                        break;

                    if( ue.getWeight() < 0.0 ) {

                        // edge is negativ. It may therefore imply child edges
                        int vID = ue.getTarget().getVertexId();

						/*
						 * Part 2.1 Gather the upper bounds scores of every edge leading down from vertex v
						 */
                        double allEdgesUB = 0.0;

                        int vEdgeEntry=0; // this is an iterator index for the current edge; vID + edgeEntry = j
                        for( Loss ve : vertices.get( vID ).getOutgoingEdges() ) {
                            if ( ve == null )
                                break;

                            assert( ve.getSource().getVertexId() == vID ) : " calc-implied: sanity check failed.";
                            if ( !delete[vID][vEdgeEntry] ) { // Only process child edges we haven't already marked for deletion
                                double x = ve.getWeight() + gUB[ve.getTarget().getVertexId()];
                                // You need to run reduce-vub first, to ensure there are no such edges.
                                // IMPORTANT: It *is* OK to run some other reduction in between, since that will only (possibly) remove edges, not change this property of any surviving edges.
                                assert( x >= 0.0 );
                                allEdgesUB += x;
                            }

                            vEdgeEntry++;
                        }

						/*
						 * part 3 - reduction steps
						 * part 3.1 UB payoff implies edges
						 */

                        ArrayList<Integer> colorsImpliedBySpecificEdge = new ArrayList<Integer>();
                        colorsImpliedBySpecificEdge.add( vertices.get( vID ).getColor() ); // Note that colors are NOT ordered!
                        ArrayList<Loss> remainingEdges = new ArrayList<Loss>(); // Will eventually contain indices in edgesBySource[] of all child edges that are not forced in by this edge.
                        double forcedInEdgesUB = 0.0;		// Will eventually be the maximum that the edges that must be present could contribute

                        vEdgeEntry = 0; // i will use that entry iterator here again
                        List<Loss> vEdges = vertices.get( vID ).getOutgoingEdges();
                        for( Loss ve : vEdges ) { // j = vID + edgeEntry; more or less. [vID][edgeEntry] => [j]
                            if ( ve == null )
                                break;

                            if ( !delete[vID][vEdgeEntry] ) { // Only process child edges we haven't already marked for deletion
                                double ubWithoutThisEdge = allEdgesUB - ( ve.getWeight() + gUB[ve.getTarget().getVertexId()] );
                                assert( ubWithoutThisEdge >= 0.0 );

                                if ( ubWithoutThisEdge + ue.getWeight() < 0.0 ) {
                                    // Even adding all other child edges with their UBs doesn't recoup the cost paid by the edge to v, so this (jth) child edge is forced in.
                                    // NOTE: If edge i winds up being kept, then it must be that impliedEdges[i] does not contain any duplicate
                                    // edges -- since if it did, this would imply duplicate colours, implying that edge i would have been deleted.
                                    // (And if edge i isn't kept, we don't care what goes in impliedEdges[i] because we'll delete it anyway.)
                                    // Note that impliedEdges[i] is not sorted.

                                    ArrayList<Loss> iEu = impliedEdges[uID][uEdgeEntry]; // impliedEdges of u; access optimization

                                    // copy entries from child v
                                    iEu.add( ve );
                                    for( Loss e : impliedEdges[vID][vEdgeEntry] ) {
                                        iEu.add(e);
                                    }

                                    for( Integer I : colorsImpliedByEdge[vID][vEdgeEntry] ) {
                                        colorsImpliedBySpecificEdge.add( I );
                                    }

                                    forcedInEdgesUB += ve.getWeight() + gUB[ve.getTarget().getVertexId()];
                                } else {
                                    remainingEdges.add( ve );
                                }
                            }

                            vEdgeEntry++;
                        }


						/*
						 * Part 4 - check the colors
						 * Part 4.1 - mark every edge that implied 2 edges leading to the same color!
						 */
                        Collections.sort(colorsImpliedBySpecificEdge);
                        // this will remove blocks of dublicated entries and returns the last valid entry
                        // if there
                        int newEnd = TypeFunction.unique( colorsImpliedBySpecificEdge, 0, colorsImpliedBySpecificEdge.size()-1 );

                        if( newEnd != colorsImpliedBySpecificEdge.size()-1 ) {
                            // More than one forced-in child edge implies the same colour: this edge cannot be present.
                            delete[uID][uEdgeEntry] = true;
                            deleteList.add( ue );
                            nEdgesDeletedDueToKnownForcedEdges++;
                        } else {

							/*
							 * Part 5 - go for implied edges by the need of combinations of edges
							 */

                            // We can still consider colours that are implied by *combinations* of the remaining children.

                            // First, we can ignore any child edge that implies any colour already implied by a forced-in edge.
                            int k=0;
                            for( int i=0; i< remainingEdges.size(); i++ ) {
                                remainingEdges.set( k, remainingEdges.get( i ) );

                                Loss e = remainingEdges.get( i );
                                if( !TypeFunction.has_nonempty_intersection( colorsImpliedBySpecificEdge, colorsImpliedByEdge[e.getSource().getVertexId()][e.gSvPos] ) )
                                    k++; // no intersection, so we keep the entry
                            }


                            // TODO there must be a fast way to do this... :/
                            if( k != remainingEdges.size() ) {
                                for( int i=remainingEdges.size()-k; i>0; i-- )
                                    remainingEdges.remove( remainingEdges.size()-1 );
                            }

							/*
							 * Part 5.2 - finding implied edges based on the remaining edges
							 */

                            ArrayList<Integer> colors = new ArrayList<Integer>();
                            if( ue.getWeight() + forcedInEdgesUB < 0.0 ) {
                                // at least 1 of the remaining edges must be forced in, though we don't know which
                                if( remainingEdges.isEmpty() ) {
                                    delete[uID][uEdgeEntry] = true;
                                    deleteList.add( ue );
                                    nEdgesDeletedDueToBetterUBs++;
                                } else {
                                    // Gather, how MANY edges must be forced in
                                    // => gather the k best edges and find invalid combinations of them

									/*
									 * Part 6 - Do the tricky part
									 */

                                    Collections.sort( remainingEdges, new CEdgeUBComperator( this.gUB) );

                                    double total = 0.0;
                                    int nRemainingEdgesForcedIn = 0;
                                    for( ; nRemainingEdgesForcedIn < remainingEdges.size() && ue.getWeight() + forcedInEdgesUB + total < 0.0; nRemainingEdgesForcedIn++ ) {
                                        total += remainingEdges.get( nRemainingEdgesForcedIn ).getWeight() + gUB[remainingEdges.get( nRemainingEdgesForcedIn ).getTarget().getVertexId()];
                                    }

                                    assert( nRemainingEdgesForcedIn > 0 );

                                    // Which colours are missing in at most (nRemainingEdgesForcedIn-1) child edges?  All such colours must be implied by any solution having
                                    // at least nRemainingEdgesForcedIn child edges.
                                    int[] colorFreqs = new int[gGraph.maxColor()+1];
                                    // Adjust colour counts so that we correctly detect when there is a colour that is implied by unknown child edges and also by a known edge
                                    for( int l=0; l < colorsImpliedBySpecificEdge.size(); l++ ) {
                                        colorFreqs[colorsImpliedBySpecificEdge.get( l )] = 1;
                                    }

                                    int c = 0;
                                    for( Loss r : remainingEdges ) {
                                        if( delete[uID][uEdgeEntry] )
                                            break;

                                        vID = r.getSource().getVertexId();
                                        vEdgeEntry = r.gSvPos;

                                        for( k=0; k<colorsImpliedByEdge[vID][vEdgeEntry].size(); k++ ) {

                                            colorFreqs[colorsImpliedByEdge[vID][vEdgeEntry].get( k )]++;
                                            if( (c = colorFreqs[colorsImpliedByEdge[vID][vEdgeEntry].get( k )]) == remainingEdges.size() - nRemainingEdgesForcedIn + 1 )
                                                colors.add( colorsImpliedByEdge[vID][vEdgeEntry].get( k ) );
                                            else if( c == remainingEdges.size() - nRemainingEdgesForcedIn +2 ) {
                                                // Yo, we have a color that appears often enough to force a conflic
                                                delete[uID][uEdgeEntry] = true;
                                                deleteList.add( ue );
                                                nEdgesDeletedDueToUnknownForcedEdges++;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if( !delete[uID][uEdgeEntry] ) {
                                // We only bother keeping implied colour lists for edges we are not about to delete.
                                colorsImpliedByEdge[uID][uEdgeEntry].clear(); // Since we now populate this at the start with the endpoint's colour.
                                // The colours implied by known forced-in edges and unknown forced-in edges must be disjoint!
                                Collections.sort( colors );

                                // man, this is awful code design. It should work in O(n) time though, like merge is supposed to be...
                                Integer[] a = colors.toArray( new Integer[colors.size()] );
                                Integer[] b = colorsImpliedBySpecificEdge.toArray( new Integer[colorsImpliedBySpecificEdge.size()] );

                                colorsImpliedByEdge[uID][uEdgeEntry].addAll( Arrays.asList( TypeFunction.merge( a, b ) ) );
                            }
                        }
                    }

                    uEdgeEntry++;
                }
            }
        }

        // interesting stats area...

        // actually delete the edges!
        for( Loss e : deleteList )
            gGraph.deleteLoss( e );

        // || gGraph.numberOfEdges() -= deleteList.size();
        System.out.println(" ... Edges deleted duo to known forced edges: " + nEdgesDeletedDueToKnownForcedEdges );
        System.out.println(" ... Edges deleted duo to unknown forced edges: " + nEdgesDeletedDueToUnknownForcedEdges );
        System.out.println(" ... Edges deleted duo to better ubs: " + nEdgesDeletedDueToBetterUBs );
        System.out.println(" ... Edges deleted by calc-implied-edges: " + deleteList.size() );

        return deleteList.size() > 0;
    }


/////
    ///////////////////////////////
///////--- SLIDE LOWER BOUND ---///////
	///////////////////////////////
/////


    /*
     * runtime: O ( | edges | )
     * TODO: check for correctness
     * TODO: postponed
     */
    protected void recalculateEdgeEntryPositions() {

		List<Fragment> vertices = gGraph.getFragments();
        for ( Fragment v : vertices ) {

            int i=0;
            for ( Loss e : v.getOutgoingEdges() ) {
                if ( e == null )
                    break;

                e.gSvPos = i++; // the source of edge e is a target edge of vertex v...
            }

            i = 0;
            for ( Loss e : v.getOutgoingEdges() ) {
                if ( e == null )
                    break;

                e.gTvPos = i++; // the target of edge e is the source of vertex v ...
            }
        }

        if ( shouldCheckPreconds ) {

            for ( Fragment v : vertices ) {

                int i=0;
                for ( Loss e : v.getOutgoingEdges() ) {
                    if ( e == null )
                        break;

                    if ( e.getSource().getOutgoingEdge( i++ ) != e ) {
                        System.err.println(" ! recalculating edge entry positions failed! abort!");
                        System.exit( 1 );
                    }
                }

                i = 0;
                for ( Loss e : v.getOutgoingEdges() ) {
                    if ( e == null )
                        break;

                    if ( e.getTarget().getOutgoingEdge( i++ ) != e ) {
                        System.err.println(" ! recalculating edge entry positions failed! abort!");
                        System.exit( 1 );
                    }
                }
            }
        }
    }

    /*
     * runtime: O( |targeLosss(v)| )
     * - set the attribute 'SvPos' of every target edge in v to the correct value
     * - this will be needed when the edge array has been sorted in any way
     * TODO: check for correctness
     * TODO: postponed
     */
    protected void recalculateTargetEdgeEntryPositionsOfVertex( Fragment v ) {

        int i=0;
        for ( Loss e : v.getOutgoingEdges() )
            e.gSvPos = i++; // the source of edge e is the vertex v
    }

    /*
     * runtime: O ( | edges | ) ? probably, maybe less, thanks to java
     * TODO: check for correctness
     * TODO: postponed
     */
    protected void truncateTargetEdgeArraysOfVertices() {

		List<Fragment> vertices = gGraph.getFragments();
        System.out.println(" TRUNCATING TARGET EDGE ARRAYS! ");
        for ( Fragment v : vertices ) {
            v.truncateTargetEdgeArray();
        }

        if ( shouldCheckPreconds ) {

            // it might be possible, that the sum of array sizes doesn't fit the current amount of edges
            // in that case, i screwed up... somewhere
            int sum = 0;
            for ( Fragment v : vertices ) {
                sum += v.getOutgoingEdges().size();
            }

            if ( sum != gGraph.numberOfEdges() ) {
                System.err.println(" ! truncate edges didn't work properly! Abort! Sum: " + sum + " , edgecount: " + gGraph.numberOfEdges() );
                System.exit( 1 );
            }
        }
    }

    // Here we rely on the fact that all vertices of the same colour form a contiguous block.
    int[] firstVertIDOfSameColorAs;
    int[] firstVertIDOfNextColor;

    /**
     * Calculates 'firstVertIDOfSameColorAs' and 'firstVertIDOfNextColor'
     * - is called by 'renameVerts', when using it with 'calcInvert == true'
	 * TODO: check for correctness
	 * TODO: postponed
     */
    protected void calcFirstVertOfSameColor() {

		List<Fragment> vertices = gGraph.getFragments();
        firstVertIDOfSameColorAs = new int[gGraph.numberOfVertices()];
        firstVertIDOfNextColor = new int[gGraph.numberOfVertices()];

        int firstVert = gGraph.getRoot().getVertexId();
        for ( int i=1; i< gGraph.numberOfVertices(); i++ ) {

            if ( vertices.get( i-1 ).getColor() != vertices.get( i ).getColor() ) {

                assert ( vertices.get( i ).getColor() == vertices.get( i-1 ).getColor() + 1 ); //DEBUG: We depend on this I think...
                for ( int j = firstVert; j < i; j++ )
                    firstVertIDOfNextColor[j] = i;

                firstVert = i;
            }

            firstVertIDOfSameColorAs[i] = firstVert;
        }

		// don't forget the last vertex!
		// and yes, that vertex ID doesn't exist.
		firstVertIDOfNextColor[vertices.size()-1] = vertices.size();
    }

	/**
	 * calculates the slide-lower-bound
	 * IS SPEED UP BY TRUNCATING TARGET EDGE ARRAYS!
	 * needs 'firstVertIDOfSameColorAs' to be calculated. That is done while using 'renumber-verts', by default
	 * @param u
	 * @param v
	 * @return
	 * TODO: check for correctness
	 * TODO: postponed
	 */
    final double slideLb ( int u, int v ) {
			return m_[u][v - firstVertIDOfSameColorAs[v]];
    }

    // m_[i][j - firstVertOfSameColourAs[j]], for i and j of the same colour, is a lower bound on the score change that could result by deleting the subtree rooted at i and adding a subtree rooted at j.  Always <= 0.
    // Access it through slideLb(i, j), which does the subtraction.
    double[][] m_;				// m[i][j], for i and j of the same colour, is a lower bound on the score change that could result by deleting the subtree rooted at i and adding a subtree rooted at j.  Always <= 0.
	double[] worstSlideLb; 		// worstSlideLb[u] is the worst value of slideLb(u, v) for any v of the same colour as u -- i.e. a LB if we don't know which vertex we'll slide u to.
	double[][] lbCol;

    /**
     * CMD: calc-rec-slide-lbs
     * - it will call 'truncateTargeLossArraysOfVertices', so that each target edge array of each vertex does not have
     *   any nullpointer values anymore
     */
    public void calcRecursiveSlideLowerBound() {

		if ( firstVertIDOfSameColorAs == null || firstVertIDOfSameColorAs.length != gGraph.numberOfVertices() ) {
			System.err.println(" firstVertIDOfSameColorAs is null or changed in length. This only happens, when 'renumber-verts' is not used! \n" +
                    " OR the graph has been truncated. \n" +
					" Do only use that if you already know that your vertices are renumbered. \n" +
					" however, gonna check top sorting! ");
			DoCheckVerticesAreTopSorted( "It is not top sorted! Abort." );
			calcFirstVertOfSameColor();
		}

        if ( gUB == null || gUB.length <= 0 ) {
            System.err.println(" You must compute vertex upper bounds for using rec-slide-lbs! ");
            // || TMain.getCMDParser().bTerminateCurrentCommands = true;
            return;
        }

		if ( lbCol == null || lbCol.length <= 0 ) {
			System.err.println( " You must compute anchor to color lower bounds ( using calc-anc-col-lbs ) before using rec-slide-lbs!");
			// || TMain.getCMDParser().bTerminateCurrentCommands = true;
			return;
		}

		List<Fragment> vertices = gGraph.getFragments();
        CEdgeColorEndpointComperator comp = new CEdgeColorEndpointComperator();
        // make sure, there are no more null-edge entries!
        truncateTargetEdgeArraysOfVertices();

        ArrayList<Fragment>[] vertsOfColor = new ArrayList[gGraph.maxColor()+1];
        for ( int i=0; i<gGraph.maxColor()+1; i++ )
            vertsOfColor[i] = new ArrayList<Fragment>(  );

        for( int i=0; i< gGraph.numberOfVertices(); i++ ) {
            vertsOfColor[vertices.get( i ).getColor()].add( vertices.get( i ) );
        }

        int[] colorOrder = new int[gGraph.maxColor()+1];
        boolean[] seenColor = new boolean[gGraph.maxColor()+1];
        int c = 0;
        for ( int i = gGraph.numberOfVertices()-1; i >= 0; i-- ) {

            if ( !seenColor[ vertices.get( i ).getColor() ] ) {
                colorOrder[c++] = vertices.get( i ).getColor();
                seenColor[ vertices.get( i ).getColor() ] = true;
            }
        }

        assert ( c == gGraph.maxColor()+1 ) : " color counter c: " + c + ", should be " + (gGraph.maxColor()+1);

        // re-initialize m_
        m_ = new double[gGraph.numberOfVertices()][];
        for ( int i=0; i< gGraph.numberOfVertices(); i++ ) {

            m_[i] = new double[ vertsOfColor[ vertices.get( i ).getColor() ].size() ];
            for ( int j=0; j<m_[i].length; j++ )
                m_[i][j] = Double.NEGATIVE_INFINITY;
        }

		// re-initiate worstSlideLb
		worstSlideLb = new double[gGraph.numberOfVertices()];
		for ( int i=0; i < gGraph.numberOfVertices(); i++ )
			worstSlideLb[i] = Double.POSITIVE_INFINITY;

		// begin calculation
        for ( int iCol=0; iCol < colorOrder.length; iCol++ ) {

            int col = colorOrder[iCol];

            // First, for each vertex of this colour, sort all its out-edges by colour.  We'll undo this at the end.
            for ( Fragment u : vertsOfColor[col] ) {
                // the sorting is slightly faster duo to the decreased array sizes ( so decreasing the size & sorting is faster )
                Collections.sort( u.getOutgoingEdges(), comp );
                recalculateTargetEdgeEntryPositionsOfVertex( u );
            }

			// System.out.println( " <> Calculating recursive slide bound for the " + vertsOfColor[col].size() + " vertices of colour " + col );
            for ( Fragment u : vertsOfColor[col] ) {

				int ui = u.getVertexId();
                for ( Fragment v : vertsOfColor[col] ) {

					int vi = v.getVertexId();
                    double best = -gUB[ui];
                    if ( u == v ) {
                        best = 0.0;
                    } else {

                        List<Loss> vEdges = v.getOutgoingEdges();
						ListIterator<Loss> it = vEdges.listIterator( 0 );
						Loss lastLoss = it.next();

                        //int startIndex = 0; // start Index in vEdges!
                        double worstForColor = 0.0;
                        double total = 0.0;
                        for ( Loss ue : u.getOutgoingEdges() ) {  // vertex us edge == ue
							// ue cannot be null here; we truncated EVERY target edge array of every vertex :)

                            // Skip any v-children of irrelevant colours
                            while ( ( it.hasNext() ) && ( ue.getTarget().getColor() > lastLoss.getTarget().getColor() ) ) {
                                // startIndex++;
								lastLoss = it.next();
                            }

                            // Always try just deleting this edge and any subtree under it.
                            // We have to also consider that the edge may not be present at all, in which case the best is 0, so we take the minimum of this and 0.
                            double bestForChild = -gUB[ue.getTarget().getVertexId()];
							if ( !it.hasNext() || ue.getTarget().getColor() < lastLoss.getTarget().getColor() ) {
								// There are no more children of v to consider for the current color
							} else {
								// v has children of the same colour as this child of u.  Try all of them.
							 	bestForChild += Math.max( 0.0, lbCol[vi][lastLoss.getTarget().getColor()] );
								bestForChild = Math.max( bestForChild, lbCol[vi][lastLoss.getTarget().getColor()] + worstSlideLb[ue.getTarget().getVertexId()] );

								// it is ugly, but i need to make sure I get the last edge checked, too
								for ( ;; ) {
									if ( lastLoss.getTarget().getColor() != ue.getTarget().getColor() )
										break;

									bestForChild = Math.max( bestForChild, lastLoss.getWeight() + slideLb( ue.getTarget().getVertexId(), lastLoss.getTarget().getVertexId() ) );

									if ( it.hasNext() )
										lastLoss = it.next();
									else
										break;
								}
							}

                            // Is this the worst child for this color so far?
                            worstForColor = Math.min( worstForColor, bestForChild - ue.getWeight() );

                            // Have we finished processing this color?      || get the edge from UEs parent, that comes after UE
                            if ( ue.gSvPos == u.getOutgoingEdges().size()-1 || u.getOutgoingEdge( ue.gSvPos+1 ).getTarget().getColor() > ue.getTarget().getColor() ) {
                                // yes we have.
                                total += worstForColor;
                                worstForColor = 0.0;
                            }
                        }

                        best = ( total > best ) ? total : best; // should be faster than: Math.max ( total, best )
                    }

                    m_[ui][ vi - firstVertIDOfSameColorAs[ vi ] ] = Math.max( best, m_[ui][ vi - firstVertIDOfSameColorAs[ vi ] ] );
					worstSlideLb[ui] = Math.min( worstSlideLb[ui], m_[ui][ vi - firstVertIDOfSameColorAs[vi] ] );
                }
            }
        }
    }

	// Given a vertex u that is in the solution and a colour i that has no vertices in the solution,
	// what is the largest weight we can add to the graph by adding an edge from a vertex on the (unknown) path
	// from the root to u (including u itself as a possibility), to some vertex of colour i?  That will be stored in lbCol[u][i].
	// In theory we could even allow chains of edges, but for now, we allow only a single edge to be added,
	// making this very similar to lb[][].
	//
	// Dealing with root-connectivity is tricky.  We could try saying "lbCol[u][iCol] must be NEGINF whenever u is disconnected", but
	// that's not enough: without extra info, there's no way for u to tell whether a particular parent p is reporting
	// lbCol[p][iCol] == NEGINF for some iCol because it's connected but there's no edge from p to any iCol-coloured vertex, or because p is disconnected.
	// In the former case, we should include p in the minimum across all parents (which will therefore go to NEGINF too) but allow
	// out-edges from u to iCol-coloured vertices to increase the overall maximum, while in the latter case
	// we should not include p in the minimum across all parents (because that edge will never be part of any solution anyway), but
	// if no other connected parents exist we should force the overall maximum to NEGINF even if out-edges to iCol-coloured vertices
	// do exist.
	//
	// Therefore, although there can of course be unreachable vertices (since we never delete a vertex), we require that there are
	// no unreachable *edges*.  If such an edge (u, v) exists then the value of lbCol[u][c[v]] may be greater than NEGINF.
	//
	// This is computed from scratch each time, so there's no need to write a clear-anc-col-lbs action.

	/**
	 * CMD: calc-anc-col-lbs
	 * TODO: check for correctness
	 * TODO: postponed
	 */
	public void calcAnchorToColorLowerBounds() {

		List<Fragment> vertices = gGraph.getFragments();

		if ( shouldCheckPreconds ) {
			// assure, that reduce-unreach has been run. Meaning, that there cannot be edges without source edges but target edges!
			// remember: that is true for the root, though.
			for ( int v=1; v<gGraph.numberOfVertices(); v++ ) {
				if ( ( vertices.get( v ).getIncomingEdges().size() == 0 ) && ( vertices.get( v ).getOutgoingEdges().size() > 0 ) ) {
					System.err.println(" You MUST run reduce-unreach to calculate AnchorToColorLowerbounds!");
					// || TMain.getCMDParser().bTerminateCurrentCommands = true;
					return;
				}
			}
		}

		// initiation
		lbCol = new double[gGraph.numberOfVertices()][gGraph.maxColor()+1];
		for ( int v=0; v < gGraph.numberOfVertices(); v++ ) {
			for ( int c=0; c < gGraph.maxColor()+1; c++ ) {
				lbCol[v][c] = Double.NEGATIVE_INFINITY;
			}
		}

		// calculate
		for ( int ui=0; ui<gGraph.numberOfVertices(); ui++ ) {
			// The only unreachable vertices we handle correctly are the "directly" unreachable ones -- those having no in-edges.
			// So ensure that reduce-unreach has been called beforehand!
			Fragment uv = vertices.get( ui );
			 if ( uv.getIncomingEdges().size() > 0 ) {
				 lbCol[ui][uv.getColor()] = 0.0; // Not sure if this is ever useful but it might be here

				 // Update scores for each colour using all outgoing edges
				 for ( Loss ue : uv.getOutgoingEdges() ) {
					 if ( ue == null )
						 break;

					 lbCol[ui][ue.getTarget().getColor()] = Math.max( lbCol[ui][ue.getTarget().getColor()], ue.getWeight() );
				 }

				 // For each colour, see whether we can do better by connecting to it from some vertex on every path from the root to u.
				 for ( int c=0; c<gGraph.maxColor()+1; c++ ) {

					 double worst = Double.POSITIVE_INFINITY; // We know that worst will drop below this since we already checked that we have at least one parent.
					 for ( Loss e : uv.getOutgoingEdges() ) {
						 if ( e == null )
							 break;

						 worst = Math.min( worst, lbCol[e.getSource().getVertexId()][c] );
					 }

					 lbCol[ui][c] = Math.max( lbCol[ui][c], worst );
				 }
			 }
		}
	}


/////
    ///////////////////////////////////////
///////--- TIM - VERTEX UPPER BOUNDS ---///////
	///////////////////////////////////////
/////


    protected int gVerticesZeroUpperBoundCount = 0;
    protected int gEdgesDeletedByZeroUpperBounds = 0;
    protected double gHighestUpperBoundScore = 0.0;

    double gScaredEdge = Double.POSITIVE_INFINITY;
    int gDeletedEdges = 0;

    /**
     * CMD: tim-vertex-ubs
     * initiator function
     * - calculate the upper bound score of every vertex using tims-method
     * ~ sum up values by calculating the highest score reachable from a vertex by only using the highest edge+vertex score of 1 color,
     *   but for every color reachable from that vertex
	 *   TODO: check for correctness, but certainly working
     */
    public void doTimVertexUpperBounds() {

		List<Fragment> vertices = gGraph.getFragments();
        resizeUpperBounds( gGraph.numberOfVertices(), Double.POSITIVE_INFINITY );
        this.gTraversedVertex.clear();
        this.gVerticesZeroUpperBoundCount = 0;
        this.gEdgesDeletedByZeroUpperBounds = 0;
        this.gHighestUpperBoundScore = Double.NEGATIVE_INFINITY;
        gScaredEdge = Double.POSITIVE_INFINITY;

        for ( Fragment v : vertices )
            if ( v != null )
                timVertexUpperBound( v );
    }

    /**
     * CMD: clear-vertex-ubs
     * reinitialize upper bound array to proper values
     */
    public void clearVertexUpperBounds( double initValue ) {

        System.out.println( "Clearing upper bounds." );
        this.gUB = new double[gGraph.numberOfVertices()-1];
        for( int i = this.gUB.length-1; i > 0; i-- ) {
            this.gUB[i] = initValue;
        }
    }

    /**
     * recursive upper bound calculation based on tims-method
     * @param v: vertex to start/proceed from
	 * TODO: check for correctness, but certainly working
     */
    private void timVertexUpperBound( Fragment v ) {

        if( !this.gTraversedVertex.get( v.getVertexId() ) ) {
            // not visited yet

			/* Make sure, that every vertex 'below' / after the current vertex has a upper bound value applied */
            List<Loss> edges = v.getOutgoingEdges();

            TIntDoubleHashMap VertexColors = new TIntDoubleHashMap( (int)( gGraph.maxColor()+1*1.5 ) );

            Double buffer;

            for( Loss e : edges ) {
                if( e == null )
                    break;

                // get down to leafs/ lower vertices first
                timVertexUpperBound( e.getTarget() );

                if( ( buffer = VertexColors.get( e.getTarget().getColor() ) ) != null ) {
                    // i can kick out any higher value on a color! so there is no need to keep old values
                    VertexColors.put( e.getTarget().getColor(), Math.max( buffer, e.getWeight() + gUB[e.getTarget().getVertexId()] ) );
                } else {
                    // System.out.println(" tim vertex double buffer got NULL");
					VertexColors.put( e.getTarget().getColor(), e.getWeight() + gUB[e.getTarget().getVertexId()] );
                }
            }

            // we can sum UB's across colors
            double x = 0.0;
            for( Integer i : VertexColors.keys() ) { // iterate through every color

                // For each colour, we can choose the best UB of any child of that colour, or not to include any child
                // of this colour at all if doing so would add a negative score.
                x += Math.max( 0.0, VertexColors.get( i ) ); // they are already summed up
            }

            // Is the existing (e.g. Sebastian's) UB for this vertex better?  If so, grab it!
            x = Math.min( x, this.gUB[v.getVertexId()] );

            // Compute some interesting stats
            if( (x == 0.0) && (v.getOutgoingEdges().size() > 0) ) {
                this.gVerticesZeroUpperBoundCount++;
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }

            this.gHighestUpperBoundScore = Math.max( this.gHighestUpperBoundScore, x );
            this.gUB[v.getVertexId()] = x;

            this.gTraversedVertex.set( v.getVertexId(), true );
        }
    }


/////
    //////////////////////////////////
///////--- SEBASTIAN-VERTEX-UBS ---///////
	//////////////////////////////////
/////


    // reachableEdges[vertex.APos].get(x)[y]
    // are the vertex v's reachable edges of a vertex x ( x reachable from v, x's target edges ), the y one of those edges
    LinkedList<Loss[]>[] gReachableEdgesBy; // reachable edges by vertex at given position

    double gHighestSebUpperBoundScoreEver = Double.NEGATIVE_INFINITY;
    boolean gShouldStrengthenSebVertexUbs = false;

    /**
     * CMD: seb-vertex-ubs
     * starter function
     * - upper bound scoreing procedure for solving he maximum colorful subtree problem
	 * TODO: check for correctness
     */
    public void doSebastianVertexUpperBounds() {

        resizeUpperBounds( gGraph.numberOfVertices(), Double.POSITIVE_INFINITY );
        this.gVerticesZeroUpperBoundCount = 0;
        this.gEdgesDeletedByZeroUpperBounds = 0;
        totalStrengtheningSebUpperBound = 0.0;
        totalSneakyStrengtheningSebUpperBound = 0.0;

		if ( !SInEdgesToColor.isInitiated )
			SInEdgesToColor.initiate();

        /*
        // ~ recreate that array. That way, we don't need any other structure watching whether a vertex has been accessed
        // ~ that will get us every edge reachable for any vertex, so we don't need to care about the validation
        // ~ runtime: approximated O(nVertices), ignoring that every edge tries to access a vertex, but only nVertices
        //            are processed!
        reachableEdges = new LinkedList<Loss[]>();
        */

        if( this.gShouldStrengthenSebVertexUbs )
            strengthenedSebastianVertexUpperBounds();
        else
            sebastianVertexUpperBounds();
    }

    /**
     * processing function
     * may only be called from doSebastianVertexUpperBounds()
     * - iterates of every vertex of graph g
	 * TODO: check for correctness, but probably working
     */
    private void sebastianVertexUpperBounds() {

        // i create a new edge that is just right know to initiate null-pointers!
		List<Fragment> vertices = gGraph.getFragments();

		Fragment nonExistingVertex = new Fragment( gGraph.numberOfVertices() );
		nonExistingVertex.setColor( gGraph.maxColor()+1 +1 ); // number of colors + 1

        final Loss nullEdge = new Loss( nonExistingVertex, nonExistingVertex );
		nullEdge.setWeight( 0.0 );

        Loss[][] bestInEdge = new Loss[gGraph.numberOfVertices()][]; // i will create the 2. dimension, if v is not a leaf

        Loss[] bestColorInEdgeToV = null;
        Loss[] bestColorInEdgeToU = null;

        // for every vertex of graph g
        for( int vi =  gGraph.numberOfVertices()-1; vi >= 0; vi-- ) {

            Fragment v = vertices.get( vi );
            double x = 0.0;
            bestColorInEdgeToV = bestInEdge[vi];

            if ( bestColorInEdgeToV == null )
                // This can only mean that v is a leaf, in which case its UB is trivially 0.  Leave it empty, and work around it later.
                // x = 0.0;  already done
                ;
            else {

                if ( gShouldStrengthenSebVertexUbs ) //redundant, but i keep it here, just in case
                    //x = calcSebVubStrengthFor();
                    ;
                else {

                    // unstrengthend

                    // accumulate source edges
					/*
						in the cpp version, a struct containing the best and second best edges;
						i might have to extend it to that point too...
					 */

                    for ( int c = gGraph.maxColor()+1-1; c>v.getColor(); c-- ) {
                        x += bestColorInEdgeToV[c].getWeight();
                    }

                }
            }
            gUB[vi] = Math.min( gUB[vi], x );

            if ( ( x == 0.0 ) && (!v.isLeaf()) ) {
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }
            gHighestSebUpperBoundScoreEver = Math.max(gHighestSebUpperBoundScoreEver, x);

            // for each parent u of v, reached by using v's source edges...
            for( Loss e : v.getIncomingEdges() ) {

                if( e == null )
                    break;

                int ui = e.getSource().getVertexId();

                // lazy initiation
                if( bestInEdge[ui] == null ) {
                    // create second dimension!
                    bestInEdge[ui] = new Loss[gGraph.maxColor()+1];
                    bestColorInEdgeToU = bestInEdge[ui];

                    // i only need to initiate those entries that may be accessed
                    for( int c = gGraph.maxColor()+1-1; c > e.getSource().getColor(); c-- )
                        bestColorInEdgeToU[c] = nullEdge; // that way, there can not be null-pointer exceptions
                } else
                    bestColorInEdgeToU = bestInEdge[ui]; // faster access to array entries

                // Only bother merging in results for vertices that are not leaves. We will avoid senseless allocation that way
                if ( !v.isLeaf() ) {

                    for( int c = gGraph.maxColor()+1-1; c > v.getColor(); c-- ) {
                        //mergeeeee
                        if( bestColorInEdgeToV[c].getWeight() > bestColorInEdgeToU[c].getWeight())
                            bestColorInEdgeToU[c] = bestColorInEdgeToV[c];

                        if ( bestColorInEdgeToU[c].getTarget() == v ) {
                            // The best in-edge for colour k is actually the one from v's child u -- so update the maxInEdge for this edge.
                            // This is (the only place) where we calculate new values for maxInEdge.
                            if ( bestColorInEdgeToU[c].getWeight() <= e.getWeight() )
                                bestColorInEdgeToU[c] = e;
                        }
                    }
                }

                // Now merge in the edge (u, v) to u's info.
                if ( bestColorInEdgeToU[v.getColor()].getWeight() < e.getWeight() )
                    bestColorInEdgeToU[v.getColor()] = e;
            }

            // We have applied everything known about v to all its parents, so we no longer need v's info.
            bestInEdge[vi] = new Loss[]{nullEdge};		// Shrinks the vector by 1, effectively deleting v's info
        }

    }

    /**
     * strengtened version of sebastian vertex upper bounds
     * altough it is possible to combine both methods into 1 single method, the unstrengthend will be 5 times faster
     * duo to some access optimizations. Therefore, it is wise to let them independent
	 * TODO: check for correctness, but probably working
     */
    private void strengthenedSebastianVertexUpperBounds() {

        // i create a new edge that is just right know to initiate null-pointers!
        List<Fragment> vertices = gGraph.getFragments();

        SInEdgesToColor[][] bestInEdgeToColor = new SInEdgesToColor[gGraph.numberOfVertices()][]; // i will create the 2. dimension, if v is not a leaf

        SInEdgesToColor[] bestColorInEdgeToV = null;
        SInEdgesToColor[] bestColorInEdgeToU = null;

        // for every vertex of graph g
        for( int vi =  gGraph.numberOfVertices()-1; vi >= 0; vi-- ) {

            Fragment v = vertices.get( vi );
            double x = 0.0;
            bestColorInEdgeToV = bestInEdgeToColor[vi];

            if ( bestColorInEdgeToV == null ) {
                // This can only mean that v is a leaf, in which case its UB is trivially 0.  Leave it empty, and work around it later.
                // x = 0.0;  already done
            } else {

                if ( gShouldStrengthenSebVertexUbs )
                    x = calcSebVubStrengthFor(v, bestInEdgeToColor[vi]);
                else {

                    // unstrengthend

                    // accumulate source edges
					/*
						in the cpp version, a struct containing the best and second best edges;
						i might have to extend it to that point too...
					 */

                    for ( int c = gGraph.maxColor()+1-1; c>v.getColor(); c-- ) {
                        x += bestColorInEdgeToV[c].besLoss.getWeight();
                    }

                }
            }
            gUB[vi] = Math.min( gUB[vi], x );

            if ( ( x == 0.0 ) && (!v.isLeaf()) ) {
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }
            gHighestSebUpperBoundScoreEver = Math.max(gHighestSebUpperBoundScoreEver, x);

            // for each parent u of v, reached by using v's source edges...
            for( Loss e : v.getIncomingEdges() ) {

                if( e == null )
                    break;

                int ui = e.getSource().getVertexId();

                // lazy initiation
                if( bestInEdgeToColor[ui] == null ) {
                    // create second dimension!
                    bestInEdgeToColor[ui] = new SInEdgesToColor[gGraph.maxColor()+1];
                    bestColorInEdgeToU = bestInEdgeToColor[ui];

                    // i only need to initiate those entries that may be accessed
                    for( int c = gGraph.maxColor()+1-1; c > e.getSource().getColor(); c-- )
                        bestColorInEdgeToU[c] = new SInEdgesToColor(); // that way, there can not be null-pointer exceptions
                } else
                    bestColorInEdgeToU = bestInEdgeToColor[ui]; // faster access to array entries

                // Only bother merging in results for vertices that are not leaves. We will avoid senseless allocation that way
                if ( !v.isLeaf() ) {

                    for ( int c = gGraph.maxColor()+1-1; c > v.getColor(); c-- ) {
                        //mergeeeee
                        SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[c], bestColorInEdgeToV[c].besLoss, bestColorInEdgeToV[c].maxInEdge);
                        SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[c], bestColorInEdgeToV[c].secondBesLoss, 0.0);

                        if ( bestColorInEdgeToU[c].besLoss.getSource() == v ) {
                            // The best in-edge for colour k is actually the one from v's child u -- so update the maxInEdge for this edge.
                            // This is (the only place) where we calculate new values for maxInEdge.
                            bestColorInEdgeToU[c].maxInEdge = Math.max( bestColorInEdgeToU[c].maxInEdge, e.getWeight() );
                        }
                    }
                }

                // Now merge in the edge (u, v) to u's info.
                SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[v.getColor()], e, Double.NEGATIVE_INFINITY);
            }

            // We have applied everything known about v to all its parents, so we no longer need v's info.
            bestInEdgeToColor[vi] = new SInEdgesToColor[]{null};		// Shrinks the vector by 1, effectively deleting v's info
        }

    }

    double totalSneakyStrengtheningSebUpperBound = 0.0;
    double totalStrengtheningSebUpperBound = 0.0;

    /* Tims' strengthening of Sebastian's original bound.
    // The idea is that if the graph we have is not a subtree, then we can find a bound
    // on how many edges need to be changed, and from that, find a bound on how much we can safely reduce the score by.
    // TODO: The only way we refer to sebUbBestInEdges[] is via sebUbBestInEdges[v], so we could avoid a layer of indirection by
    // TODO: passing a ref to sebUbBestInEdges[v] directly.
    *
    *  TODO: check for correctness, but probably working
    */
    private double calcSebVubStrengthFor(Fragment v, SInEdgesToColor[] bestInEdgeToColor) {

        // calculate tentative UB ( same as unstrengthend )
        double x = 0.0;
        for ( SInEdgesToColor edge : bestInEdgeToColor ) {
            if ( edge != null ) {
                x += edge.besLoss.getWeight();
            }
        }

        // Now look for violations of the subtree property
        // this refers to Bu
        ArrayList<Integer>[] colorsFromColor = new ArrayList[gGraph.maxColor()+1];
        // initiation
        for( int i=0; i<gGraph.maxColor()+1; i++ )
            colorsFromColor[i] = new ArrayList<Integer>( gGraph.maxColor()+1 ); // to eliminate reallocation!

        for( int i=0; i<gGraph.maxColor()+1; i++ ) {
            if ( bestInEdgeToColor[i] != null && bestInEdgeToColor[i].besLoss.getSource() != SInEdgesToColor.ZERO_VERTEX ) {
                colorsFromColor[ bestInEdgeToColor[i].besLoss.getSource().getColor() ].add( i );
            }
        }

        boolean[] safeToMessWithInEdgeToColour = new boolean[gGraph.maxColor()+1];

        // We can consider all out-edges leaving a particular colour as an independent problem to fix.
        // Only need to consider colours >= v's colour.
        for( int i = v.getColor(); i < gGraph.maxColor()+1; i++ ) {
            //HACK: May be a bit expensive to allocate an nVerts-size array in this loop...
            // repairCostsFromVertex[i] is the cost to repair all edges from vertex i to some other vertex that are in the current solution.
            TIntDoubleMap repairCostsFromVertex = new TIntDoubleHashMap(  );
            double totalRepairCost = 0.0;

            for ( int j=0; j < colorsFromColor[i].size(); j++ ) {
                // Note that although this loop seems to add an nCol^2 factor to the runtime, it doesn't because the total number
                // of iterations across all values of i is just the number of colours.

                // We know that bestInEdges[coloursFromColour[i][j]].edges[1].w cannot be NEGINF because we know there is at least 1 edge to coloursFromColour[i][j], so it will be min(0.0, that).
                double cost = bestInEdgeToColor[colorsFromColor[i].get( j )].besLoss.getWeight() - bestInEdgeToColor[colorsFromColor[i].get( j )].secondBesLoss.getWeight();

                if( cost == Double.POSITIVE_INFINITY )
                    System.err.println(" positive infinity! ");

				repairCostsFromVertex.adjustOrPutValue( bestInEdgeToColor[colorsFromColor[i].get( j )].besLoss.getSource().getVertexId(), cost, cost );
                totalRepairCost += cost;
            }

            double bestRepairCost = totalRepairCost; // Corresponds to repairing *all* outgoing edges.

            // Try making each i-coloured vertex u the parent of all these edges.
            // We only bother trying the i-coloured vertices that are actually the starting point of
            // an out-edge, since inEdgeRepairCost must be >= 0 and it will cost totalRepairCost + inEdgeRepairCost
            // to move all these out-edges to some different i-coloured vertex, and we already consider
            // repairing all out-edges for a total cost of totalRepairCost.
            for ( int j=0; j < colorsFromColor[i].size(); j++ ) {
                Fragment u = bestInEdgeToColor[colorsFromColor[i].get( j )].besLoss.getSource();

                double inEdgeRepairCost;
                double maxInEdge = bestInEdgeToColor[colorsFromColor[i].get( j )].maxInEdge; //HACK: maybe get rid of this var altogether.

                if ( ( i > v.getColor() ) && ( bestInEdgeToColor[i].besLoss.getSource() == SInEdgesToColor.ZERO_VERTEX ) && ( !colorsFromColor[i].isEmpty() ) ) {
                    // We have no in-edges to this colour so far but at least one out-edge, so we can add in the best edge to this vertex (which will be negative, since otherwise it would already be present)
                    //HACK: We no longer compute this exactly because it's too expensive -- instead we just take
                    // the second-best edge to this colour, which is a UB.
                    //inEdgeRepairCost = -maxInEdgeToVertex[u];
                    //inEdgeRepairCost = -bestInEdges[c[u]][1].w;

                    inEdgeRepairCost = -maxInEdge;
                } else {
                    if ( ( i > v.getColor() ) && ( bestInEdgeToColor[i].besLoss.getSource() != SInEdgesToColor.ZERO_VERTEX ) && ( bestInEdgeToColor[i].besLoss.getTarget() != u ) && ( !colorsFromColor[i].isEmpty() ) && safeToMessWithInEdgeToColour[i] ) {

                        inEdgeRepairCost = bestInEdgeToColor[i].besLoss.getWeight() - maxInEdge;
                        totalSneakyStrengtheningSebUpperBound += inEdgeRepairCost;
                    } else {
                        inEdgeRepairCost = 0.0;
                    }
                }

                double tryUsingU = totalRepairCost - repairCostsFromVertex.get(u.getVertexId()) + inEdgeRepairCost;
                bestRepairCost = Math.min( bestRepairCost, tryUsingU );
            }

            if ( bestRepairCost == 0.0 ) { // I know, I know -- FP equality comparisons are The Devil.
                // Everything about this colour looks good.  So mark all colours we get to from its out-edges
                // as safe to mess with.  (We'll be processing these later.)
                for ( int j=0; j < colorsFromColor[i].size(); j++ ) {
                    safeToMessWithInEdgeToColour[ colorsFromColor[i].get( j )] = true;
                }
            }

            x -= bestRepairCost;
            totalStrengtheningSebUpperBound += bestRepairCost;
        }

        return x;
    }


    /**
     * resize ubs array to proper size while keeping already applied values
     * @param size: array size
     * @param val: initial value applied to new entries
	 * WORKING
     */
    private void resizeUpperBounds( int size, double val ) {

        double[] buffer = new double[size];
        if( (gUB != null) && (gUB.length > 0) ) {

            // some ubs already exists
            System.arraycopy(gUB,0, buffer,0, gUB.length );

            //initiate new values, if there are any
            if ( size > gGraph.numberOfVertices() )
                for( int i = gGraph.numberOfVertices(); i<size; i++ )
                    buffer[i] = val;
        }
        else
            for( int i=0; i<size; i++)
                buffer[i] = val;

        this.gUB = buffer;
    }


/////
    ////////////////////////
///////--- ANCHOR-LBS ---///////
	////////////////////////
/////


	/**
	 * CMD: DEBUG-calc-anchor-lbs
	 * TODO: check for correctness
	 * TODO: postponed
	 */
    public void DoDEBUGcalcAnchorLowerBounds() {

        // resize gLB to fit correct size!
        gLB = new double[gGraph.numberOfVertices()][];
        for( int i=0; i< gGraph.numberOfVertices(); i++ ) {
            gLB[i] = new double[i+1];
            for( int j=0; j<i+1; j++ )
                gLB[i][j] = Double.NEGATIVE_INFINITY;
        }

        for( int v=0; v < gGraph.numberOfVertices(); v++ ) {
            boolean[] seen = new boolean[gGraph.numberOfVertices()]; // false by default
            double[] memo = new double[gGraph.numberOfVertices()];   // 0.0 by default, doesn't really matter

            for( int u = 0; u <= v; u++ ) {
                gLB[v][u] = DEBUGanchorLowerBound( u, v, memo, seen );
            }
        }
    }

	/**
	 *
	 * @param u
	 * @param v
	 * @param memo
	 * @param seen
	 * @return
	 * TODO: check for correctness
	 * TODO: postponed
	 */
    private double DEBUGanchorLowerBound( int u, int v, double[] memo, boolean[] seen ) {

        if ( u > v )
            return Double.NEGATIVE_INFINITY;

		List<Fragment> vertices = gGraph.getFragments();
        if ( !seen[u] ) {
            // We require that there be no unreachable edges in the graph.
            //// This is assured if every vertex is either (a) the root, (b) has at least 1 in-edge, or
            //// (c) has no in-edges and no out-edges.  The following assert checks this.
            Fragment uv = vertices.get( u );
            assert( uv.getIncomingEdges().size() > 0 || ( uv.getIncomingEdges().size() == 0 && ( uv.getOutgoingEdges().size() == 0 || ( uv.getVertexId() == 0 ) ) ) );

            double x; // it will be set in any case.

            if ( v == 0 ) {
                x = 0.0;
            } else {

                double north;
                if( uv.getIncomingEdges().size() == 0 ) {
                    // This vertex has no in-edges: Either it's the root, or it has no out-edges either.
                    // Either way, there's no possibility to connect v north of it.
                    north = Double.NEGATIVE_INFINITY;
                } else {

                    // This vertex is reachable from the root and has at least 1 in-edge.
                    north = Double.POSITIVE_INFINITY; // We know there is at least 1 term in the min(), so this will come down.
                    for( Loss e : uv.getOutgoingEdges() ) {
                        if( e == null )
                            break;

                        int p = e.getSource().getVertexId();
                        double plb = DEBUGanchorLowerBound( p, v, memo, seen );
                        if( plb < north ) // this is basically like "min", just faster :)
                            north = plb;
                    }
                }

                double direct = Double.NEGATIVE_INFINITY;
                // And is there actually an edge directly from u to v?  We could also choose that.
                // We do this the hard way...
                for( Loss e : vertices.get( v ).getOutgoingEdges() ) {
                    if ( e == null )
                        break;

                    if ( e.getSource() == uv ) {
                        direct = e.getWeight();
                        break;
                    }
                }

                // If u is root-reachable and v == u, then we trivially can force v in for 0.
                if ( u == v && vertices.get( v ).getIncomingEdges().size() > 0 )
                    direct = 0.0;

                x = ( north > direct ) ? north : direct;
            }

            memo[u] = x;
            seen[u] = true;
        }

        return memo[u];
    }


/////
    ///////////////////////////////
/////// --- GETTER & SETTER --- ///////
	///////////////////////////////
/////


    public void enableSebVertexUbsStrengthening() {

        this.gShouldStrengthenSebVertexUbs = true;
    }

    public void enableMaximumSpeed() {

        this.shouldMaximizeSpeed = true;
    }

    public void toggleShouldCheckPreConditions() {

        this.shouldCheckPreconds = !shouldCheckPreconds;
    }

    public double[][] getgLB() { return gLB; }
    public void setgLB(double[][] gLB) {
        this.gLB = gLB;
    }
    public double[] getgUB() { return gUB; }
    public void setgUB(double[] gUB) { this.gUB = gUB; }

    public FGraph getgGraph() { return this.gGraph; };

    ///////////////////////
    /// --- TESTING --- ///


    public void printVertexUpperBounds() {

        System.out.println( "||---------------------------------" );
        System.out.println( "||--- Printing Vertex Upper Bounds " );

		List<Fragment> vertices = gGraph.getFragments();

        for(int i=0; i<this.gUB.length; i++) {
            System.out.println("ub["+i+"] = "+this.gUB[i]+" @ Vertex with ID: "+vertices.get( i ).getVertexId() );
        }

        System.out.println( "||--- finished " );
        System.out.println( "||---------------------------------" );
    }

	public void printLowerBounds() {

		System.out.println( "||--------------------------" );
		System.out.println( "||--- Printing Lower Bounds " );

		for ( int i=0; i<gLB.length; i++ ) {
			System.out.print( i );
			for ( int j=0; j<gLB[i].length; j++ ) {
				System.out.print( " " + gLB[i][j] );
			}
			System.out.println();
		}

		System.out.println( "||--- finished " );
		System.out.println( "||--------------------------" );
	}

	public void printm_() {

		System.out.println( "||----------------------------" );
		System.out.println( "||--- Printing lower bound m_ " );

		if ( m_ != null ) {

			String s = "";
			for ( int i=0; i<m_.length; i++ ) {

				s = "";
				for ( int j=0; j<m_[i].length; j++ ) {
					s = s + (float)( (int)(m_[i][j]*10000.0) ) / 10000.0 + " <> ";
				}
				System.out.println( " ||< " + i + " >  " + s );
			}
		} else {
			System.out.println( " m_ is null." );
		}

		System.out.println( "||--- finished " );
		System.out.println( "||---------------------------------" );
	}
}
