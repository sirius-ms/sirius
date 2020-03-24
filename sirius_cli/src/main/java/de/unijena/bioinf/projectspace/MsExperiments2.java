package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.LoggerFactory;

public class MsExperiments2 extends de.unijena.bioinf.ChemistryBase.ms.utils.MsExperiments {
    public static MolecularFormula extractMolecularFormula(Ms2Experiment exp) {
        final String f = extractMolecularFormulaString(exp);
        if (f == null)
            return null;
        return MolecularFormula.parseOrNull(f);
    }

    public static String extractMolecularFormulaString(Ms2Experiment exp) {
        MolecularFormula formula = exp.getMolecularFormula();
        if (formula != null) {
            return formula.toString();
        }

        if (exp.hasAnnotation(InChI.class)) {
            InChI inChI = exp.getAnnotationOrThrow(InChI.class);
            formula = inChI.extractFormulaOrThrow();
            return formula.toString();
        }

        if (exp.hasAnnotation(Smiles.class)) {
            Smiles smiles = exp.getAnnotationOrThrow(Smiles.class);
            try {
                final IAtomContainer mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(smiles.smiles);
                String formulaString = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(mol));
                return formulaString;
            } catch (CDKException e) {
                LoggerFactory.getLogger(MsExperiments2.class).warn("Could not parse Smiles from Compound: " + exp.getName(), e);
            }
        }
        return null;
    }

    public static void clearMolecularFormulaAnnotations(Ms2Experiment exp) {
        exp.removeAnnotation(Smiles.class);
        exp.removeAnnotation(InChI.class);
    }


}
