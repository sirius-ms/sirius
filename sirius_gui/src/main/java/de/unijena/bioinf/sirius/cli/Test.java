package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Precursor;

public class Test {

    public static void main(String[] args) {
        PrecursorIonType.getPrecursorIonType("[M]+");
        System.out.println(PrecursorIonType.getPrecursorIonType("[2M+H]+"));
    }

}
