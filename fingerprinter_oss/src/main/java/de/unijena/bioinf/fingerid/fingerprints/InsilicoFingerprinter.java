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

package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smarts.SmartsPattern;

import java.util.BitSet;

import static de.unijena.bioinf.fingerid.fingerprints.BiosmartsFingerprinter.getSMARTSPATTERN;

/**
 * This class lists SMARTS patterns obtained from insilico fragmentation.
 * - we used epimetheus to fragment all molecules in our training data
 * - each bond break splits the molecule into two connection components. We transform both components into SMARTS strings
 * - transformation is done in a way that atoms which are not part of a bond break are fixed (their number of hydrogens is specified), while other atoms are variable (only the number of connections is specified, but they could be connected to hydrogens or heteroatoms)
 * - we collect all SMARTS that match at least 50 structures in our data
 * - we train a DNN to predict all SMARTS in crossvalidation to obtain MCC values
 * - we merge them with all existing fingerprints and iteratively add the FP with highest MCC to the set of used FPs for which the highest Tanimoto to all already used FPs is at least 0.1 lower than the MCC value
 *
 *
 *  This fingerprint assumes implicit hydrogens!
 */
public class InsilicoFingerprinter extends SubstructureFingerprinter {

    private final static String[] SMARTS_PATTERN = getSMARTSPATTERN(CdkFingerprintVersion.USED_FINGERPRINTS.INSILICO);


    public InsilicoFingerprinter() {
        super(SMARTS_PATTERN);
    }

}
