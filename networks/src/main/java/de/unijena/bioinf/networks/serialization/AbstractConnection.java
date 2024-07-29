package de.unijena.bioinf.networks.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.networks.EdgeType;

public abstract class AbstractConnection {

    @JsonProperty public String targetName;
    @JsonProperty public float weight;
    @JsonProperty public float mzdiff;

    public AbstractConnection(String targetName, float mzdiff, float weight) {
        this.targetName = targetName;
        this.weight = weight;
        this.mzdiff = mzdiff;

    }

    public abstract EdgeType asEdgeType();
}
