package de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface Decomposer<S> {

    public S initialize(ChemicalAlphabet alphabet, MSExperimentInformation informations);

    public boolean alphabetStillValid(S decomposer, ChemicalAlphabet alphabet);

    public boolean isDecomposable(S decomposer, double mass, MSExperimentInformation info);

    public List<MolecularFormula> decompose(S decomposer, double mass, MSExperimentInformation info);

}
