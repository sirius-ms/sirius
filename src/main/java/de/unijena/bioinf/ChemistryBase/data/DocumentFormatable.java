package de.unijena.bioinf.ChemistryBase.data;

public interface DocumentFormatable {

    public <G, D, L> G addToDocument(DataDocument<G, D, L> document, G entry);

}
