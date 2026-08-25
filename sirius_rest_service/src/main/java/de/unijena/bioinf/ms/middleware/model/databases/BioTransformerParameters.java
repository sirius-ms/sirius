package de.unijena.bioinf.ms.middleware.model.databases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.biotransformer.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static de.unijena.bioinf.ms.biotransformer.MetabolicTransformation.HUMAN_CUSTOM_MULTI;

// Bound through the implicit no-arg constructor and setters on purpose:
// this DTO documents optional-with-default properties, and only field binding keeps those defaults when
// a request omits them. Constructor binding would deliver null for an omitted primitive, which is
// rejected (FAIL_ON_NULL_FOR_PRIMITIVES) and would contradict the published schema. Do not add
// @Builder/@AllArgsConstructor here: Jackson would prefer the all-args constructor, and @Builder.Default
// moves the field initialisers away so the documented defaults would silently become 0/false/null.
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BioTransformerParameters {
    /**
     * Specify the Phase I/Cyp450 mode for all provided BioTransformerSequenceSteps. Will only be applied to Steps that
     * require the Cyp450 mode as parameter. Can be null in cases where only BioTransformerSequenceSteps are specified
     * that do not need the Cyp450 mode.
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "COMBINED")
    private Cyp450Mode cyp450Mode = Cyp450Mode.COMBINED;

    /**
     * Specify the Phase II mode for all provided BioTransformerSequenceSteps. Will only be applied to Steps that
     * require the Phase II mode as parameter. Can be null in cases where only BioTransformerSequenceSteps are specified
     * that do not need the Phase II mode.
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "BT_RULE_BASED")
    private P2Mode p2Mode = P2Mode.BT_RULE_BASED;

    /**
     * Specify whether structures should additionally be retrieved from the HMDB database.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "true")
    private boolean useDB = true;

    /**
     * Specify BioTransformerSequenceSteps to be applied to input structures. MultiStep MetabolicTransformations can
     * only be used as singletons (list size of one).
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<BioTransformerSequenceStep> bioTransformerSequenceSteps;

    @JsonIgnore
    public BioTransformerSettings toSettings(){
        if (bioTransformerSequenceSteps == null || bioTransformerSequenceSteps.isEmpty())
            throw new IllegalArgumentException("No BioTransformer sequence step found! At least one BioTransformer sequence step is required!");
        if (bioTransformerSequenceSteps.size() > 1 && !bioTransformerSequenceSteps.stream()
                .map(BioTransformerSequenceStep::getMetabolicTransformation)
                .allMatch(MetabolicTransformation::isSequenceAllowed)){
            throw new IllegalArgumentException("More then one BioTransformer sequence steps found. At least one specified MetabolicTransformation is not allowed for multistep sequence. Sequence must not contain any multi step MetabolicTransformation!");
        }

        if (bioTransformerSequenceSteps.stream().map(BioTransformerSequenceStep::getMetabolicTransformation).anyMatch(HUMAN_CUSTOM_MULTI::equals))
            throw new IllegalArgumentException(HUMAN_CUSTOM_MULTI.getDisplayName() + " is not allowed as parameter. Please specify the individual steps instead.");

        BioTransformerSettings settings = new BioTransformerSettings()
                .setUseSub(true)
                .setUseDB(useDB)
                .setP2Mode(p2Mode)
                .setCyp450Mode(cyp450Mode);
        if (bioTransformerSequenceSteps.size() == 1) {
            BioTransformerSequenceStep biotrans = bioTransformerSequenceSteps.getFirst();
            settings.setMetabolicTransformation(biotrans.getMetabolicTransformation())
                    .setIterations(biotrans.getIterations());
        } else {
            settings.setMetabolicTransformation(HUMAN_CUSTOM_MULTI);
            bioTransformerSequenceSteps.forEach(settings::addSequenceStep);
        }

        return settings;
    }
}
