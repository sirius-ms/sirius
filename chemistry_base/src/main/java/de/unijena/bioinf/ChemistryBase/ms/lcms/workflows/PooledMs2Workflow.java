package de.unijena.bioinf.ChemistryBase.ms.lcms.workflows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * We have several MS1 samples from different conditions, as well
 * as one or more pooled MS2 sample across all conditions. We additionally
 * have a pooled MS1 sample. We want to align the pooled MS2 with the pooled
 * MS1 and then align them across all other MS1
 */
@JsonTypeName("pooled-ms2")
public class PooledMs2Workflow extends LCMSWorkflow {

    @JsonProperty("pooled-ms2") private final String[] pooledMs2;
    @JsonProperty("pooled-ms1") private final String[] pooledMs1;
    @JsonProperty("remaining-ms1") private final String[] remainingMs1;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PooledMs2Workflow(
            @JsonProperty("pooled-ms2") String[] pooledMs2,
            @JsonProperty("pooled-ms1")  String[] pooledMs1,
            @JsonProperty("remaining-ms1") String[] remainingMs1) {
        this.pooledMs2 = pooledMs2;
        this.pooledMs1 = pooledMs1;
        this.remainingMs1 = remainingMs1;
    }

    public String[] getPooledMs2() {
        return pooledMs2;
    }

    public String[] getPooledMs1() {
        return pooledMs1;
    }

    public String[] getRemainingMs1() {
        return remainingMs1;
    }
}
