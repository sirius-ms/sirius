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

package de.unijena.bioinf.fingerid.fingerprints;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.AbstractFingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.ICountFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.Map;

public class ShortestPathFingerprinter extends AbstractFingerprinter implements IFingerprinter {

    public final static String[] smartstrings2 = new String[]{"Oc([c])[c]C", "C[c]n", "n(c[cH])[c]", "Cc([c])n", "OC=[N]", "OCC[CH][CH2]", "N[c][cH]", "O=C[O]", "[NH]", "CCN=[C]C", "NC(C)[CH2]", "N=C([C]C)O", "[C]=O", "C=N", "CC(C[CH2])N", "NCC[CH]", "N(C[CH2])C[CH]", "OC[c]", "CO[c][cH]", "O=C[c]", "c(c[cH])([cH])n", "Oc(c[cH])[cH]", "NC[c]", "c([cH])(co)[c]", "OC[CH]N", "NC[c][cH]", "COc(c[cH])[c]", "O=CCC[CH2]", "NCC[c][cH]", "NCc(c[cH])[c]", "NCC[c]", "c(c(C)[c])[c]O", "C[n]", "CC(CO)[N]", "OC[CH]", "Cc(cn)[c]", "NC[C]O", "c([cH])o[c]", "O=C[N]", "CO[CH2]", "CN[c]", "[c]O", "[c]N", "CCN=[C]", "O=C[C]", "CC[C]=O", "[n]", "Oc([cH])c(C)[c]", "O[CH]", "c([c]C)n[c]", "O[CH][CH2]", "CO[c]", "OC[CH2]", "NC[CH]", "OCC[CH2]", "NCc([cH])[c]", "CN=[C]O", "OCCC[c]", "NCCC[CH]", "[CH2]O", "CC([N])=O", "NC[CH][CH2]", "OC[CH][CH2]", "C[C]=N", "OC[c][cH]", "[OH]", "N=C(C)[O]", "N[CH]", "C([CH2])[CH]N", "[CH2]C[CH]O", "C[NH]", "c([cH])(cn)[c][c]", "O=CC[CH2]", "n(c[c]C)c[c]", "c(c[c])c=O", "O=C[CH]C", "O=C[CH2]", "CCO[c]", "c([cH])(c[c])O[CH2]", "N([CH2])[CH]", "n(c[c])c([cH])[c]", "CN=[C]", "NC[C]=O", "N[CH][CH2]", "[cH]n", "O=C[CH]", "O=C[N][CH2]", "N", "[S]", "[o]", "O=[c]", "[nH]", "[CH](C[CH2])[C]O"};

    public final static String[] smartstrings = new String[]{"CC[C]=O", "C=N", "o", "[n]", "CC([N])=O", "[OH]", "N=C(C)[O]", "[S]", "[o]", "[Cl]", "P", "c[N]", "c[O]", "O=C[O]"};

    public final static char[] elems = new char[]{'O', 'N', 'N', 'N', 'C', 'O', 'N', 'C', 'N', 'N', 'N', 'N', 'C', 'C', 'N', 'N', 'N', 'O', 'O', 'O', 'N', 'O', 'N', 'O', 'N', 'N', 'O', 'O', 'N', 'N', 'N', 'O', 'C', 'N', 'O', 'N', 'N', 'O', 'C', 'O', 'N', 'C', 'C', 'N', 'O', 'O', 'N', 'O', 'C', 'N', 'O', 'O', 'O', 'N', 'O', 'N', 'N', 'O', 'N', 'C', 'N', 'N', 'O', 'N', 'O', 'O', 'N', 'C', 'N', 'O', 'C', 'N', 'O', 'N', 'O', 'O', 'O', 'O', 'O', 'N', 'N', 'N', 'N', 'N', 'C', 'O', 'N', 'N', 'S', 'O', 'C', 'N', 'O'};

    // O=8
    // N=7
    // S=16
    // C=6

    public final static int[] atomicnumbers = new int[]{8,7,8,7,8,8,8,16,8, 17, 15, 7, 8, 6};

    public final static int[] atomicnumbers2 = new int[]{8, 7, 8, 7, 6, 8, 7, 6, 7, 7, 7, 7, 6, 6, 7, 7, 7, 8, 8, 8, 7, 8, 7, 8, 7, 7, 8, 8, 7, 7, 7, 8, 6, 7, 8, 7, 7, 8, 6, 8, 7, 6, 6, 7, 8, 8, 7, 8, 6, 7, 8, 8, 8, 7, 8, 7, 7, 8, 7, 6, 7, 7, 8, 7, 8, 8, 7, 6, 7, 8, 6, 7, 8, 7, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 8, 7, 7, 16, 8, 6, 7, 8};


    public ShortestPathFingerprinter() {
    }


    @Override
    public IBitFingerprint getBitFingerprint(IAtomContainer atomContainer) throws CDKException {
        return null;
    }

    @Override
    public ICountFingerprint getCountFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public Map<String, Integer> getRawFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public int getSize() {
        return (smartstrings.length*smartstrings.length)/2 - smartstrings.length;
    }
}
