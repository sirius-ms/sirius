/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.reduction;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.*;

/**
 * Created by Xentrics on 30.03.14.
 */
public class TReduce {

    final protected FGraph gGraph;
    final protected int VertexColorCount;


    // traverse related
    protected BitSet gTraversedVertex;

    // bounds
    protected double[] gUB; // global upper bounds for vertices
    protected double[][] gLB; // global lower bounds for vertices

    ////////////////////////////
    /// --- CONSTRUCTORS --- ///

    /**
     * - creates instance of TReduce and top-sorts vertices, internally
     *
     * @param G
     */
    public TReduce(final FGraph G) {

        gTraversedVertex = new BitSet(G.numberOfVertices());
        gGraph = G;
        VertexColorCount = G.maxColor() + 1;
        // we assume, we get top sorted input!
    }


/////
    ///////////////////////////////
///////--- RENUMBER VERTICES ---///////
    ///////////////////////////////
////

    /**
     * - is called when --checkPreconds is enabled
     * - this method will terminate this program when the is at least 1 edge, that doesn't confirm the following
     * conditions, even though renumbering has been applied on the graph:
     * (i) e = edge ( u, v ) {@literal =>} u.ArrayIndex {@literal <} v.ArrayIndex
     * (ii) u {@literal <} v {@literal =>} u.color {@literal <=} v.color
     *
     * @param errMsg : massage, that will be printed if above conditions are not fulfilled
     *               TODO: check for correctness, but probably working, rewritten
     */
    protected void DoCheckVerticesAreTopSorted(java.lang.String errMsg) {

        gTraversedVertex = new BitSet(gGraph.numberOfVertices());
        if (!checkVerticesAreTopSorted(gGraph.getRoot())) {
            System.err.println(errMsg);
            System.exit(1);
        }
    }


    /**
     * recursion to check topological sort property of edges
     *
     * @param v
     * @return TODO: check for correctness, but probably working, rewritten
     */
    private boolean checkVerticesAreTopSorted(Fragment v) {

        if (!gTraversedVertex.get(v.getVertexId())) {

            gTraversedVertex.set(v.getVertexId(), true);

            for (Loss e : v.getOutgoingEdges()) {

                if (e == null)
                    break;

                if (e.getSource().getVertexId() >= e.getTarget().getVertexId()) {
                    System.out.println("Edge (" + e.getSource().getVertexId() + ", " + e.getTarget().getVertexId() + ") exists from a higher-numbered vertex to a lower-numbered vertex!\n");
                    return false;
                }

                // Also check colours.
                if (e.getSource().getColor() >= e.getTarget().getColor()) {
                    System.out.println("Edge (" + e.getSource().getVertexId() + ", " + e.getTarget().getVertexId() + ") exists from a higher-numbered colour (" + e.getSource().getColor() + ") to a lower-numbered colour (" + e.getTarget().getColor() + ")!\n");
                    return false;
                }

                return checkVerticesAreTopSorted(e.getTarget());
            }

        }

        return true;
    }

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

    /**
     * CMD: reduce-colsubtree-adv
     *
     * @return: TRUE, if at least 1 edge has been deleted within this call
     * TODO: check for correctness
     */
    public boolean doReduceColorsubtreeAdvantage() {

        if (gLB == null || gLB.length == 0 || gLB.length != gGraph.numberOfVertices()) {

            // || TMain.getCMDParser().terminateCommandChain( "You need to calculate 'gLB' lower bounds ( using DEBUG-calc-anchor-lbs ) before using 'reduce-dompath'! abort." );
            return false;
        }


        int nCol = this.VertexColorCount; // || max color is colorcount + 1 ?
        int deleted = 0;
        HashSet<Loss> edgesToDelete = new HashSet<Loss>(gGraph.numberOfEdges() / 10);

        for (int i = gGraph.numberOfVertices() - 1; i >= 0; i--) {
            ColSubtreeAdvantageReductionFor(gGraph.getFragmentAt(i), edgesToDelete, nCol);
        }

        deleted = edgesToDelete.size();

        for (Loss e : edgesToDelete)
            gGraph.deleteLoss(e);

        return deleted > 0;
    }


    /**
     * - part of 'reduce-colsubtree-adv'
     * TODO: check for correctness
     *
     * @param u
     * @return
     */
    private void ColSubtreeAdvantageReductionFor(Fragment u, HashSet<Loss> edgesToDelete, int nCol) {

        for (Loss ute : u.getOutgoingEdges()) { //u-target-edge
            if (ute == null)
                break;

            Fragment v = ute.getTarget();
            int vi = v.getVertexId();

            SInEdgesToColor[] besLosss = new SInEdgesToColor[this.VertexColorCount];
            for (int j = 0; j < besLosss.length; j++)
                besLosss[j] = new SInEdgesToColor(); // they get initiated correctly in there

            boolean[] seen = new boolean[gGraph.numberOfVertices()];

            double bestCon = Double.POSITIVE_INFINITY;
            for (Loss use : u.getOutgoingEdges()) { // u-source-edge
                if (use == null)
                    break;

                bestCon = Math.min(bestCon, gLB[vi][use.getSource().getVertexId()]);
            }

            if (u == gGraph.getRoot())
                bestCon = Double.NEGATIVE_INFINITY;

            colSubtreeAdvantageGatherEdges(v, u, besLosss, seen, Double.NEGATIVE_INFINITY, bestCon, nCol);

            double total = 0.0;
            if (gShouldStrengthenSebVertexUbs)
                total = calcSebVubStrengthFor(v, besLosss);
            else {
                for (int j = 0; j < besLosss.length; j++)
                    total += besLosss[j].besLoss.getWeight();
            }

            if (total + ute.getWeight() < 0.0) {
                edgesToDelete.add(ute);
            }
        }
    }


    /**
     * - part of 'reduce-colsubtree-adv'
     * TODO: check correctness, rewritten
     *
     * @param x
     * @param u
     * @param besLosss
     * @param seen
     * @param inEdgeWeight
     * @param bestCon
     */
    private void colSubtreeAdvantageGatherEdges(Fragment x, Fragment u, SInEdgesToColor[] besLosss, boolean[] seen, double inEdgeWeight, double bestCon, final int nCol) {

        for (Loss ex : x.getOutgoingEdges()) {
            if (ex == null)
                break;

            Fragment y = ex.getTarget();

            if (!seen[x.getVertexId()]) {

                seen[x.getVertexId()] = true;
                if (gLB[y.getVertexId()][u.getVertexId()] < ex.getWeight()) {

                    if (bestCon > 0.0) {
                        bestCon /= Math.min(nCol - x.getColor(), x.getOutgoingEdges().size() + 1); // x.gTELastEntry+1
                    }

                    Loss temp = new Loss(ex.getSource(), ex.getTarget());
                    temp.setWeight(Math.min(ex.getWeight(), -bestCon));

                    SInEdgesToColor.sebUBMergeEdge(besLosss[y.getColor()], temp, inEdgeWeight);

                    colSubtreeAdvantageGatherEdges(y, u, besLosss, seen, temp.getWeight(), gLB[y.getVertexId()][u.getVertexId()], nCol);
                }
            }

            if (besLosss[y.getColor()].besLoss.getSource() == x)
                besLosss[y.getColor()].maxInEdge = Math.max(besLosss[y.getColor()].maxInEdge, inEdgeWeight);

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
     * TODO: check for correctness, probably working, rewritten
     */
    public boolean reduceEdgesByVertexUpperBound() {

        if ((this.gUB == null) || (this.gUB.length == 0) || (this.gUB.length != gGraph.numberOfVertices())) {
            return false;
        }

        int edgesDeleted = 0;

        for (Fragment v : gGraph.getFragments()) {

            List<Loss> edges = v.getOutgoingEdges();
            ListIterator<Loss> it = edges.listIterator(0);
            Loss e;
            int i = 0;

            while (it.hasNext()) {

                e = it.next();

                // check if the edge should be deleted
                if (e.getWeight() + this.gUB[e.getTarget().getVertexId()] > 0) {
                    // this edge will survive
                    gScaredEdge = Math.min(gScaredEdge, e.getWeight() + this.gUB[e.getTarget().getVertexId()]);
                    i++; // proceed to next edge
                } else {
                    gGraph.deleteLoss(e); // save & fast deletion procedure
                    edgesDeleted++;
                    // must do that to evade iterator-access violation
                    // even i points onto the last element, it.hasNext() will return true, since it didn't return
                    // anything yet
                    it = edges.listIterator(i);
                }
            }
        }

        gDeletedEdges += edgesDeleted;

        return edgesDeleted > 0;
    }


    /**
     * CMD: reduce-slide-strong
     * Like reduce-slide, except we require lb[][] to be populated, and we allow edges to *any* vertex of the same colour as the
     * endpoint, and it can begin at an ancestor of the start point.
     * TODO: check for correctness, probably working when slide methods work, rewritten
     */
    public boolean reduceWithSlideStrong() {

        if (m_ == null || m_.length <= 0) {
            return false;
        }

        if (gLB == null || gLB.length <= 0)
            return false;

        int edgesDeleted = 0;

        int startZ = 0;
        for (Fragment vert : gGraph.getFragments()) {

            List<Loss> edges = vert.getOutgoingEdges();
            int i = 0;
            // iterate edges of 'vert'
            while (i <= vert.getOutgoingEdges().size()) { // TELastEntry is updated when an edge is deleted

                Loss e = edges.get(i);
                int ui = e.getSource().getVertexId();
                Fragment v = e.getTarget();

                // Find the start of v's colour (remember all vertices of the same colour now form a contiguous block
                startZ = firstVertIDOfSameColorAs[v.getVertexId()];

                boolean bDeleted = false;
                for (int z = startZ; z < gGraph.numberOfVertices() && gGraph.getFragmentAt(z).getColor() == v.getColor(); z++) {
                    if (gLB[z][ui] + slideLb(v.getVertexId(), z) > e.getWeight()) {
                        gGraph.deleteLoss(e);
                        edgesDeleted++;
                        bDeleted = true;
                        break;
                    }
                }

                if (!bDeleted)
                    i++;
            }
        }

        gDeletedEdges += edgesDeleted;

        return edgesDeleted > 0;
    }


    /**
     * CMD: reduce-unreach
     * reduces edges by the following principle:
     * - if there is a vertex with no source edges (inedges) that is not the root, delete its edges
     * - we can do that, basically cause we can not build up a solution starting at the root
     * TODO: check for correctness, rewritten
     */
    public boolean reduceUnreachableEdges() {

        // CRITICAL: THIS WILL BREAK THE GRAPH, IF IT IS NOT TOP SORTED!

        int nDeletedEdges = 0;

        for (int i = 1; i < gGraph.numberOfVertices(); i++) {

            if (!(gGraph.getFragmentAt(i).getIncomingEdges().size() > 0) && (gGraph.getFragmentAt(i).getOutgoingEdges().size() > 0)) {

                List<Loss> edges = gGraph.getFragmentAt(i).getOutgoingEdges();
                Loss e = edges.get(0);
                while (e != null) {

                    gGraph.deleteLoss(e);
                    e = edges.get(0);
                    nDeletedEdges++;
                }
            }
        }

        gDeletedEdges += nDeletedEdges;

        return nDeletedEdges > 0;
    }


    /**
     * CMD: reduce-negpend
     *
     * @return: true, if at least 1 edge has been deleted
     * TODO: check for correctness, rewritten
     */
    public boolean reduceNegativePendantEdges() {

        int EdgesDeleted = 0;

        for (int vi = gGraph.numberOfVertices() - 1; vi >= 1; vi--) { // we can ignore the root.

            final Fragment V = gGraph.getFragmentAt(vi);
            if (V.isLeaf()) {
                // since v is a leaf, there might be negative edges
                // in that case, those edges cannot be part of the maximum solution, since they decrease the result!
                List<Loss> edges = V.getOutgoingEdges();
                int i = 0;
                while (i < V.getIncomingEdges().size()) {

                    // zero edges to not decrease the maximum, but they so not increase it either
                    if (edges.get(i).getWeight() <= 0) {
                        // this edge dies. Remember: we sawp edges, so do not increase i here!
                        gGraph.deleteLoss(edges.get(i));
                        EdgesDeleted++;
                    } else
                        i++;
                }
            }
        }

        gDeletedEdges += EdgesDeleted;

        return EdgesDeleted > 0;
    }


    // Here we rely on the fact that all vertices of the same colour form a contiguous block.
    int[] firstVertIDOfSameColorAs;

    /**
     * calculates the slide-lower-bound
     * IS SPEED UP BY TRUNCATING TARGET EDGE ARRAYS!
     * needs 'firstVertIDOfSameColorAs' to be calculated. That is done while using 'renumber-verts', by default
     *
     * @param u
     * @param v
     * @return TODO: check for correctness, rewritten
     * TODO: postponed
     */
    final double slideLb(int u, int v) {
        return m_[u][v - firstVertIDOfSameColorAs[v]];
    }

    double[][] m_;                // m[i][j], for i and j of the same colour, is a lower bound on the score change that could result by deleting the subtree rooted at i and adding a subtree rooted at j.  Always <= 0.
    double[][] lbCol;

    /**
     * CMD: calc-anc-col-lbs
     * TODO: check for correctness, rewritten
     * TODO: postponed
     */
    public void calcAnchorToColorLowerBounds() {

        // initiation
        lbCol = new double[gGraph.numberOfVertices()][this.VertexColorCount];
        for (int v = 0; v < gGraph.numberOfVertices(); v++) {
            for (int c = 0; c < this.VertexColorCount; c++) {
                lbCol[v][c] = Double.NEGATIVE_INFINITY;
            }
        }

        // calculate
        for (int ui = 0; ui < gGraph.numberOfVertices(); ui++) {
            // The only unreachable vertices we handle correctly are the "directly" unreachable ones -- those having no in-edges.
            // So ensure that reduce-unreach has been called beforehand!
            Fragment uv = gGraph.getFragmentAt(ui);
            if (uv.getIncomingEdges().size() > 0) {
                lbCol[ui][uv.getColor()] = 0.0; // Not sure if this is ever useful but it might be here

                // Update scores for each colour using all outgoing edges
                for (Loss ue : uv.getOutgoingEdges()) {
                    if (ue == null)
                        break;

                    lbCol[ui][ue.getTarget().getColor()] = Math.max(lbCol[ui][ue.getTarget().getColor()], ue.getWeight());
                }

                // For each colour, see whether we can do better by connecting to it from some vertex on every path from the root to u.
                for (int c = 0; c < this.VertexColorCount; c++) {

                    double worst = Double.POSITIVE_INFINITY; // We know that worst will drop below this since we already checked that we have at least one parent.
                    for (Loss e : uv.getOutgoingEdges()) {
                        if (e == null)
                            break;

                        worst = Math.min(worst, lbCol[e.getSource().getVertexId()][c]);
                    }

                    lbCol[ui][c] = Math.max(lbCol[ui][c], worst);
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
     * but for every color reachable from that vertex
     * TODO: check for correctness, but certainly working, rewritten
     */
    public void doTimVertexUpperBounds() {

        resizeUpperBounds(gGraph.numberOfVertices(), Double.POSITIVE_INFINITY);
        this.gTraversedVertex.clear();
        this.gVerticesZeroUpperBoundCount = 0;
        this.gEdgesDeletedByZeroUpperBounds = 0;
        this.gHighestUpperBoundScore = Double.NEGATIVE_INFINITY;
        gScaredEdge = Double.POSITIVE_INFINITY;

        for (Fragment v : gGraph.getFragments())
            if (v != null)
                timVertexUpperBound(v);
    }

    /**
     * CMD: clear-vertex-ubs
     * reinitialize upper bound array to proper values
     */
    public void clearVertexUpperBounds(double initValue) {

        this.gUB = new double[gGraph.numberOfVertices() - 1];
        for (int i = this.gUB.length - 1; i > 0; i--) {
            this.gUB[i] = initValue;
        }
    }

    /**
     * recursive upper bound calculation based on tims-method
     *
     * @param v: vertex to start/proceed from
     *           TODO: check for correctness, but certainly working, rewritten
     */
    private void timVertexUpperBound(Fragment v) {

        if (!this.gTraversedVertex.get(v.getVertexId())) {
            // not visited yet

			/* Make sure, that every vertex 'below' / after the current vertex has a upper bound value applied */
            List<Loss> edges = v.getOutgoingEdges();

            TIntDoubleHashMap VertexColors = new TIntDoubleHashMap((int) (this.VertexColorCount * 1.5));

            Double buffer;

            for (Loss e : edges) {
                if (e == null)
                    break;

                // get down to leafs/ lower vertices first
                timVertexUpperBound(e.getTarget());

                if ((buffer = VertexColors.get(e.getTarget().getColor())) != null) {
                    VertexColors.put(e.getTarget().getColor(), Math.max(buffer, e.getWeight() + gUB[e.getTarget().getVertexId()]));
                } else {
                    VertexColors.put(e.getTarget().getColor(), e.getWeight() + gUB[e.getTarget().getVertexId()]);
                }
            }

            // we can sum UB's across colors
            double x = 0.0;
            for (Integer i : VertexColors.keys()) { // iterate through every color

                // For each colour, we can choose the best UB of any child of that colour, or not to include any child
                // of this colour at all if doing so would add a negative score.
                x += Math.max(0.0, VertexColors.get(i)); // they are already summed up
            }

            // Is the existing (e.g. Sebastian's) UB for this vertex better?  If so, grab it!
            x = Math.min(x, this.gUB[v.getVertexId()]);

            // Compute some interesting stats
            if ((x == 0.0) && (v.getOutgoingEdges().size() > 0)) {
                this.gVerticesZeroUpperBoundCount++;
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }

            this.gHighestUpperBoundScore = Math.max(this.gHighestUpperBoundScore, x);
            this.gUB[v.getVertexId()] = x;

            this.gTraversedVertex.set(v.getVertexId(), true);
        }
    }


/////
    //////////////////////////////////
///////--- SEBASTIAN-VERTEX-UBS ---///////
    //////////////////////////////////
/////

    double gHighestSebUpperBoundScoreEver = Double.NEGATIVE_INFINITY;
    boolean gShouldStrengthenSebVertexUbs = false;

    /**
     * CMD: seb-vertex-ubs
     * starter function
     * - upper bound scoreing procedure for solving he maximum colorful subtree problem
     * TODO: check for correctness, rewritten
     */
    public void doSebastianVertexUpperBounds() {

        resizeUpperBounds(gGraph.numberOfVertices(), Double.POSITIVE_INFINITY);
        this.gVerticesZeroUpperBoundCount = 0;
        this.gEdgesDeletedByZeroUpperBounds = 0;

        if (!SInEdgesToColor.isInitiated)
            SInEdgesToColor.initiate();

        if (this.gShouldStrengthenSebVertexUbs)
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
        Fragment nonExistingVertex = new Fragment(gGraph.numberOfVertices());
        nonExistingVertex.setColor(this.VertexColorCount + 1); // number of colors + 1

        final Loss nullEdge = new Loss(nonExistingVertex, nonExistingVertex);
        nullEdge.setWeight(0.0);

        Loss[][] bestInEdge = new Loss[gGraph.numberOfVertices()][]; // i will create the 2. dimension, if v is not a leaf

        Loss[] bestColorInEdgeToV = null;
        Loss[] bestColorInEdgeToU = null;

        // for every vertex of graph g
        for (int vi = gGraph.numberOfVertices() - 1; vi >= 0; vi--) {

            Fragment v = gGraph.getFragmentAt(vi);
            double x = 0.0;
            bestColorInEdgeToV = bestInEdge[vi];

            if (bestColorInEdgeToV == null)
                // This can only mean that v is a leaf, in which case its UB is trivially 0.  Leave it empty, and work around it later.
                // x = 0.0;  already done
                ;
            else {

                // UNSTRENGTHENED VERSION
                for (int c = this.VertexColorCount - 1; c > v.getColor(); c--) {
                    x += bestColorInEdgeToV[c].getWeight();
                }
            }
            gUB[vi] = Math.min(gUB[vi], x);

            if ((x == 0.0) && (!v.isLeaf())) {
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }
            gHighestSebUpperBoundScoreEver = Math.max(gHighestSebUpperBoundScoreEver, x);

            // for each parent u of v, reached by using v's source edges...
            for (Loss e : v.getIncomingEdges()) {

                if (e == null)
                    break;

                int ui = e.getSource().getVertexId();

                if (bestInEdge[ui] == null) {
                    bestInEdge[ui] = new Loss[this.VertexColorCount];
                    bestColorInEdgeToU = bestInEdge[ui];

                    // i only need to initiate those entries that may be accessed
                    for (int c = this.VertexColorCount - 1; c > e.getSource().getColor(); c--)
                        bestColorInEdgeToU[c] = nullEdge; // that way, there can not be null-pointer exceptions
                } else
                    bestColorInEdgeToU = bestInEdge[ui]; // faster access to array entries

                // Only bother merging in results for vertices that are not leaves. We will avoid senseless allocation that way
                if (!v.isLeaf()) {

                    for (int c = this.VertexColorCount - 1; c > v.getColor(); c--) {
                        //mergeeeee
                        if (bestColorInEdgeToV[c].getWeight() > bestColorInEdgeToU[c].getWeight())
                            bestColorInEdgeToU[c] = bestColorInEdgeToV[c];

                        if (bestColorInEdgeToU[c].getTarget() == v) {
                            // The best in-edge for colour k is actually the one from v's child u -- so update the maxInEdge for this edge.
                            // This is (the only place) where we calculate new values for maxInEdge.
                            if (bestColorInEdgeToU[c].getWeight() <= e.getWeight())
                                bestColorInEdgeToU[c] = e;
                        }
                    }
                }

                // Now merge in the edge (u, v) to u's info.
                if (bestColorInEdgeToU[v.getColor()].getWeight() < e.getWeight())
                    bestColorInEdgeToU[v.getColor()] = e;
            }

            // We have applied everything known about v to all its parents, so we no longer need v's info.
            bestInEdge[vi] = new Loss[]{nullEdge};        // Shrinks the vector by 1, effectively deleting v's info
        }

    }

    /**
     * strengtened version of sebastian vertex upper bounds
     * altough it is possible to combine both methods into 1 single method, the unstrengthend will be 5 times faster
     * duo to some access optimizations. Therefore, it is wise to let them independent
     * TODO: check for correctness, but probably working, rewritten
     */
    private void strengthenedSebastianVertexUpperBounds() {

        // i create a new edge that is just right know to initiate null-pointers!
        SInEdgesToColor[][] bestInEdgeToColor = new SInEdgesToColor[gGraph.numberOfVertices()][]; // i will create the 2. dimension, if v is not a leaf

        SInEdgesToColor[] bestColorInEdgeToV = null;
        SInEdgesToColor[] bestColorInEdgeToU = null;

        // for every vertex of graph g
        for (int vi = gGraph.numberOfVertices() - 1; vi >= 0; vi--) {

            Fragment v = gGraph.getFragmentAt(vi);
            double x = 0.0;
            bestColorInEdgeToV = bestInEdgeToColor[vi];

            if (bestColorInEdgeToV == null) {
                // This can only mean that v is a leaf, in which case its UB is trivially 0.  Leave it empty, and work around it later.
            } else {

                // STRENGTHENED VERSION!
                x = calcSebVubStrengthFor(v, bestInEdgeToColor[vi]);
            }
            gUB[vi] = Math.min(gUB[vi], x);

            if ((x == 0.0) && (!v.isLeaf())) {
                this.gEdgesDeletedByZeroUpperBounds += v.getOutgoingEdges().size();
            }
            gHighestSebUpperBoundScoreEver = Math.max(gHighestSebUpperBoundScoreEver, x);

            // for each parent u of v, reached by using v's source edges...
            for (Loss e : v.getIncomingEdges()) {

                if (e == null)
                    break;

                int ui = e.getSource().getVertexId();

                if (bestInEdgeToColor[ui] == null) {
                    bestInEdgeToColor[ui] = new SInEdgesToColor[this.VertexColorCount];
                    bestColorInEdgeToU = bestInEdgeToColor[ui];

                    // i only need to initiate those entries that may be accessed
                    for (int c = this.VertexColorCount - 1; c > e.getSource().getColor(); c--)
                        bestColorInEdgeToU[c] = new SInEdgesToColor(); // that way, there can not be null-pointer exceptions
                } else
                    bestColorInEdgeToU = bestInEdgeToColor[ui]; // faster access to array entries

                // Only bother merging in results for vertices that are not leaves. We will avoid senseless allocation that way
                if (!v.isLeaf()) {

                    for (int c = this.VertexColorCount - 1; c > v.getColor(); c--) {
                        //mergeeeee
                        SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[c], bestColorInEdgeToV[c].besLoss, bestColorInEdgeToV[c].maxInEdge);
                        SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[c], bestColorInEdgeToV[c].secondBesLoss, 0.0);

                        if (bestColorInEdgeToU[c].besLoss.getSource() == v) {
                            // The best in-edge for colour k is actually the one from v's child u -- so update the maxInEdge for this edge.
                            // This is (the only place) where we calculate new values for maxInEdge.
                            bestColorInEdgeToU[c].maxInEdge = Math.max(bestColorInEdgeToU[c].maxInEdge, e.getWeight());
                        }
                    }
                }

                // Now merge in the edge (u, v) to u's info.
                SInEdgesToColor.sebUBMergeEdge(bestColorInEdgeToU[v.getColor()], e, Double.NEGATIVE_INFINITY);
            }

            // We have applied everything known about v to all its parents, so we no longer need v's info.
            bestInEdgeToColor[vi] = new SInEdgesToColor[]{null};        // Shrinks the vector by 1, effectively deleting v's info
        }

    }


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
        for (SInEdgesToColor edge : bestInEdgeToColor) {
            if (edge != null) {
                x += edge.besLoss.getWeight();
            }
        }

        // Now look for violations of the subtree property
        // this refers to Bu
        ArrayList<Integer>[] colorsFromColor = new ArrayList[this.VertexColorCount];
        // initiation
        for (int i = 0; i < this.VertexColorCount; i++)
            colorsFromColor[i] = new ArrayList<Integer>(this.VertexColorCount); // to eliminate reallocation!

        for (int i = 0; i < this.VertexColorCount; i++) {
            if (bestInEdgeToColor[i] != null && bestInEdgeToColor[i].besLoss.getSource() != SInEdgesToColor.ZERO_VERTEX) {
                colorsFromColor[bestInEdgeToColor[i].besLoss.getSource().getColor()].add(i);
            }
        }

        boolean[] safeToMessWithInEdgeToColour = new boolean[this.VertexColorCount];

        // We can consider all out-edges leaving a particular colour as an independent problem to fix.
        // Only need to consider colours >= v's colour.
        for (int i = v.getColor(); i < this.VertexColorCount; i++) {
            //HACK: May be a bit expensive to allocate an nVerts-size array in this loop...
            // repairCostsFromVertex[i] is the cost to repair all edges from vertex i to some other vertex that are in the current solution.
            TIntDoubleMap repairCostsFromVertex = new TIntDoubleHashMap();
            double totalRepairCost = 0.0;

            for (int j = 0; j < colorsFromColor[i].size(); j++) {
                // Note that although this loop seems to add an nCol^2 factor to the runtime, it doesn't because the total number
                // of iterations across all values of i is just the number of colours.

                // We know that bestInEdges[coloursFromColour[i][j]].edges[1].w cannot be NEGINF because we know there is at least 1 edge to coloursFromColour[i][j], so it will be min(0.0, that).
                double cost = bestInEdgeToColor[colorsFromColor[i].get(j)].besLoss.getWeight() - bestInEdgeToColor[colorsFromColor[i].get(j)].secondBesLoss.getWeight();

                if (cost == Double.POSITIVE_INFINITY)
                    System.err.println(" positive infinity! ");

                repairCostsFromVertex.adjustOrPutValue(bestInEdgeToColor[colorsFromColor[i].get(j)].besLoss.getSource().getVertexId(), cost, cost);
                totalRepairCost += cost;
            }

            double bestRepairCost = totalRepairCost; // Corresponds to repairing *all* outgoing edges.

            // Try making each i-coloured vertex u the parent of all these edges.
            // We only bother trying the i-coloured vertices that are actually the starting point of
            // an out-edge, since inEdgeRepairCost must be >= 0 and it will cost totalRepairCost + inEdgeRepairCost
            // to move all these out-edges to some different i-coloured vertex, and we already consider
            // repairing all out-edges for a total cost of totalRepairCost.
            for (int j = 0; j < colorsFromColor[i].size(); j++) {
                Fragment u = bestInEdgeToColor[colorsFromColor[i].get(j)].besLoss.getSource();

                double inEdgeRepairCost;
                double maxInEdge = bestInEdgeToColor[colorsFromColor[i].get(j)].maxInEdge; //HACK: maybe get rid of this var altogether.

                if ((i > v.getColor()) && (bestInEdgeToColor[i].besLoss.getSource() == SInEdgesToColor.ZERO_VERTEX) && (!colorsFromColor[i].isEmpty())) {
                    // We have no in-edges to this colour so far but at least one out-edge, so we can add in the best edge to this vertex (which will be negative, since otherwise it would already be present)
                    //HACK: We no longer compute this exactly because it's too expensive -- instead we just take
                    // the second-best edge to this colour, which is a UB.
                    //inEdgeRepairCost = -maxInEdgeToVertex[u];
                    //inEdgeRepairCost = -bestInEdges[relative[u]][1].w;

                    inEdgeRepairCost = -maxInEdge;
                } else {
                    if ((i > v.getColor()) && (bestInEdgeToColor[i].besLoss.getSource() != SInEdgesToColor.ZERO_VERTEX) && (bestInEdgeToColor[i].besLoss.getTarget() != u) && (!colorsFromColor[i].isEmpty()) && safeToMessWithInEdgeToColour[i]) {

                        inEdgeRepairCost = bestInEdgeToColor[i].besLoss.getWeight() - maxInEdge;
                    } else {
                        inEdgeRepairCost = 0.0;
                    }
                }

                double tryUsingU = totalRepairCost - repairCostsFromVertex.get(u.getVertexId()) + inEdgeRepairCost;
                bestRepairCost = Math.min(bestRepairCost, tryUsingU);
            }

            if (bestRepairCost == 0.0) { // I know, I know -- FP equality comparisons are The Devil.
                // Everything about this colour looks good.  So mark all colours we get to from its out-edges
                // as safe to mess with.  (We'll be processing these later.)
                for (int j = 0; j < colorsFromColor[i].size(); j++) {
                    safeToMessWithInEdgeToColour[colorsFromColor[i].get(j)] = true;
                }
            }

            x -= bestRepairCost;
        }

        return x;
    }


    /**
     * resize ubs array to proper size while keeping already applied values
     *
     * @param size: array size
     * @param val:  initial value applied to new entries
     *              WORKING
     */
    private void resizeUpperBounds(int size, double val) {

        double[] buffer = new double[size];
        if ((gUB != null) && (gUB.length > 0)) {

            // some ubs already exists
            System.arraycopy(gUB, 0, buffer, 0, gUB.length);

            //initiate new values, if there are any
            if (size > gGraph.numberOfVertices())
                for (int i = gGraph.numberOfVertices(); i < size; i++)
                    buffer[i] = val;
        } else
            for (int i = 0; i < size; i++)
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
     * TODO: check for correctness, rewritten
     * TODO: postponed
     */
    public void DoDEBUGcalcAnchorLowerBounds() {

        // resize gLB to fit correct size!
        gLB = new double[gGraph.numberOfVertices()][];
        for (int i = 0; i < gGraph.numberOfVertices(); i++) {
            gLB[i] = new double[i + 1];
            for (int j = 0; j < i + 1; j++)
                gLB[i][j] = Double.NEGATIVE_INFINITY;
        }

        for (int v = 0; v < gGraph.numberOfVertices(); v++) {
            boolean[] seen = new boolean[gGraph.numberOfVertices()]; // false by default
            double[] memo = new double[gGraph.numberOfVertices()];   // 0.0 by default, doesn't really matter

            for (int u = 0; u <= v; u++) {
                gLB[v][u] = DEBUGanchorLowerBound(u, v, memo, seen);
            }
        }
    }

    /**
     * @param u
     * @param v
     * @param memo
     * @param seen
     * @return TODO: check for correctness, rewritten
     * TODO: postponed
     */
    private double DEBUGanchorLowerBound(int u, int v, double[] memo, boolean[] seen) {

        if (u > v)
            return Double.NEGATIVE_INFINITY;

        if (!seen[u]) {
            // We require that there be no unreachable edges in the graph.
            //// This is assured if every vertex is either (a) the root, (b) has at least 1 in-edge, or
            //// (relative) has no in-edges and no out-edges.  The following assert checks this.
            Fragment uv = gGraph.getFragmentAt(u);
            assert (uv.getIncomingEdges().size() > 0 || (uv.getIncomingEdges().size() == 0 && (uv.getOutgoingEdges().size() == 0 || (uv.getVertexId() == 0))));

            double x; // it will be set in any case.

            if (v == 0) {
                x = 0.0;
            } else {

                double north;
                if (uv.getIncomingEdges().size() == 0) {
                    // This vertex has no in-edges: Either it's the root, or it has no out-edges either.
                    // Either way, there's no possibility to connect v north of it.
                    north = Double.NEGATIVE_INFINITY;
                } else {

                    // This vertex is reachable from the root and has at least 1 in-edge.
                    north = Double.POSITIVE_INFINITY; // We know there is at least 1 term in the min(), so this will come down.
                    for (Loss e : uv.getOutgoingEdges()) {
                        if (e == null)
                            break;

                        int p = e.getSource().getVertexId();
                        double plb = DEBUGanchorLowerBound(p, v, memo, seen);
                        if (plb < north) // this is basically like "min", just faster :)
                            north = plb;
                    }
                }

                double direct = Double.NEGATIVE_INFINITY;
                // And is there actually an edge directly from u to v?  We could also choose that.
                // We do this the hard way...
                for (Loss e : gGraph.getFragmentAt(v).getOutgoingEdges()) {
                    if (e == null)
                        break;

                    if (e.getSource() == uv) {
                        direct = e.getWeight();
                        break;
                    }
                }

                // If u is root-reachable and v == u, then we trivially can force v in for 0.
                if (u == v && gGraph.getFragmentAt(v).getIncomingEdges().size() > 0)
                    direct = 0.0;

                x = (north > direct) ? north : direct;
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

    protected double[][] getLB() {
        return gLB;
    }

    protected void setLB(double[][] gLB) {
        this.gLB = gLB;
    }

    protected double[] getUB() {
        return gUB;
    }

    protected void setUB(double[] gUB) {
        this.gUB = gUB;
    }

    protected FGraph getGraph() {
        return this.gGraph;
    }

    ///////////////////////
    /// --- TESTING --- ///


    public void printVertexUpperBounds() {

        System.out.println("||---------------------------------");
        System.out.println("||--- Printing Vertex Upper Bounds ");

        for (int i = 0; i < this.gUB.length; i++) {
            System.out.println("ub[" + i + "] = " + this.gUB[i] + " @ Vertex with ID: " + gGraph.getFragmentAt(i).getVertexId());
        }

        System.out.println("||--- finished ");
        System.out.println("||---------------------------------");
    }

    public void printLowerBounds() {

        System.out.println("||--------------------------");
        System.out.println("||--- Printing Lower Bounds ");

        for (int i = 0; i < gLB.length; i++) {
            System.out.print(i);
            for (int j = 0; j < gLB[i].length; j++) {
                System.out.print(" " + gLB[i][j]);
            }
            System.out.println();
        }

        System.out.println("||--- finished ");
        System.out.println("||--------------------------");
    }

    public void printm_() {

        System.out.println("||----------------------------");
        System.out.println("||--- Printing lower bound m_ ");

        if (m_ != null) {

            String s = "";
            for (int i = 0; i < m_.length; i++) {

                s = "";
                for (int j = 0; j < m_[i].length; j++) {
                    s = s + (float) ((int) (m_[i][j] * 10000.0)) / 10000.0 + " <> ";
                }
                System.out.println(" ||< " + i + " >  " + s);
            }
        } else {
            System.out.println(" m_ is null.");
        }

        System.out.println("||--- finished ");
        System.out.println("||---------------------------------");
    }

}
