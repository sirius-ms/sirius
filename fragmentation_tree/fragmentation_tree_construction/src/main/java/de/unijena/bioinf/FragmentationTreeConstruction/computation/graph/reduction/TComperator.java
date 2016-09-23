package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created with IntelliJ IDEA.
 * User: B. Seelbinder
 * UID:  ga25wul
 * Date: 07.02.14
 * Time: 01:31
 * *
 */

public class TComperator {

	final static int roundgrade = 10;

    /**
     * DEBUG.
     * Can be used for upper and lower bounds, if needed. Will only be valid vor equally renumbered verts!
     * @param a
     * @param b
     * @return
     */
    protected static boolean hasSameBounds( double[] a, double[] b ) {

        System.out.println(" Testing bounds...");
        if ( a.length != b.length ) {
            LoggerFactory.getLogger(TComperator.class).error(" Bounds cannot be equal; Unequal array length! ");
            return false;
        }

        boolean bfound = false;

		int mult = (int) Math.pow( 10, roundgrade );
		for( int i=0; i<a.length; i++ ) {
            if ( (int) ( a[i] * mult ) != (int) ( b[i] * mult ) ) {
                LoggerFactory.getLogger(TComperator.class).error(" xX: " + i + ", a: " + a[i] + " , b: " + b[i]);
                bfound = true;
            }
        }

        return !bfound;
    }

    /**
     * DEBUG.
     * Can be used for upper and lower bounds, if needed. Will only be valid vor equally renumbered verts!
     * @param a
     * @param b
     * @return
     */
    protected static boolean hasSameBounds( double[][] a, double[][] b ) {

        System.out.println(" Testing bounds...");
        if ( a.length != b.length ) {
            LoggerFactory.getLogger(TComperator.class).error(" ERROR. Bounds cannot be equal; Unequal array length! ");
            return false;
        }

        boolean bfound = false;

		int mult = (int) Math.pow( 10, roundgrade );
        for( int i=0; i<a.length; i++ ) {
            for( int j=0; j < a[i].length; j++ ) {

                if ( (int) ( a[i][j] * mult ) != (int) ( b[i][j] * mult ) ) {
                    LoggerFactory.getLogger(TComperator.class).error(" xX: ( " + i + " , " + j + " ) , a: " + a[i] + " , b: " + b[i]);
                    bfound = true;
                }
            }
        }

        return !bfound;
    }

    /**
     * DEBUG. Used to check, if unrenumber is working, but it can be used on every method that may changes edge directions
     *        Even so, this will probably the only case
     * @param g1 : reduced/changed graph
     * @param ori : original graph
     * @return
     */
    public static boolean hasSameEdges( FGraph g1, FGraph ori ) {

        if ( g1 != null || ori != null || g1.getFragments().size() == ori.getFragments().size() ) {

            System.out.println(" ~~~ Checking g1 for unequal edges in g2... ");

            ArrayList<Loss> wrongUnrenumbered = new ArrayList<Loss>();
            ArrayList<Double> wrongValue = new ArrayList<Double>();
            double[][] VertexToVertex = new double[g1.getFragments().size()][g1.getFragments().size()];

            // secure initialize
            for( int x = 0; x < g1.getFragments().size(); x++ ) {
                for( int y = 0; y < ori.getFragments().size(); y++ ) {
                    VertexToVertex[x][y] = Double.NEGATIVE_INFINITY;
                }
            }

            // access optimization.
            for( Fragment v : ori.getFragments() ) {
                for( Loss e : v.getOutgoingEdges() ) {
                    if( e == null )
                        break;

                    VertexToVertex[v.getVertexId()][e.getTarget().getVertexId()] = e.getWeight();
                }
            }

            for( Fragment v : g1.getFragments() ) {
                for( Loss e : v.getOutgoingEdges() ) {
                    if( e.getWeight() != VertexToVertex[e.getSource().getVertexId()][e.getTarget().getVertexId()] ) {
                        wrongUnrenumbered.add(e);
                        wrongValue.add(VertexToVertex[e.getSource().getVertexId()][e.getTarget().getVertexId()]);
                    }
                }
            }

            if ( wrongUnrenumbered.size() > 0 ) {

                Iterator<Double> dit = wrongValue.listIterator(0);
                for( Loss e : wrongUnrenumbered ) {
                    System.out.println(" * " + e + " | " + dit.next() );
                }
                LoggerFactory.getLogger(TComperator.class).error(" ... Unequal edges from g1 in g2");
                return false;
            } else {
                LoggerFactory.getLogger(TComperator.class).error(" ... Equl edges from g1 in g2!");
                return true;
            }

        } else {
            LoggerFactory.getLogger(TComperator.class).error(" cannot validate correct renumbering: graph NULL or invalid vertex count!");
            return false;
        }
    }


    public static boolean compareLowerBounds( double[] lb1, double[] lb2 ) {

        if( lb1.length != lb2.length ) {
            LoggerFactory.getLogger(TComperator.class).error("The lower bound tables should have the same size to be compared!");
            return false;
        }

        return hasSameBounds( lb1, lb2 );
    }

    public static boolean compareLowerBounds( TReduce g1, TReduce g2 ) {

        if( g1.getLB().length != g2.getLB().length ) {
            LoggerFactory.getLogger(TComperator.class).error("The lower bound tables should have the same size to be compared!");
            return false;
        }

        return hasSameBounds( g1.getLB(), g2.getLB() );
    }

    public static boolean compareUpperBounds( TReduce g1, TReduce g2 ) {

        return compareSingleDoubleTable( g1.getUB(), g2.getUB() );
    }

	public static boolean compareSingleDoubleTable( double[] ub1, double[] ub2 ) {

		if( ub1.length != ub2.length ) {
            LoggerFactory.getLogger(TComperator.class).error("The upper bound tables should have the same size to be compared!");
			return false;
		}

		LinkedList<Integer> ubId = new LinkedList<Integer>(  );
		LinkedList<Point2D.Double> ubSc = new LinkedList<Point2D.Double>(  );

		int mult = (int) Math.pow( 10, roundgrade );

		for( int i=0; i<ub1.length; i++ ) {

			if ( (int) ( ub1[i] * mult ) != (int) ( ub2[i] * mult ) ) {
				// here is some difference
				ubId.add( i );
				ubSc.add( new Point2D.Double( ub1[i], ub2[i] ) );
			}
		}

		if ( ubId.size() > 0 ) {

			System.out.println(" --- Found some ubs differences ---");
			ListIterator<Point2D.Double> secIterator = ubSc.listIterator( 0 );
			Point2D.Double ub = ubSc.getFirst();
			for ( Integer I : ubId ) {

				System.out.println("- [" + I + "] " + ub.x + " , " + ub.y );
				ub = secIterator.next();
			}

			return false;
		} else {
			System.out.println(" --- Identical upper bounds! ---");
		}

		return true;
	}

	/**
	 * compare the graph in File1 with File2
	 * - returns TRUE, if there are asynchronous edges
	 */
	public static boolean compareGraphsForMissingEdges( double[][] EdgesAtVertex1, double[][] EdgesAtVertex2 ) {

		LinkedList<Point> MissingEdgesIn1 = new LinkedList<Point>(  );
		LinkedList<Point> MissingEdgesIn2 = new LinkedList<Point>(  );
		LinkedList<Point> DifferedWeightEdges = new LinkedList<Point>(  );

		int mult = (int) Math.pow( 10, roundgrade );

		// this currently is expansive to allocate. but it will do fast, i guess
		for( int u=0; u<EdgesAtVertex1.length; u++ ) {

			double[] weightRowOf1 = EdgesAtVertex1[u];
			double[] weightRowOf2 = EdgesAtVertex2[u];

			for( int v=0; v<EdgesAtVertex1[u].length; v++ ) {

				if ( weightRowOf1[v] != Double.NEGATIVE_INFINITY ) {

					if ( weightRowOf2[v] != Double.NEGATIVE_INFINITY ) {

						if ( (int) ( weightRowOf1[v]*mult) != (int)(weightRowOf2[v]*mult) ) {
							// both are set and they are unequal in weight?!
							DifferedWeightEdges.add( new Point( u, v ) );
						} // else: edges are OK
					} else {

						// edge of graph 2 doesn't exist!
						MissingEdgesIn2.add( new Point( u, v ) );
					}
				} else {

					if ( weightRowOf2[v] != Double.NEGATIVE_INFINITY ) {

						// edges doesn't exist in graph 1!
						MissingEdgesIn1.add( new Point( u, v ) );
					} // else: edge does not exist!
				}
			}
		}

		if ( MissingEdgesIn1.size() > 0 ) {
			System.out.println(" Edges missing in graph 1" );

			for( Point p : MissingEdgesIn1 ) {
				System.out.println( " * ( " + p.x + " , " + p.y + " ) : " + EdgesAtVertex2[p.x][p.y] );
			}
		}

		if ( MissingEdgesIn2.size() > 0 ) {
			System.out.println(" Edges missing in graph 2" );

			for( Point p : MissingEdgesIn2 ) {
				System.out.println( " * ( " + p.x + " , " + p.y + " ) : " + EdgesAtVertex1[p.x][p.y] );
			}
		}

		if ( DifferedWeightEdges.size() > 0 ) {
			System.out.println(" Edges with different weights: " );

			for( Point p : DifferedWeightEdges ) {
				System.out.println( " * ( " + p.x + " , " + p.y + " ) w1: " + EdgesAtVertex1[p.x][p.y] + " | w2: " + EdgesAtVertex2[p.x][p.y] );
			}
		}

		// return FALSE, if both graphs are equal in everything
		return !( MissingEdgesIn1.size() == 0 && MissingEdgesIn2.size() == 0 && DifferedWeightEdges.size() == 0 );
	}
}
