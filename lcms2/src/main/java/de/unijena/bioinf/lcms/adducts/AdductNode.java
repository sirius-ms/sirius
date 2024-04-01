package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.util.ArrayList;
import java.util.List;

public class AdductNode {

    protected final AlignedFeatures features;
    protected final List<AdductEdge> edges;

    public AdductNode(AlignedFeatures features) {
        this.features = features;
        this.edges = new ArrayList<>();
    }

    protected void addEdge(AdductNode target, KnownMassDelta[] knownMassDeltas) {
        AdductEdge edge = new AdductEdge(this, target, knownMassDeltas);
        this.edges.add(edge);
        target.edges.add(edge);
    }

    public List<AdductNode> getNeighbours() {
        List<AdductNode> xs = new ArrayList<>(edges.size());
        for (AdductEdge edge : edges) {
            if (edge.left==this) xs.add(edge.right);
            else xs.add(edge.left);
        }
        return xs;
    }

    public double getMass() {
        return features.getAverageMass();
    }

    public AlignedFeatures getFeature() {
        return features;
    }

    public double getRetentionTime() {
        return features.getRetentionTime().getRetentionTimeInSeconds();
    }
}
