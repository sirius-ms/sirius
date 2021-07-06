package de.unijena.bioinf.networks.serialization;

import de.unijena.bioinf.networks.Correlation;
import de.unijena.bioinf.networks.EdgeType;

public class CorrelationConnection extends AbstractConnection {

    public CorrelationConnection(String targetName, float mzdiff, float weight) {
        super(targetName, mzdiff, weight);
    }

    @Override
    public EdgeType asEdgeType() {
        return new Correlation(this.weight);
    }
}
