package de.unijena.bioinf.ms.persistence.model.core.feature;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.ChemistryBase.ms.PossibleElement;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;

/**
 * Analogue to detected adducts we can also detect the presence of elements.
 */
@ToString
@Getter
@Setter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.ANY)
public class DetectedElements {

    private Map<de.unijena.bioinf.ChemistryBase.ms.DetectedElements.Source, DetectedElementalComposition> detectedElements;

    public DetectedElements() {
        this(new HashMap<>());
    }

    public DetectedElements(Map<de.unijena.bioinf.ChemistryBase.ms.DetectedElements.Source, DetectedElementalComposition> detectedElements) {
        this.detectedElements = detectedElements;
    }

    public void addDetectedElements(de.unijena.bioinf.ChemistryBase.ms.DetectedElements det) {
        for (Map.Entry<de.unijena.bioinf.ChemistryBase.ms.DetectedElements.Source, PossibleElement[]> key : det.getDetections().entrySet()) {
            this.detectedElements.put(key.getKey(), new DetectedElementalComposition(key.getValue()));
        }
    }

}
