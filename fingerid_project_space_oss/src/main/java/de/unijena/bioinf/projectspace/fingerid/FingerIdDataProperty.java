package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.PosNegProperty;

public class FingerIdDataProperty extends PosNegProperty<FingerIdData> {
    public FingerIdDataProperty(FingerIdData positive, FingerIdData negative) {
        super(positive, negative);
    }
}
