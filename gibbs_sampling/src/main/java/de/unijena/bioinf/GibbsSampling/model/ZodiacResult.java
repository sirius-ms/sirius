package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;

public class ZodiacResult<C extends Candidate<?>> {
    private String[] ids;
    private Graph<C> graph;
    private CompoundResult<C>[] results;

    public ZodiacResult(String[] ids, Graph<C> graph, CompoundResult<C>[] results) {
        this.ids = ids;
        this.graph = graph;
        this.results = results;
    }

    public String[] getIds() {
        return ids;
    }

    public Graph<C> getGraph() {
        return graph;
    }

    public CompoundResult<C>[] getResults() {
        return results;
    }
}
