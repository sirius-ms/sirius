package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdductNode {


    protected final int index;

    protected final AlignedFeatures features;
    protected final List<AdductEdge> edges;

    protected boolean hasMsMs;

    public AdductNode(AlignedFeatures features, int index) {
        this.features = features;
        this.edges = new ArrayList<>();
        this.index = index;
    }

    public List<AdductNode> getNeighbours() {
        List<AdductNode> xs = new ArrayList<>(edges.size());
        for (AdductEdge edge : edges) {
            if (edge.left==this) xs.add(edge.right);
            else xs.add(edge.left);
        }
        return xs;
    }

    public List<AdductEdge> getEdges() {
        return edges;
    }

    public int getIndex() {
        return index;
    }

    public AlignedFeatures getFeatures() {
        return features;
    }

    public double getMass() {
        return features.getAverageMass();
    }

    public AlignedFeatures getFeature() {
        return features;
    }

    public double getRetentionTime() {
        if(features.getRetentionTime() == null)
            return Double.NaN;
        return features.getRetentionTime().getRetentionTimeInSeconds();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "<%.4f m/z @ %.2f min>", getMass(), getRetentionTime()/60d);
    }
}
