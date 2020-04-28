package de.unijena.bioinf.projectspace.canopus;

import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.PosNegProperty;

public class CanopusDataProperty extends PosNegProperty<CanopusData> {
    public CanopusDataProperty(CanopusData positive, CanopusData negative) {
        super(positive, negative);
    }
}
