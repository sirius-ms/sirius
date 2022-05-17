

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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.fingerid.fingerprints.FixedMACCSFingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;

import java.util.BitSet;

public class FormulaBits {

    public final static int[] MACCS = new int[]{28, 41, 139, 141, 145, 158, 160, 163};
    public final int PUBCHEM_FSIZE = 114;
    public final static int[] KLEKOTHA_ROTH = new int[]{2974, 4031, 4073, 4079, 4330, 4842};

    public MaskedFingerprintVersion removeFormulaBits(MaskedFingerprintVersion version) {
        final MaskedFingerprintVersion.Builder b = version.modify();
        final CdkFingerprintVersion cdk = (CdkFingerprintVersion)version.getMaskedFingerprintVersion();

        // remove MACCS
        if (cdk.isFingerprintTypeInUse(CdkFingerprintVersion.USED_FINGERPRINTS.MACCS)) {
            int offset = cdk.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.MACCS);
            for (int index : MACCS) b.disable(offset+index);
        }

        // remove PUBCHEM
        if (cdk.isFingerprintTypeInUse(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM)) {
            int offset = cdk.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM);
            for (int i=0; i < PUBCHEM_FSIZE; ++i) {
                b.disable(i+offset);
            }
        }
        // remove KLEKOTHA ROTH
        if (cdk.isFingerprintTypeInUse(CdkFingerprintVersion.USED_FINGERPRINTS.KLEKOTA_ROTH)) {
            int offset = cdk.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.KLEKOTA_ROTH);
            for (int index : KLEKOTHA_ROTH) b.disable(offset+index);
        }
        return b.toMask();
    }

    public void restoreFingerprints(Fingerprinter fingerprinter, double[] completeFp, MolecularFormula formula) {
        final boolean[] fp = new boolean[completeFp.length];
        restoreFingerprints(fingerprinter, fp, formula);
        int offset = 0;
        for (IFingerprinter f : fingerprinter.getFingerprinters()) {
            if (f instanceof MACCSFingerprinter || f instanceof FixedMACCSFingerprinter) {
                for (int index : MACCS) {
                    completeFp[offset+index] = fp[offset+index] ? 1 : 0;
                }
            } else if (f instanceof PubchemFingerprinter) {
                for (int i=0; i < PUBCHEM_FSIZE; ++i) {
                    completeFp[offset+i] = fp[offset+i] ? 1 : 0;
                }
            } else if (f instanceof KlekotaRothFingerprinter) {
                for (int index : KLEKOTHA_ROTH) {
                    completeFp[offset+index] = fp[offset+index] ? 1 : 0;
                }
            }

            offset += f.getSize();
        }
    }

    public void restoreFingerprints(Fingerprinter fingerprinter, boolean[] completeFp, MolecularFormula formula) {
        int offset = 0;
        for (IFingerprinter f : fingerprinter.getFingerprinters()) {
            if (f instanceof MACCSFingerprinter || f instanceof FixedMACCSFingerprinter) {
                restoreMACCS(completeFp, formula, offset);
            } else if (f instanceof PubchemFingerprinter) {
                restorePubchem(completeFp, formula, offset);
            } else if (f instanceof KlekotaRothFingerprinter) {
                restoreKlekotha(completeFp, formula, offset);
            }
            offset += f.getSize();
        }
    }


    private void restoreKlekotha(boolean[] completeFp, MolecularFormula formula, int i) {
        completeFp[i++] = formula.numberOf("C")>=1; // TODO: This is not a formula bit... C = aliphatic C
        completeFp[i++] = formula.numberOf("F")>=1;
        completeFp[i++] = formula.numberOf("I")>=1;
        completeFp[i++] = formula.numberOf("N")>=1; // TODO: This is not a formula bit... N = aliphatic N
        completeFp[i++] = formula.numberOf("O")>=1; // TODO: This is not a formula bit... O = aliphatic O
        completeFp[i++] = formula.numberOf("S")>=1;
    }

    private void restoreMACCS(boolean[] completeFp, MolecularFormula formula, int i) {
        completeFp[i++] = formula.numberOf("P")>=1;
        completeFp[i++] = formula.numberOf("F")>=1;
        completeFp[i++] = formula.numberOf("O")>=4;
        completeFp[i++] = formula.numberOf("N")>=2;
        completeFp[i++] = formula.numberOf("O")>=3;
        completeFp[i++] = formula.numberOf("O")>=2;
        completeFp[i++] = formula.numberOf("N")>=1;
        completeFp[i++] = formula.numberOf("O")>=1;
    }
    public static BitSet getMaccsBits(MolecularFormula formula) {
        final BitSet bitset = new BitSet(8);
        bitset.set(0, formula.numberOf("P")>=1);
        bitset.set(1, formula.numberOf("F")>=1);
        bitset.set(2, formula.numberOf("O")>=4);
        bitset.set(3, formula.numberOf("N")>=2);
        bitset.set(4, formula.numberOf("O")>=3);
        bitset.set(5, formula.numberOf("O")>=2);
        bitset.set(6, formula.numberOf("N")>=1);
        bitset.set(7, formula.numberOf("O")>=1);
        return bitset;
    }

    public void removeFingerprintBitsFrom(Fingerprinter fingerprinter, Mask mask) {
        int offset = 0;
        for (IFingerprinter f : fingerprinter.getFingerprinters()) {
            if (f instanceof MACCSFingerprinter || f instanceof FixedMACCSFingerprinter) {
                for (int index : MACCS) {
                    mask.disableFingerprint(offset + index);
                }
            } else if (f instanceof PubchemFingerprinter) {
                for (int i=0; i < PUBCHEM_FSIZE; ++i) {
                    mask.disableFingerprint(offset + i);
                }
            } else if (f instanceof KlekotaRothFingerprinter) {
                for (int index : KLEKOTHA_ROTH) {
                    mask.disableFingerprint(offset + index);
                }
            }

            offset += f.getSize();
        }
    }

    private void restorePubchem(boolean[] completeFp, MolecularFormula formula, int i) {
        final boolean[] pubchem = getPubchem(formula);
        System.arraycopy(pubchem, 0, completeFp, i, PUBCHEM_FSIZE);
    }

    public static boolean[] getPubchem(MolecularFormula formula) {
        boolean[] fp = new boolean[115];
        int b = 0;
        if (formula.numberOf("H") >= 4) fp[b] = true;
        b = 1;
        if (formula.numberOf("H") >= 8) fp[b] = true;
        b = 2;
        if (formula.numberOf("H") >= 16) fp[b] = true;
        b = 3;
        if (formula.numberOf("H") >= 32) fp[b] = true;
        b = 4;
        if (formula.numberOf("Li") >= 1) fp[b] = true;
        b = 5;
        if (formula.numberOf("Li") >= 2) fp[b] = true;
        b = 6;
        if (formula.numberOf("B") >= 1) fp[b] = true;
        b = 7;
        if (formula.numberOf("B") >= 2) fp[b] = true;
        b = 8;
        if (formula.numberOf("B") >= 4) fp[b] = true;
        b = 9;
        if (formula.numberOf("C") >= 2) fp[b] = true;
        b = 10;
        if (formula.numberOf("C") >= 4) fp[b] = true;
        b = 11;
        if (formula.numberOf("C") >= 8) fp[b] = true;
        b = 12;
        if (formula.numberOf("C") >= 16) fp[b] = true;
        b = 13;
        if (formula.numberOf("C") >= 32) fp[b] = true;
        b = 14;
        if (formula.numberOf("N") >= 1) fp[b] = true;
        b = 15;
        if (formula.numberOf("N") >= 2) fp[b] = true;
        b = 16;
        if (formula.numberOf("N") >= 4) fp[b] = true;
        b = 17;
        if (formula.numberOf("N") >= 8) fp[b] = true;
        b = 18;
        if (formula.numberOf("O") >= 1) fp[b] = true;
        b = 19;
        if (formula.numberOf("O") >= 2) fp[b] = true;
        b = 20;
        if (formula.numberOf("O") >= 4) fp[b] = true;
        b = 21;
        if (formula.numberOf("O") >= 8) fp[b] = true;
        b = 22;
        if (formula.numberOf("O") >= 16) fp[b] = true;
        b = 23;
        if (formula.numberOf("F") >= 1) fp[b] = true;
        b = 24;
        if (formula.numberOf("F") >= 2) fp[b] = true;
        b = 25;
        if (formula.numberOf("F") >= 4) fp[b] = true;
        b = 26;
        if (formula.numberOf("Na") >= 1) fp[b] = true;
        b = 27;
        if (formula.numberOf("Na") >= 2) fp[b] = true;
        b = 28;
        if (formula.numberOf("Si") >= 1) fp[b] = true;
        b = 29;
        if (formula.numberOf("Si") >= 2) fp[b] = true;
        b = 30;
        if (formula.numberOf("P") >= 1) fp[b] = true;
        b = 31;
        if (formula.numberOf("P") >= 2) fp[b] = true;
        b = 32;
        if (formula.numberOf("P") >= 4) fp[b] = true;
        b = 33;
        if (formula.numberOf("S") >= 1) fp[b] = true;
        b = 34;
        if (formula.numberOf("S") >= 2) fp[b] = true;
        b = 35;
        if (formula.numberOf("S") >= 4) fp[b] = true;
        b = 36;
        if (formula.numberOf("S") >= 8) fp[b] = true;
        b = 37;
        if (formula.numberOf("Cl") >= 1) fp[b] = true;
        b = 38;
        if (formula.numberOf("Cl") >= 2) fp[b] = true;
        b = 39;
        if (formula.numberOf("Cl") >= 4) fp[b] = true;
        b = 40;
        if (formula.numberOf("Cl") >= 8) fp[b] = true;
        b = 41;
        if (formula.numberOf("K") >= 1) fp[b] = true;
        b = 42;
        if (formula.numberOf("K") >= 2) fp[b] = true;
        b = 43;
        if (formula.numberOf("Br") >= 1) fp[b] = true;
        b = 44;
        if (formula.numberOf("Br") >= 2) fp[b] = true;
        b = 45;
        if (formula.numberOf("Br") >= 4) fp[b] = true;
        b = 46;
        if (formula.numberOf("I") >= 1) fp[b] = true;
        b = 47;
        if (formula.numberOf("I") >= 2) fp[b] = true;
        b = 48;
        if (formula.numberOf("I") >= 4) fp[b] = true;
        b = 49;
        if (formula.numberOf("Be") >= 1) fp[b] = true;
        b = 50;
        if (formula.numberOf("Mg") >= 1) fp[b] = true;
        b = 51;
        if (formula.numberOf("Al") >= 1) fp[b] = true;
        b = 52;
        if (formula.numberOf("Ca") >= 1) fp[b] = true;
        b = 53;
        if (formula.numberOf("Sc") >= 1) fp[b] = true;
        b = 54;
        if (formula.numberOf("Ti") >= 1) fp[b] = true;
        b = 55;
        if (formula.numberOf("V") >= 1) fp[b] = true;
        b = 56;
        if (formula.numberOf("Cr") >= 1) fp[b] = true;
        b = 57;
        if (formula.numberOf("Mn") >= 1) fp[b] = true;
        b = 58;
        if (formula.numberOf("Fe") >= 1) fp[b] = true;
        b = 59;
        if (formula.numberOf("Co") >= 1) fp[b] = true;
        b = 60;
        if (formula.numberOf("Ni") >= 1) fp[b] = true;
        b = 61;
        if (formula.numberOf("Cu") >= 1) fp[b] = true;
        b = 62;
        if (formula.numberOf("Zn") >= 1) fp[b] = true;
        b = 63;
        if (formula.numberOf("Ga") >= 1) fp[b] = true;
        b = 64;
        if (formula.numberOf("Ge") >= 1) fp[b] = true;
        b = 65;
        if (formula.numberOf("As") >= 1) fp[b] = true;
        b = 66;
        if (formula.numberOf("Se") >= 1) fp[b] = true;
        b = 67;
        if (formula.numberOf("Kr") >= 1) fp[b] = true;
        b = 68;
        if (formula.numberOf("Rb") >= 1) fp[b] = true;
        b = 69;
        if (formula.numberOf("Sr") >= 1) fp[b] = true;
        b = 70;
        if (formula.numberOf("Y") >= 1) fp[b] = true;
        b = 71;
        if (formula.numberOf("Zr") >= 1) fp[b] = true;
        b = 72;
        if (formula.numberOf("Nb") >= 1) fp[b] = true;
        b = 73;
        if (formula.numberOf("Mo") >= 1) fp[b] = true;
        b = 74;
        if (formula.numberOf("Ru") >= 1) fp[b] = true;
        b = 75;
        if (formula.numberOf("Rh") >= 1) fp[b] = true;
        b = 76;
        if (formula.numberOf("Pd") >= 1) fp[b] = true;
        b = 77;
        if (formula.numberOf("Ag") >= 1) fp[b] = true;
        b = 78;
        if (formula.numberOf("Cd") >= 1) fp[b] = true;
        b = 79;
        if (formula.numberOf("In") >= 1) fp[b] = true;
        b = 80;
        if (formula.numberOf("Sn") >= 1) fp[b] = true;
        b = 81;
        if (formula.numberOf("Sb") >= 1) fp[b] = true;
        b = 82;
        if (formula.numberOf("Te") >= 1) fp[b] = true;
        b = 83;
        if (formula.numberOf("Xe") >= 1) fp[b] = true;
        b = 84;
        if (formula.numberOf("Cs") >= 1) fp[b] = true;
        b = 85;
        if (formula.numberOf("Ba") >= 1) fp[b] = true;
        b = 86;
        if (formula.numberOf("Lu") >= 1) fp[b] = true;
        b = 87;
        if (formula.numberOf("Hf") >= 1) fp[b] = true;
        b = 88;
        if (formula.numberOf("Ta") >= 1) fp[b] = true;
        b = 89;
        if (formula.numberOf("W") >= 1) fp[b] = true;
        b = 90;
        if (formula.numberOf("Re") >= 1) fp[b] = true;
        b = 91;
        if (formula.numberOf("Os") >= 1) fp[b] = true;
        b = 92;
        if (formula.numberOf("Ir") >= 1) fp[b] = true;
        b = 93;
        if (formula.numberOf("Pt") >= 1) fp[b] = true;
        b = 94;
        if (formula.numberOf("Au") >= 1) fp[b] = true;
        b = 95;
        if (formula.numberOf("Hg") >= 1) fp[b] = true;
        b = 96;
        if (formula.numberOf("Tl") >= 1) fp[b] = true;
        b = 97;
        if (formula.numberOf("Pb") >= 1) fp[b] = true;
        b = 98;
        if (formula.numberOf("Bi") >= 1) fp[b] = true;
        b = 99;
        if (formula.numberOf("La") >= 1) fp[b] = true;
        b = 100;
        if (formula.numberOf("Ce") >= 1) fp[b] = true;
        b = 101;
        if (formula.numberOf("Pr") >= 1) fp[b] = true;
        b = 102;
        if (formula.numberOf("Nd") >= 1) fp[b] = true;
        b = 103;
        if (formula.numberOf("Pm") >= 1) fp[b] = true;
        b = 104;
        if (formula.numberOf("Sm") >= 1) fp[b] = true;
        b = 105;
        if (formula.numberOf("Eu") >= 1) fp[b] = true;
        b = 106;
        if (formula.numberOf("Gd") >= 1) fp[b] = true;
        b = 107;
        if (formula.numberOf("Tb") >= 1) fp[b] = true;
        b = 108;
        if (formula.numberOf("Dy") >= 1) fp[b] = true;
        b = 109;
        if (formula.numberOf("Ho") >= 1) fp[b] = true;
        b = 110;
        if (formula.numberOf("Er") >= 1) fp[b] = true;
        b = 111;
        if (formula.numberOf("Tm") >= 1) fp[b] = true;
        b = 112;
        if (formula.numberOf("Yb") >= 1) fp[b] = true;
        b = 113;
        if (formula.numberOf("Tc") >= 1) fp[b] = true;
        b = 114;
        if (formula.numberOf("U") >= 1) fp[b] = true;
        return fp;
    }
    public static BitSet getPubchemBitset(MolecularFormula formula) {
        BitSet fp = new BitSet(115);
        int b = 0;
        if (formula.numberOf("H") >= 4) fp.set(b);
        b = 1;
        if (formula.numberOf("H") >= 8) fp.set(b);
        b = 2;
        if (formula.numberOf("H") >= 16) fp.set(b);
        b = 3;
        if (formula.numberOf("H") >= 32) fp.set(b);
        b = 4;
        if (formula.numberOf("Li") >= 1) fp.set(b);
        b = 5;
        if (formula.numberOf("Li") >= 2) fp.set(b);
        b = 6;
        if (formula.numberOf("B") >= 1) fp.set(b);
        b = 7;
        if (formula.numberOf("B") >= 2) fp.set(b);
        b = 8;
        if (formula.numberOf("B") >= 4) fp.set(b);
        b = 9;
        if (formula.numberOf("C") >= 2) fp.set(b);
        b = 10;
        if (formula.numberOf("C") >= 4) fp.set(b);
        b = 11;
        if (formula.numberOf("C") >= 8) fp.set(b);
        b = 12;
        if (formula.numberOf("C") >= 16) fp.set(b);
        b = 13;
        if (formula.numberOf("C") >= 32) fp.set(b);
        b = 14;
        if (formula.numberOf("N") >= 1) fp.set(b);
        b = 15;
        if (formula.numberOf("N") >= 2) fp.set(b);
        b = 16;
        if (formula.numberOf("N") >= 4) fp.set(b);
        b = 17;
        if (formula.numberOf("N") >= 8) fp.set(b);
        b = 18;
        if (formula.numberOf("O") >= 1) fp.set(b);
        b = 19;
        if (formula.numberOf("O") >= 2) fp.set(b);
        b = 20;
        if (formula.numberOf("O") >= 4) fp.set(b);
        b = 21;
        if (formula.numberOf("O") >= 8) fp.set(b);
        b = 22;
        if (formula.numberOf("O") >= 16) fp.set(b);
        b = 23;
        if (formula.numberOf("F") >= 1) fp.set(b);
        b = 24;
        if (formula.numberOf("F") >= 2) fp.set(b);
        b = 25;
        if (formula.numberOf("F") >= 4) fp.set(b);
        b = 26;
        if (formula.numberOf("Na") >= 1) fp.set(b);
        b = 27;
        if (formula.numberOf("Na") >= 2) fp.set(b);
        b = 28;
        if (formula.numberOf("Si") >= 1) fp.set(b);
        b = 29;
        if (formula.numberOf("Si") >= 2) fp.set(b);
        b = 30;
        if (formula.numberOf("P") >= 1) fp.set(b);
        b = 31;
        if (formula.numberOf("P") >= 2) fp.set(b);
        b = 32;
        if (formula.numberOf("P") >= 4) fp.set(b);
        b = 33;
        if (formula.numberOf("S") >= 1) fp.set(b);
        b = 34;
        if (formula.numberOf("S") >= 2) fp.set(b);
        b = 35;
        if (formula.numberOf("S") >= 4) fp.set(b);
        b = 36;
        if (formula.numberOf("S") >= 8) fp.set(b);
        b = 37;
        if (formula.numberOf("Cl") >= 1) fp.set(b);
        b = 38;
        if (formula.numberOf("Cl") >= 2) fp.set(b);
        b = 39;
        if (formula.numberOf("Cl") >= 4) fp.set(b);
        b = 40;
        if (formula.numberOf("Cl") >= 8) fp.set(b);
        b = 41;
        if (formula.numberOf("K") >= 1) fp.set(b);
        b = 42;
        if (formula.numberOf("K") >= 2) fp.set(b);
        b = 43;
        if (formula.numberOf("Br") >= 1) fp.set(b);
        b = 44;
        if (formula.numberOf("Br") >= 2) fp.set(b);
        b = 45;
        if (formula.numberOf("Br") >= 4) fp.set(b);
        b = 46;
        if (formula.numberOf("I") >= 1) fp.set(b);
        b = 47;
        if (formula.numberOf("I") >= 2) fp.set(b);
        b = 48;
        if (formula.numberOf("I") >= 4) fp.set(b);
        b = 49;
        if (formula.numberOf("Be") >= 1) fp.set(b);
        b = 50;
        if (formula.numberOf("Mg") >= 1) fp.set(b);
        b = 51;
        if (formula.numberOf("Al") >= 1) fp.set(b);
        b = 52;
        if (formula.numberOf("Ca") >= 1) fp.set(b);
        b = 53;
        if (formula.numberOf("Sc") >= 1) fp.set(b);
        b = 54;
        if (formula.numberOf("Ti") >= 1) fp.set(b);
        b = 55;
        if (formula.numberOf("V") >= 1) fp.set(b);
        b = 56;
        if (formula.numberOf("Cr") >= 1) fp.set(b);
        b = 57;
        if (formula.numberOf("Mn") >= 1) fp.set(b);
        b = 58;
        if (formula.numberOf("Fe") >= 1) fp.set(b);
        b = 59;
        if (formula.numberOf("Co") >= 1) fp.set(b);
        b = 60;
        if (formula.numberOf("Ni") >= 1) fp.set(b);
        b = 61;
        if (formula.numberOf("Cu") >= 1) fp.set(b);
        b = 62;
        if (formula.numberOf("Zn") >= 1) fp.set(b);
        b = 63;
        if (formula.numberOf("Ga") >= 1) fp.set(b);
        b = 64;
        if (formula.numberOf("Ge") >= 1) fp.set(b);
        b = 65;
        if (formula.numberOf("As") >= 1) fp.set(b);
        b = 66;
        if (formula.numberOf("Se") >= 1) fp.set(b);
        b = 67;
        if (formula.numberOf("Kr") >= 1) fp.set(b);
        b = 68;
        if (formula.numberOf("Rb") >= 1) fp.set(b);
        b = 69;
        if (formula.numberOf("Sr") >= 1) fp.set(b);
        b = 70;
        if (formula.numberOf("Y") >= 1) fp.set(b);
        b = 71;
        if (formula.numberOf("Zr") >= 1) fp.set(b);
        b = 72;
        if (formula.numberOf("Nb") >= 1) fp.set(b);
        b = 73;
        if (formula.numberOf("Mo") >= 1) fp.set(b);
        b = 74;
        if (formula.numberOf("Ru") >= 1) fp.set(b);
        b = 75;
        if (formula.numberOf("Rh") >= 1) fp.set(b);
        b = 76;
        if (formula.numberOf("Pd") >= 1) fp.set(b);
        b = 77;
        if (formula.numberOf("Ag") >= 1) fp.set(b);
        b = 78;
        if (formula.numberOf("Cd") >= 1) fp.set(b);
        b = 79;
        if (formula.numberOf("In") >= 1) fp.set(b);
        b = 80;
        if (formula.numberOf("Sn") >= 1) fp.set(b);
        b = 81;
        if (formula.numberOf("Sb") >= 1) fp.set(b);
        b = 82;
        if (formula.numberOf("Te") >= 1) fp.set(b);
        b = 83;
        if (formula.numberOf("Xe") >= 1) fp.set(b);
        b = 84;
        if (formula.numberOf("Cs") >= 1) fp.set(b);
        b = 85;
        if (formula.numberOf("Ba") >= 1) fp.set(b);
        b = 86;
        if (formula.numberOf("Lu") >= 1) fp.set(b);
        b = 87;
        if (formula.numberOf("Hf") >= 1) fp.set(b);
        b = 88;
        if (formula.numberOf("Ta") >= 1) fp.set(b);
        b = 89;
        if (formula.numberOf("W") >= 1) fp.set(b);
        b = 90;
        if (formula.numberOf("Re") >= 1) fp.set(b);
        b = 91;
        if (formula.numberOf("Os") >= 1) fp.set(b);
        b = 92;
        if (formula.numberOf("Ir") >= 1) fp.set(b);
        b = 93;
        if (formula.numberOf("Pt") >= 1) fp.set(b);
        b = 94;
        if (formula.numberOf("Au") >= 1) fp.set(b);
        b = 95;
        if (formula.numberOf("Hg") >= 1) fp.set(b);
        b = 96;
        if (formula.numberOf("Tl") >= 1) fp.set(b);
        b = 97;
        if (formula.numberOf("Pb") >= 1) fp.set(b);
        b = 98;
        if (formula.numberOf("Bi") >= 1) fp.set(b);
        b = 99;
        if (formula.numberOf("La") >= 1) fp.set(b);
        b = 100;
        if (formula.numberOf("Ce") >= 1) fp.set(b);
        b = 101;
        if (formula.numberOf("Pr") >= 1) fp.set(b);
        b = 102;
        if (formula.numberOf("Nd") >= 1) fp.set(b);
        b = 103;
        if (formula.numberOf("Pm") >= 1) fp.set(b);
        b = 104;
        if (formula.numberOf("Sm") >= 1) fp.set(b);
        b = 105;
        if (formula.numberOf("Eu") >= 1) fp.set(b);
        b = 106;
        if (formula.numberOf("Gd") >= 1) fp.set(b);
        b = 107;
        if (formula.numberOf("Tb") >= 1) fp.set(b);
        b = 108;
        if (formula.numberOf("Dy") >= 1) fp.set(b);
        b = 109;
        if (formula.numberOf("Ho") >= 1) fp.set(b);
        b = 110;
        if (formula.numberOf("Er") >= 1) fp.set(b);
        b = 111;
        if (formula.numberOf("Tm") >= 1) fp.set(b);
        b = 112;
        if (formula.numberOf("Yb") >= 1) fp.set(b);
        b = 113;
        if (formula.numberOf("Tc") >= 1) fp.set(b);
        b = 114;
        if (formula.numberOf("U") >= 1) fp.set(b);
        return fp;
    }

}
