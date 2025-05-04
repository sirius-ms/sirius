package de.unijena.bioinf.ms.biotransformer;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class BioTransformation {
    @NotNull
    @Builder.Default
    private Set<IAtomContainer> substrates = Set.of();
    @Nullable
    private String reactionType;
    @NotNull
    @Builder.Default
    private List<String> enzymeNames = List.of();
    @NotNull
    @Builder.Default
    private Set<IAtomContainer> products = Set.of();
    @Nullable
    private String bioSystemName;
    @Builder.Default
    private double score = Double.NaN;
}
