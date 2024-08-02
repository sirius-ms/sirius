package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.fragmenter.InsilicoFragmentationResult;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Stores substructure annotations for compound candidates
 */
@Getter
@Builder
public class SubstructureAnnotationResult implements ResultAnnotation {
    private final Map<String, InsilicoFragmentationResult> inchiToFragmentationResult;
}
