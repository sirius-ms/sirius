package de.unijena.bioinf.sirius.gui.db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;

interface CompoundImportedListener {
    public void compoundImported(InChI inchi);
}
