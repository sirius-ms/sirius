package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;

import java.util.Map;

public class ZodiacResultsWithClusters extends ZodiacResult<FragmentsCandidate> {
    Map<String, String[]> representativeToCluster;

    public ZodiacResultsWithClusters(String[] ids, Graph<FragmentsCandidate> graph, CompoundResult<FragmentsCandidate>[] results, Map<String, String[]> representativeToCluster) {
        super(ids, graph, results);
        this.representativeToCluster = representativeToCluster;
    }

    public Map<String, String[]> getRepresentativeToCluster() {
        return representativeToCluster;
    }

}
