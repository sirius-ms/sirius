package de.unijena.bioinf.ms.biotransformer;

import io.siriusms.shadow.bt.cdk.AtomContainerSet;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@Builder
@Getter
public class BioTransformation {
    @NotNull
    @Builder.Default
    private AtomContainerSet substrates = new AtomContainerSet();
    @Nullable
    private String reactionType;
    @NotNull
    @Builder.Default
    private ArrayList<String> enzymeNames = new ArrayList();
    @NotNull
    @Builder.Default
    private AtomContainerSet products = new AtomContainerSet();
    @Nullable
    private String bioSystemName;
    @Builder.Default
    private double score = Double.NaN;
}
