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
import de.unijena.bioinf.ChemistryBase.fp.SubstructureProperty;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;

/**
 * This class lists 1003 SMARTS strings which are mined from a molecular structure database and occur frequently in our training data.
 * - we select all molecular structures from the bio database and check if they occur frequently as substructure in other biomolecules
 * - we then transform their SMILES pattern into a SMARTS pattern:
 *      - we fix the number of implicit hydrogens for each "inner" atom (atoms with degree &gt; 1), such that the exact connectivity of the substructure is enforced. This means: the substructure is only allowed to be attached to a molecule by replacing one of its outer atoms
 *      - outer atoms (i.e. atoms with degree 1) do allowed to have a variable number of hydrogens and can be replaced by other hetero atoms/elements. Their bond to the inner atoms is variable, too.
 *
 * - for all such SMARTS strings that occur at least 50 times in our training data, we train a model and compute F1 via crossvalidation. We also compare each MP against all existing MPs. We now iterate:
 *  - if F1 is above 0.25 and the difference between F1 and Tanimoto between the MP and the closest existing MP is above 0.1, we choose this molecular property
 *  - afterwards we recompute the Tanimotos by considering this new MP
 *
 *
 *  This fingerprint assumes implicit hydrogens!
 */
public class BiosmartsFingerprinter extends SubstructureFingerprinter {

    protected static String[] getSMARTSPATTERN(CdkFingerprintVersion.USED_FINGERPRINTS type) {
        final CdkFingerprintVersion CDK = CdkFingerprintVersion.getExtended();
        final int offset = CDK.getOffsetFor(type);

        final int length = type.length;

        String[] SMARTS = new String[length];
        for (int k=0; k < length; ++k) {
            SMARTS[k] = ((SubstructureProperty)CDK.getMolecularProperty(offset+k)).getSmarts();
        }

        return SMARTS;

    }

    private final static String[] SMARTS_PATTERN = getSMARTSPATTERN(CdkFingerprintVersion.USED_FINGERPRINTS.BIOSMARTS);


    public BiosmartsFingerprinter() {
        super(SMARTS_PATTERN);
    }
}
