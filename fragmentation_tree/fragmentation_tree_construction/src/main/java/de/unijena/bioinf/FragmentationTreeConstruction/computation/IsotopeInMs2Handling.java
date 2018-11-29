package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;

@DefaultProperty(propertyParent = "IsotopeHandlingMs2")
public enum IsotopeInMs2Handling {
    /**
     * never look for isotopes in MS2
     */
    IGNORE,

    /**
     * enable if estimated isolation window allows for isotope peaks in MS2
     */
    IF_NECESSARY,

    /**
     * look for isotopes in MS2 if experiment is measured on a Bruker Maxis
     */
    BRUKER_ONLY,

    /**
     * look for isotopes in MS2 if experiment is measured on a Bruker Maxis.
     * But enable isotope scoring only if enough intensive peaks show an isotope pattern
     */
    BRUKER_IF_NECESSARY,

    /**
     * enforce scoring of isotopes in MS2, even if spectrum is not measured on a Bruker Maxis.
     */
    ALWAYS;

    public static final IsotopeInMs2Handling DEFAULT() {
        return PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeInMs2Handling.class);
    }
}
