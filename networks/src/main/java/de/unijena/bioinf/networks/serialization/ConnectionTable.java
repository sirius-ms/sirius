package de.unijena.bioinf.networks.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.HashSet;
import java.util.Set;

public class ConnectionTable implements DataAnnotation {
    @JsonProperty public String id;
    @JsonProperty public float mz;
    @JsonProperty public int subNetwork;
    @JsonProperty public CorrelationConnection[] correlations;

    public ConnectionTable(String nodeId, int subNetworkId, float mz, CorrelationConnection[] correlations) {
        this.correlations = correlations;
        this.id = nodeId;
        this.mz = mz;
        this.subNetwork = subNetworkId;
    }

    public Set<String> neighbours() {
        final HashSet<String> xs = new HashSet<>();
        for (CorrelationConnection c : correlations) {
            xs.add(c.targetName);
        }

        return xs;
    }

    public AbstractConnection[] edges() {
        final AbstractConnection[] edges = new AbstractConnection[correlations.length];
        int offset = 0;
        System.arraycopy(correlations,0,edges,offset,correlations.length);
        offset += correlations.length;

        return edges;
    }
}
