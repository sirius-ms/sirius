package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.babelms.cef.P;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.util.ArrayList;
import java.util.List;

public class AdductNode {

    private static KnownAdductType[] NOTHING_KNOWN_PLUS = new KnownAdductType[]{new IonType(PrecursorIonType.unknown(1))},
                                    NOTHING_KNOWN_MINUS = new KnownAdductType[]{new IonType(PrecursorIonType.unknown(-1))};

    protected final int index;

    protected final AlignedFeatures features;
    protected final List<AdductEdge> edges;

    protected int selectedIon;
    protected KnownAdductType[] ionTypes;

    public AdductNode(AlignedFeatures features, int index) {
        this.features = features;
        this.edges = new ArrayList<>();
        this.index = index;
        this.ionTypes = features.getCharge()>0 ? NOTHING_KNOWN_PLUS : NOTHING_KNOWN_MINUS;
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
