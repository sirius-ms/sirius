package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Getter
@Builder
public class InsilicoFragmentationResult {

    private final CombinatorialSubtree subtree;
    private final Map<Fragment, ArrayList<CombinatorialFragment>> fragmentMapping;
    private final double score;

    public static InsilicoFragmentationResult of(CombinatorialSubtreeCalculator calculator){
        return InsilicoFragmentationResult.builder()
                .fragmentMapping(Collections.unmodifiableMap(calculator.getMapping()))
                .score(calculator.getScore())
                .subtree(calculator.getSubtree())
                .build();
    }
}
