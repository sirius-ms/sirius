
package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaPacker;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaSet;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by kaidu on 31.03.14.
 */
public class FormulaPackerTest {

    @Test
    public void testEncodingDecoding() {
        final MolecularFormulaPacker packer = MolecularFormulaPacker.newPacker(ChemicalAlphabet.alphabetFor(MolecularFormula.parseOrThrow("CHNOPSClBrF")));

        final String[] formulas = new String[]{
                "C6H12O6", "C3H4NOP3", "C2H4SO8", "C22H60P8S8", "C16H22F16O8", "C4H7Br"
        };

        for (String s : formulas) {
            final MolecularFormula f = MolecularFormula.parseOrThrow(s);
            assertEquals(f, packer.decode(packer.encode(f)));
        }
    }

    @Test
    public void testPackerCreationFromFormulas() {
        final String[] formulas = new String[]{
                "C6H12O6", "C3H4NOP3", "C2H4SO8", "C22H60P8S8", "C16H22F16O8", "C4H7Br50"
        };
        final MolecularFormula[] forms = new MolecularFormula[formulas.length];
        for (int k = 0; k < formulas.length; ++k) forms[k] = MolecularFormula.parseOrThrow(formulas[k]);

        final MolecularFormulaPacker packer = MolecularFormulaPacker.newPackerFor(forms);

        for (MolecularFormula f : forms) {
            assertEquals(f, packer.decode(packer.encode(f)));
        }
    }

    @Test
    public void packerShouldBehaveLikeMolecularFormulas() {
        final MolecularFormula[] formulas = new MolecularFormula[]{
                MolecularFormula.parseOrThrow("C6H12O6"), MolecularFormula.parseOrThrow("C8N2H15PS"),
                MolecularFormula.parseOrThrow("C"), MolecularFormula.parseOrThrow("CBrI7"),
                MolecularFormula.parseOrThrow("C5H7Br3I3F2")
        };
        final MolecularFormulaPacker packer = MolecularFormulaPacker.newPackerFor(formulas);
        for (MolecularFormula f : formulas) {
            final long packed = packer.encode(f);
            assertEquals(f, packer.decode(packer.encode(f)));
            assertEquals(f.rdbe(), packer.rdbe(packed), 1e-6);
            assertEquals(f.doubledRDBE(), packer.doubledRDBE(packed));
            assertEquals(f.getMass(), packer.getMass(packed), 1e-6);
            assertEquals(f.atomCount(), packer.atomCount(packed));
            assertEquals(f.hetero2CarbonRatio(), packer.hetero2CarbonRatio(packed), 1e-6);
            assertEquals(f.heteroWithoutOxygenToCarbonRatio(), packer.heteroWithoutOxygenToCarbonRatio(packed), 1e-6);
            assertEquals(f.hydrogen2CarbonRatio(), packer.hydrogen2CarbonRatio(packed), 1e-6);
            assertEquals(f.hetero2OxygenRatio(), packer.hetero2OxygenRatio(packed), 1e-6);
            assertEquals(f.numberOfHydrogens(), packer.numberOfHydrogens(packed));
            assertEquals(f.numberOfCarbons(), packer.numberOfCarbons(packed));
            assertEquals(f.numberOfNitrogens(), packer.numberOfNitrogens(packed));
            assertEquals(f.numberOfOxygens(), packer.numberOfOxygens(packed));
            assertEquals(f.isCHNO(), packer.isCHNO(packed));
            assertEquals(f.isCHNOPS(), packer.isCHNOPS(packed));
            assertEquals(f.getIntMass(), packer.getIntMass(packed));
            assertEquals(f.maybeCharged(), packer.maybeCharged(packed));
            assertEquals(f.add(MolecularFormula.parseOrThrow("C4H4O6")),
                    packer.decode(packer.add(packed, packer.encode(MolecularFormula.parseOrThrow("C4H4O6")))));
            final MolecularFormula g = MolecularFormula.parseOrThrow("C27H44N7O13P6S5Br5I9F4");
            assertEquals(g.subtract(f), packer.decode(packer.subtract(packer.encode(g), packed)));
            assertEquals(f.isSubtractable(MolecularFormula.parseOrThrow("C4H2")), packer.isSubtractable(packed,
                    packer.encode(MolecularFormula.parseOrThrow("C4H2"))));
        }
    }

    final String[] SAMPLE = new String[]{"C23H38O2", "C9H13N3O", "C6H5N3O4", "C24H32O7", "C17H16O4", "C18H31N2O27S4",
            "C40H56", "C7H13NO4", "C44H69NO12", "C24H42O4", "C20H18O4", "C20H26Br2O2", "C32H57N5O9", "C8H15NO9S2",
            "C14H20N2O2", "C11H11N5", "C3H7NO6S", "C20H21NO3", "C11H19NOS", "C10H12ClNO4", "C28H48O", "C36H63N12O6",
            "C33H46N2O10", "C45H54N8O10", "C15H14O10", "C9H11NO6", "C22H34O5", "C10H23O2PS2", "C13H16N10O5S",
            "C25H27N9O8S2", "C18H26ClN3", "C20H16NO4", "C55H74N4O5", "C12H26O", "C18H25O5P", "C22H24N2O4",
            "C15H22BrNO", "C17H14N2O7", "C12H8O", "C10H18O", "C15H18N2O6", "C15H21N3O2", "C37H48O10", "C21H30N4O5S",
            "C17H14O3", "C26H38N7O17P3S2", "C15H22N5O7P", "C5H15NO4P", "C13H18N5O8P", "C19H14O5", "C25H40NO8P",
            "C8H15O3PS", "C8H11NO2", "C10H16O20S4", "C10H7Cl5O", "C21H26Cl2N2O6", "C18H19NO3", "C30H42", "C7H8",
            "C13H7Cl4NO2", "C23H37NO5", "C36H48O36", "C25H26O12", "C21H24O6", "C12H3Cl5O", "C50H71N13O12", "C5H9NO4S",
            "C6H9N5", "C12H9ClO2", "C24H26N2O7S", "C20H16ClF3N2O2S", "C24H26O8", "C14H18N5O11P", "C25H39NO6S",
            "C30H50O25", "C29H38O9", "C14H20N2O16P2", "C8H12O5", "C2HCl3O", "C3H2O5", "C30H52", "C32H46O9", "C5H11NO2S",
            "C19H34O2", "C12H19NO13S", "C9H16N2O4", "C8H17N", "C18H34O3", "C40H50O2", "C7H15N3O5", "C28H36N4O4",
            "C21H27N3O7S", "C8H7NO2", "C41H65NO10", "C22H32O8", "C25H48N6O8", "C7H14N2O4", "C8H16N2O4",
            "C15H15ClN4O6S", "C9H14N2O7P", "C10H10N4O6", "C7H16O7", "C10H9NOS", "C21H35N3O8S", "C22H22O11",
            "C7H11NO5", "C19H24O6S", "C31H48N7O17P3S", "C28H36N2O4", "C14H8N2O8", "C13H28N4O7", "C5H11N3O2",
            "C14H18N6O7S", "C37H47NO12", "C26H37NO6", "C8H6N4O5", "C40H52O2", "C13H15O13S", "C23H34O2", "C10H18O3",
            "C15H10N2O2", "C11H8N2O3S2", "C24H39N8O18P3S", "C8H15N3O4", "C16H30O", "C8H14N4S", "C15H17NO4",
            "C16H22N2O2", "C10H18N4O4S3", "C8H11NO", "C12H8Cl4N2", "C21H16NO5", "C40H50O18", "C17H19N5O6S",
            "C24H36O18", "C23H31ClO6", "C24H27NO7", "C33H42N4O6", "C3H6N2O4", "C13H16N2O6", "C20H25N3O7S",
            "C7H15N5O4", "C23H20F2N5O5PS", "C3H10NO4P", "C33H52O5", "C52H78O3", "C12H19N3O8S", "C27H43NO3",
            "C33H40O21", "C8H10N2O4", "C12H18O3", "C11H13N4", "C17H30O7", "C16H23N5O6", "C29H36O11", "C7H12N4O3S2",
            "C24H28O2", "C12H24N9P3", "C42H72O14", "C6H6N4O4", "C12H7ClO2", "C22H31FO3", "C28H29F2N3O", "C3H9O5P",
            "C4H12NO4P", "C27H48O6", "C23H22O7", "C16H10", "C13H20N5O16P3", "N2H2", "C31H50N7O19P3S", "C12H16O14",
            "C16H18O9", "C10H13Cl2FN2O2S2", "C7H8N2O2", "C48H69NO12", "C7H10O4", "C3H8O10P2", "C6H4N2O4",
            "C11H16N4O4", "C38H50O6", "C14H13NO4", "C15H26N2", "C16H14Cl2N2O2", "C30H27O12", "C17H20O4",
            "C28H37ClO7", "C19H20N2O4", "C43H73O6P", "C15H11ClF3NO4", "C19H12", "C19H15FN2O4", "C6H12O",
            "C10H15NO3", "C19H21N5O2", "C38H60O9", "C8H8O7", "C42H66O15", "C4H11O4P", "C33H52O9"};


    @Test
    public void testFormulaSet() throws IOException {
        final MolecularFormula[] formulas = new MolecularFormula[SAMPLE.length];
        for (int k=0; k < formulas.length; ++k) formulas[k] = MolecularFormula.parseOrThrow(SAMPLE[k]);
        final MolecularFormulaSet set = new MolecularFormulaSet(ChemicalAlphabet.alphabetFor(formulas));
        for (MolecularFormula f : formulas) set.add(f);

        for (MolecularFormula f : formulas) assertEquals("expect " + f + " to be contained", true, set.contains(f));
        assertEquals(false, set.contains(MolecularFormula.parseOrThrow("C7H14NO7PS")));
        assertEquals(false, set.contains(MolecularFormula.parseOrThrow("C7H14F512")));
        final ByteArrayOutputStream wr = new ByteArrayOutputStream(1024);
        set.store(wr);
        final ByteArrayInputStream ir = new ByteArrayInputStream(wr.toByteArray());
        final MolecularFormulaSet set2 = MolecularFormulaSet.load(ir);
        for (MolecularFormula f : formulas) assertEquals("expect " + f + " to be contained", true, set2.contains(f));
        assertEquals(false, set2.contains(MolecularFormula.parseOrThrow("C7H14NO7PS")));
        assertEquals(false, set2.contains(MolecularFormula.parseOrThrow("C7H14F512")));
    }
}
