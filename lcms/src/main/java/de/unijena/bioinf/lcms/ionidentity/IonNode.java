package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.model.lcms.IonGroup;

import java.util.*;

class IonNode {

    protected List<Edge> neighbours;
    private AlignedFeatures feature;
    protected double mz;
    protected boolean hasMsMs;

    protected IonAssignment assignment;

    public IonNode(AlignedFeatures feature) {
        this.mz = feature.getMass();
        this.neighbours = new ArrayList<>();
        setFeature(feature);
    }

    public Set<PrecursorIonType> possibleIonTypes() {
        final HashSet<PrecursorIonType> types = new HashSet<>();
        for (Edge e : neighbours) {
            if (e.type== Edge.Type.ADDUCT) {
                if (e.fromType!=null) types.add(e.fromType);
            }
        }
        return types;
    }

    public boolean hasConflicts() {
        int prev = 0;
        for (; prev < neighbours.size(); ++prev) {
            if (neighbours.get(prev).type== Edge.Type.ADDUCT)
                break;
        }
        for (int k=prev+1; k < neighbours.size(); ++k) {
            if (neighbours.get(k).type== Edge.Type.ADDUCT) {
                if (!neighbours.get(prev).fromType.equals(neighbours.get(k).fromType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public AlignedFeatures getFeature() {
        return feature;
    }

    public void setFeature(AlignedFeatures feature) {
        this.feature = feature;
        this.hasMsMs = feature.getFeatures().values().stream().anyMatch(x->x.getMsMs()!=null);
    }

    public boolean hasEdge(Edge edge) {
        if (edge.fromType!=null && edge.toType!=null) {
            return neighbours.stream().anyMatch(n->edge.to == n.to && edge.fromType.equals(n.fromType) && edge.toType.equals(n.toType));
        } else {
            return neighbours.stream().anyMatch(n->n.to==edge.to);
        }

    }

    @Override
    public String toString() {
        return "IonNode{" +
                "neighbours=" + neighbours +
                ", feature=" + feature +
                ", mz=" + mz +
                ", hasMsMs=" + hasMsMs +
                '}';
    }

    public Set<PrecursorIonType> likelyIonTypes() {
        final HashSet<PrecursorIonType> types = new HashSet<>();
        for (int k=0; k < assignment.probabilities.length; ++k) {
            if (assignment.probabilities[k]>=0.05) {
                types.add(assignment.ionTypes[k]);
            }
        }
        return types;
    }

    String likelyTypesWithProbs() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        for (int k=0; k < assignment.probabilities.length; ++k) {
            if (assignment.probabilities[k]>=0.1) {
                buf.append('"');
                buf.append(assignment.ionTypes[k].toString());
                buf.append('"');
                buf.append(':');
                buf.append(Math.round(assignment.probabilities[k] * 100));
                buf.append(",");
            }
        }
        buf.append("}");
        return buf.toString();
    }
}
