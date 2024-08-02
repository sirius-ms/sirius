package de.unijena.bioinf.ChemistryBase.ms.lcms.workflows;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property = "workflow")
@JsonSubTypes({@JsonSubTypes.Type(MixedWorkflow.class), @JsonSubTypes.Type(PooledMs2Workflow.class),@JsonSubTypes.Type(RemappingWorkflow.class)})
public abstract class LCMSWorkflow {
}
