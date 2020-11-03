/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.io;

import de.unijena.bioinf.ms.gui.io.csv.GeneralCSVDialog;
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
