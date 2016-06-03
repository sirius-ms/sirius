/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.ChemistryBase.chem.ElectronIonization;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;

public class GCMSMissingValueValidator extends MissingValueValidator {

    @Override
    public MutableMs2Experiment validate(Ms2Experiment originalInput, Warning warn, boolean repair) throws InvalidException {
        final MutableMs2Experiment input = new MutableMs2Experiment(originalInput);
        if (input.getMs1Spectra() == null || input.getMs1Spectra().isEmpty()) throw new InvalidException("Missing MS1 spectra");
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkNeutralMass(warn, repair, input);
        return input;
    }

    @Override
    protected void checkIonization(Warning warn, boolean repair, MutableMs2Experiment input) {
        if (input.getPrecursorIonType() == null) {
            throwOrWarn(warn, repair, "No ionization is given");
            final Ionization ion = new ElectronIonization();
            input.setPrecursorIonType(PeriodicTable.getInstance().getPrecursorIonTypeForEI());
        }
    }

    @Override
    protected void checkNeutralMass(Warning warn, boolean repair, MutableMs2Experiment input) {
        if (input.getMoleculeNeutralMass() == 0 || !validDouble(input.getMoleculeNeutralMass(), false)) {
            if (input.getMolecularFormula() != null) {
                throwOrWarn(warn, repair, "Neutral mass is missing, but formula known.");
                input.setMoleculeNeutralMass(input.getMolecularFormula().getMass());
            } else if (input.getIonMass() != 0){
                throwOrWarn(warn, repair, "Neutral mass is missing, but ion mass known.");
                input.setMoleculeNeutralMass(input.getPrecursorIonType().precursorMassToNeutralMass(input.getIonMass()));
            }
        }
    }

    private boolean validDouble(double val, boolean mayNegative) {
        return !Double.isInfinite(val) && !Double.isNaN(val) && (mayNegative || val >= 0d);
    }

}
