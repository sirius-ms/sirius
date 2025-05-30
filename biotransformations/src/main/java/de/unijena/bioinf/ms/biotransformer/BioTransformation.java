package de.unijena.bioinf.ms.biotransformer;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.*;

@Builder
@Getter
public class BioTransformation {
    //substrateTransformation is null if substrate is the origin molecule.
    @Setter
    @NotNull
    @Builder.Default
    private List<BioTransformation> possibleSubstrateTransformations = new ArrayList<>(2);

    @NotNull
    private IAtomContainer substrate;

    @Nullable
    private String reactionType;
    @NotNull
    @Builder.Default
    private List<String> enzymeNames = List.of();
    @NotNull
    private Set<IAtomContainer> products;
    @Nullable
    private String bioSystemName;
    @Builder.Default
    private double score = Double.NaN;

    public boolean isOriginTransformation(){
        return Utils.isNullOrEmpty(possibleSubstrateTransformations);
    }

    public IAtomContainer getOrigin(){
        return getShortestTransformationPath().getFirst().getSubstrate();
    }

    /**
     * Performs a breadth-first search on the graph of biotranformations to find the shortest path from the current transformation to the origin.
     * @return the shortest transformation path from origin (first) to current transformation (last element of the list).
     */
    public List<BioTransformation> getShortestTransformationPath() {
        return new ArrayList<>(getParent(List.of(new LinkedHashSet<>(List.of(this)))).reversed());
    }

    private LinkedHashSet<BioTransformation> getParent(final List<LinkedHashSet<BioTransformation>> paths){
        // iterate over all paths and try to extend them towards origin.
        // stop if origin is already reached. Discard a path if a circle would be created.
        List<LinkedHashSet<BioTransformation>> nuPaths = new ArrayList<>(paths.size() * 2);
        for (LinkedHashSet<BioTransformation> path : paths) {
            BioTransformation current = path.getLast();
            if(current.isOriginTransformation()){
                return path;
            } else {
                @NotNull List<BioTransformation> parents = current.getPossibleSubstrateTransformations();
                if (parents.size() == 1){
                    // circle found, remove a path from possible solutions.
                    if (!path.contains(parents.getFirst())){
                        path.add(parents.getFirst());
                        nuPaths.add(path);
                    }
                } else {
                    for (BioTransformation parent : parents) {
                        // when no circle found, create a split.
                        if (!path.contains(parent)){
                            LinkedHashSet<BioTransformation> nuPath = new LinkedHashSet<>(path);
                            nuPath.add(parent);
                            nuPaths.add(nuPath);
                        }
                    }
                }
            }
        }
        return getParent(nuPaths);
    }
}
