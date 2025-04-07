package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import lombok.AllArgsConstructor;

@DefaultProperty
@AllArgsConstructor
public class AnalogSpectraSearch implements Ms2ExperimentAnnotation {

    public final boolean value;

    public AnalogSpectraSearch() {
        this.value = false;
    }
}