package de.unijena.bioinf.fingerid.db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;

public interface CompoundImportedListener {
    void compoundImported(InChI inchi);
}
