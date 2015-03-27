package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;

/**
 * quick'n dirty solution for one-way writing into data document
 */
public interface WriteIntoDataDocument {

    public <G,D,L> void writeIntoDataDocument(DataDocument<G,D,L> document, D dictionary);

}
