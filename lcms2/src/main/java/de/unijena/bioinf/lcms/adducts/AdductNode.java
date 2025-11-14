package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.util.*;

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

    public void removeEdgeTo(AdductNode v) {
        ListIterator<AdductEdge> e = edges.listIterator();
        while (e.hasNext()) {
            if (e.next().getOther(this)==v) {
                e.remove();
                return;
            }
        }
    }

    public boolean isIsotopeNode() {
        for (AdductEdge e : getEdges()) {
            if (e.getRight()==this && e.isIsotopeEdge()) {
                return true;
            }
        }
        return false;
    }

    HashSet<AdductNode> collectIsotopes(HashSet<AdductNode> buffer) {
        for (AdductEdge e : getEdges()) {
            if (e.isIsotopeEdge()) {
                if (e.getLeft()==this) {
                    e.getRight().collectIsotopes(buffer);
                } else {
                    buffer.add(this);
                }
            }
        }
        return buffer;
    }
}
