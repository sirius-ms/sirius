/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.retention;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.chemdb.LogPEstimator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

public class SimpleCompound implements PredictableCompound {

    protected final InChI inchi;
    protected final String smiles;
    protected double retentionTime;
    protected final IAtomContainer molecule;
    protected double xlogp;

    public SimpleCompound(InChI inchi, String smiles, IAtomContainer molecule, double retentionTime) {
        this.inchi = inchi;
        this.molecule = molecule;
        this.smiles = smiles;
        this.retentionTime = retentionTime;

        // xlogp calculation
        try {
            this.xlogp = new LogPEstimator().prepareMolAndComputeLogP(molecule.clone());;
        } catch (CDKException | CloneNotSupportedException e) {
            e.printStackTrace();
        }

    }

    public double getRetentionTime() {
        return retentionTime;
    }

    @Override
    public InChI getInChI() {
        return inchi;
    }

    public String getSmiles() {
        return smiles;
    }

    @Override
    public IAtomContainer getMolecule() {
        return molecule;
    }
}
