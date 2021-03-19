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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by ge28quv on 08/06/17.
 */
public class Main {

    public static void main(String... args) throws ChemicalDatabaseException, IOException{
        ChemicalDatabase chemicalDatabase = new ChemicalDatabase();



        int i = 0;
        Deviation dev = new Deviation(10);
        PrecursorIonType ionType = PrecursorIonType.getPrecursorIonType("[M+H]+");
        while (true) {
            i++;
            if (i>10) break;
            double mass = Math.random()*500+100;
            List<FormulaCandidate> formulas = chemicalDatabase.lookupMolecularFormulas(mass, dev, ionType);

            if (formulas.size()>0){
                System.out.println(formulas.get(0).formula);
                List<FingerprintCandidate> candidates = chemicalDatabase.lookupStructuresAndFingerprintsByFormula(formulas.get(0).formula);
            }

        }


        List<FingerprintCandidate> fp = chemicalDatabase.lookupFingerprintsByInchis(Collections.singleton("UKQHHPFOVDGOPK"));
        System.out.println(Arrays.toString(fp.get(0).fingerprint.toIndizesArray()));


        chemicalDatabase.close();
//
//        MolecularFormula mf = MolecularFormula.parse("C6H12O6");
//        List<FingerprintCandidate> candidateList =  chemicalDatabase.lookupStructuresAndFingerprintsByFormula(mf);
//
//
//        System.out.println(candidateList.size());
//        System.out.println(candidateList.get(0).getFingerprint().toOneZeroString());
//        System.out.println(candidateList.get(0).getInchi());
//
//
////        candidateList.clear();
////        chemicalDatabase.lookupStructuresAndFingerprintsByFormulaShortArray(mf, candidateList);
////        System.out.println(candidateList.size());
////        System.out.println(candidateList.get(0).getFingerprint().toOneZeroString());
////        System.out.println(candidateList.get(0).getInchi());
////
////
////        FingerprintCandidate candidate = chemicalDatabase.lookupFingerprintsByInchisShortArray(Collections.singleton(candidateList.get(0).getInchiKey2D())).get(0);
////
//////        System.out.println(candidateList.size());
////        System.out.println(candidate.getFingerprint().toOneZeroString());
////        System.out.println(candidate.getInchi());
    }
}
