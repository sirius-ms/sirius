package de.unijena.bioinf.babelms.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.Set;

/*
    DON'T USE THIS YET! It's just a draft!
 */
public interface CompoundQuery {

    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, double absoluteDeviation);
    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, Deviation allowedDeviation);

}
