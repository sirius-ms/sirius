package de.unijena.bioinf.ChemistryBase.ms.lcms.workflows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("mixed")
public class MixedWorkflow extends LCMSWorkflow {

    private final String[] files;
    private final boolean align;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MixedWorkflow(
            @JsonProperty("files") String[] files,
            @JsonProperty("align") boolean align) {
        this.files = files;
        this.align = align;
    }

    public String[] getFiles() {
        return files;
    }

    public boolean isAlign() {
        return align;
    }
}
