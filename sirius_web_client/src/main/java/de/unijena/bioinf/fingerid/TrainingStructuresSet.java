package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;

import java.util.HashSet;
import java.util.Set;

public class TrainingStructuresSet {

    final Set<String> inchiKeys2D;

    public TrainingStructuresSet(InChI[] inchis) {
        this.inchiKeys2D = new HashSet<>();
        for (InChI inchi : inchis) {
            inchiKeys2D.add(inchi.key2D());
        }
    }

    public boolean isInTrainingData(InChI inchi){
        return inchiKeys2D.contains(inchi.key2D());
    }
}
