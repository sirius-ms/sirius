package de.unijena.bioinf.ChemistryBase.ms.ft;


import de.unijena.bioinf.ChemistryBase.chem.Ionization;

public interface FTGraph<T extends FTFragment> {

    public T getRoot();
    public Ionization getIonization();



}
