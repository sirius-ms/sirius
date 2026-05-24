package reactionTool.sirius.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ParallelStep.class, name = "PARALLEL"),
    @JsonSubTypes.Type(value = LoopStep.class, name = "LOOP")
})
public abstract class Step {
}
