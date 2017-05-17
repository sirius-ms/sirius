package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.sirius.gui.load.csv.GeneralCSVDialog;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.List;

public class CsvFields {

    public static class InChIField extends GeneralCSVDialog.Field implements GeneralCSVDialog.FieldCheck {
        public InChIField(int minNumber, int maxNumber) {
            super("InChI", minNumber, maxNumber, null);
            check = this;
        }

        @Override
        public int check(List<String> values, int column) {
            for (String line : values) {
                if (!line.startsWith("InChI=")) return -1;
            }
            return 100;
        }
    }

    public static class SMILESField extends GeneralCSVDialog.Field implements GeneralCSVDialog.FieldCheck {
        public SMILESField(int minNumber, int maxNumber) {
            super("SMILES", minNumber, maxNumber, null);
            check = this;
        }

        @Override
        public int check(List<String> values, int column) {
            int likely = 50;
            try {
                final SmilesParser smp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                for (String line : values) {
                    final IAtomContainer m = smp.parseSmiles(line);
                    IMolecularFormula im = MolecularFormulaManipulator.getMolecularFormula(m);

                    double n = MolecularFormulaManipulator.getElementCount(im, Elements.CARBON) + MolecularFormulaManipulator.getElementCount(im, Elements.HYDROGEN) + MolecularFormulaManipulator.getElementCount(im, Elements.OXYGEN) + MolecularFormulaManipulator.getElementCount(im, Elements.NITROGEN);
                    double t = m.getAtomCount();
                    if (n/t <= 0.75) likely -= 10;
                }
                System.err.println(likely);
                return likely;
            } catch (InvalidSmilesException e) {
                return -1;
            }
        }
    }

    public static class IDField extends GeneralCSVDialog.Field implements GeneralCSVDialog.FieldCheck {
        public IDField(int minNumber, int maxNumber) {
            super("ID", minNumber, maxNumber, null);
            check = this;
        }

        @Override
        public int check(List<String> values, int column) {
            return 4-column;
        }


    }

}
