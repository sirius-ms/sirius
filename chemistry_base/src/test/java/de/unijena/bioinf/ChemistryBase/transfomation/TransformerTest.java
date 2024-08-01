package de.unijena.bioinf.ChemistryBase.transfomation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Created by fleisch on 30.05.17.
 */
public class TransformerTest {

    @Test
    public void simpleTest(){

        List<MolecularFormula> transformation = BioTransformer.transform(MolecularFormula.parseOrThrow("C3H6O2"), BioTransformation.C2H2);
        assertNotNull(transformation);
        assertFalse(transformation.isEmpty());
        System.out.println(transformation);

        transformation = BioTransformer.transform(MolecularFormula.parseOrThrow("CH6O2"), BioTransformation.C2H2);
        assertNotNull(transformation);
        assertFalse(transformation.isEmpty());
        System.out.println(transformation);

        transformation = BioTransformer.transform(MolecularFormula.parseOrThrow("C6SO3H"), BioTransformation.SH);
        assertNotNull(transformation);
        assertFalse(transformation.isEmpty());
        System.out.println(transformation);

        transformation = BioTransformer.transform(MolecularFormula.parseOrThrow("C6HS"), BioTransformation.SH);
        assertNotNull(transformation);
        assertFalse(transformation.isEmpty());
        System.out.println(transformation);

        transformation = BioTransformer.getAllTransformations(MolecularFormula.parseOrThrow("C3H6O2"));
        assertNotNull(transformation);
        assertFalse(transformation.isEmpty());
        System.out.println(transformation);

    }

}
