package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class Test {

    public static void main(String[] args) {
        PrecursorIonType.getPrecursorIonType("[M]+");
        System.out.println(PrecursorIonType.getPrecursorIonType("[2M+H]+"));
    }

}
