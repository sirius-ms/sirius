package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.ArrayList;

/**
 * Created by Spectar on 13.11.2014.
 */
abstract public class AbstractSolver {

    protected final int[] offsets;
    protected final int[] edgeIds;
    protected boolean built;

    protected final FGraph graph;
    protected final ArrayList<Loss> losses;

    protected final ProcessedInput input;
    protected final TreeBuilder feasibleSolver;

    protected final double lowerbound;
    protected final int timelimit;


    ////////////////////////
    //--- CONSTRUCTORS ---//
    ////////////////////////


    public AbstractSolver(FGraph graph)
    {
        this(graph, null, Double.NEGATIVE_INFINITY, null, -1);
    }

    public AbstractSolver(FGraph graph, double lowerbound)
    {
        this(graph, null, lowerbound, null, -1);
    }

    protected AbstractSolver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit)
    {
        this.graph = graph;
        this.losses = new ArrayList<Loss>(graph.numberOfEdges() + 1);

        // get all edges from the graph as single list
        for (Fragment f : graph.getFragments())
            for (Loss e : f.getIncomingEdges())
                this.losses.add(e);

        this.lowerbound = lowerbound;

        this.input = input;
        this.feasibleSolver = feasibleSolver;

        this.offsets = new int[graph.numberOfVertices()];
        this.edgeIds = new int[this.losses.size()];
        this.timelimit = Integer.MAX_VALUE;

        this.built = false;
    }

    /**
     * - this class should be implemented through abstract sub methods
     * - model.update() like used within the gurobi solver may be used within one of those, if necessary
     */
    public void build() {
        try {
            defineVariables();
            if (feasibleSolver != null) {
                final FTree presolvedTree = feasibleSolver.buildTree(input, graph, lowerbound);
                setStartValues(presolvedTree);
            }
            computeOffsets();
            setConstraints();
            setLowerbound();
            built = true;
        } catch (Exception e) {
            throw new RuntimeException(String.valueOf(e.getMessage()), e);
        }
    }

    //-- Methods to initiate the solver
    //-- Exception types may be override within subclasses, if needed

    protected void setConstraints() throws Exception {
        setTreeConstraint();
        setColorConstraint();
        setMinimalTreeSizeConstraint();
    }


    abstract protected void defineVariables() throws Exception;
    abstract protected void setStartValues(FTree presolvedTree) throws Exception;
    abstract protected void computeOffsets() throws Exception;
    abstract protected void setLowerbound() throws Exception;

    abstract protected void setTreeConstraint() throws Exception;
    abstract protected void setColorConstraint() throws Exception;

    abstract protected void setMinimalTreeSizeConstraint() throws Exception;

    abstract protected FTree solve() throws Exception;
}
