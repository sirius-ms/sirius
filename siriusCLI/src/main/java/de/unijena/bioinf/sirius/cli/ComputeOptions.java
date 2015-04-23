package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.sirius.TreeOptions;

public interface ComputeOptions extends TreeOptions {

    @Option(shortName = "f", longName = "formula", description = "the molecular formula of the input compound", defaultToNull = true)
    public String getMolecularFormula();

}
