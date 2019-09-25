package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;

public interface AnnotatedSpectrum<T extends Peak> extends Spectrum<T>, Annotated<SpectrumAnnotation> {

}
