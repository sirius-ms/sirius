
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

import org.openscience.cdk.fingerprint.SubstructureFingerprinter;

/**
 * Created by kaidu on 09.10.2014.
 */
public class CFMFingerprinter extends SubstructureFingerprinter {

    final static String[] SMARTS = new String[]{
            "CCCCl", "O=Cc1cccc(Cl)c1", "C=CCl", "C=NS(=O)=O", "N=CS", "C=CS", "NC(=N)S", "C=CCS", "CC(N)S", "CCCS",
            "CCSC", "C=NS(=O)(=O)c1ccccc1", "C=C(C)S", "N=Cc1ccccc1Cl", "CCCl", "C=CCl", "C=Nc1cccc(Cl)c1",
            "Nc1cccc(Cl)c1", "Nc1ccc(Cl)cc1", "OCc1cccc(Cl)c1", "C=Nc1ccc(Cl)cc1", "CCCCCl", "COc1ccc(Cl)cc1",
            "Cc1cccc(Cl)c1", "N=Cc1ccc(Cl)cc1", "CSc1ccccc1", "C1CCSC1", "NCc1ccc(Cl)cc1", "Cc1ccccc1Cl",
            "FC1CCCCC1", "C#CC", "C=CC#N", "C#CC", "C#CCC", "C1=CCCC=C1", "C1=CCCCC1", "C1CCCCC1", "N=CO", "CC1CC=CCC1",
            "COc1ccc(OC)cc1", "C#CCO", "C=CO", "OCCO", "Cc1cccc(O)c1", "COCO", "COC=O", "CCC(O)OC", "C=C(C)C=O",
            "C=C(C)C(O)=O", "C=C(C)CO", "C#CCC", "C#CCCC", "C#CCCCC", "CC(O)O", "O=Cc1ccccc1", "OCc1ccccc1",
            "OC1CC=CCC1", "OC(=O)c1ccccc1", "C=C(O)C=O", "C=C(C)O", "C=COC", "C#CC(C)C", "CC1C=CCC=C1", "C=NC",
            "CC1C=CCCC1", "C#CC(C)CC", "C=NCC", "C=NCCC", "C=CCCN=C", "N#Cc1ccccc1", "CCC(O)O", "CC(C)C(O)O",
            "CC(C)=C", "C=CCCO", "C=CCO", "C=CC(C)=C", "C=CN", "CC1CCC(O)CC1", "C=C1CCC(O)CC1", "C#CC(C)O", "C=CC(C)O",
            "CC1CCC=CC1=C", "CCCCC=C", "C#CCCCCC", "CCCCCC=C", "C#CCCCCCC", "CCCCCCC=C", "C#CCCCCCCC", "CCCCCCCC=C",
            "C#CCCCCCCCC", "CCCCCCCCC=C", "C#CCCCCCCCCC", "NC=CO", "CC(C)(C)O", "CC(C)(C)OCO", "CC(C)(C)OC=O",
            "CC(C)(C)C", "CCC(C)(C)C", "C=C1C=CC=CC1", "CC1CCCCC1", "C=C1CCCCC1", "CC1CCCCC1C", "CC1CCCCC1=C",
            "C=CCCCCO", "CCCCCCCCC", "CC(C)(C)C=O", "CCCCC(C)=C", "C=CCCC(C)=C", "CCCC(C)=C", "C=CCC(C)=C", "CCC(C)=C",
            "COC1CCCCC1", "COC1CC=CCC1", "COc1ccccc1", "COC1CCC(C)CC1", "C#CCNC", "C=CCNC", "C=CCOC", "C#CCN", "C=NC=O",
            "C=C(N)C=O", "N=C(O)O", "C1=CCCNC1", "C1CCCNC1", "C=CCN", "C=CNC", "C=NN", "C=CNC(=C)C", "C=C(C)N", "CC(N)O",
            "C=NC(=N)O", "NC(=N)O", "C=CC(=C)O", "NC=N", "C=C(C)OC", "C=CC(C)OC", "CC#CCC", "C=C(N)CO", "C#CC(O)CC",
            "C=CC(O)CC", "CCC(N)O", "C=C(C)C(=N)O", "C=C(C)C#N", "CCC#N", "C=CN=C", "C=NCCO", "C=NCO", "C=NC=N",
            "C1=CCNCC1", "C1CCNCC1", "CC#CCC", "C=CC(=C)N", "C=C(O)C=O", "C=CNCC", "COCCN", "C=CCCN", "C=CCCCO",
            "C=C(C)CCN", "CN1CCCC1", "CN1CC=CC1", "C=CCN=C", "C=NN", "NCC=N", "C=C(C)C=N", "C=C(C)CN", "C=C(C)CCO",
            "C1=CCOC1", "OCC=CO", "CC=CCN", "C=CCCOC", "C#CCCC", "C1=CCCNC1", "C1=CCCC1", "C1=CCNC1", "C1CC1", "CC=CO",
            "CN(C)C=O", "CNCO", "C=NC(C)O", "CCOCO", "C1COCCN1", "NC1CC1", "NC=CCO", "CC1CC1", "COC(C)O", "Nc1ccccc1",
            "CNC(C)O", "N=Cc1ccccc1", "NCc1ccccc1", "C=COCC", "NC=Cc1ccccc1", "C=C1CCCCC1=C", "CCC(=C)CC", "C=CC(=C)CC",
            "C=C(C)C(C)=C", "CC(=C)C(C)C", "CC1CCC(=C)CC1", "C#CC(C)CC", "C=C(C)CC(C)=C", "C=Cc1ccccc1", "C=CC1CCCCC1",
            "CCC(C)C=C", "CC(C)C=C", "C#CC(C)C", "CCC1CC=CCC1", "C=C1C=CCCC1", "CC1CCCC(=C)C1", "CC(C)(C)c1ccccc1",
            "C=Cc1ccccc1", "C=C(N)CC", "C=CN(C)C", "C#CCC(C)(C)C", "C1CCCC1", "NC1CCCC1", "C#CC(CC)CC", "C=Nc1ccccc1",
            "CC(=C)c1ccccc1", "C=C(C)NC", "OCC1CC=CCC1", "OCC1CCCCC1", "N=C(O)c1ccccc1", "C#CC(C)C(C)(C)C",
            "C=CC(C)(C)C", "COc1cccc(C=O)c1", "CCCCOC", "CNc1ccccc1", "CC1CC=CC(=C)C1", "CCC1CC=CCC1", "CCC1CCCCC1",
            "C=C(O)c1ccccc1", "CN1CCCCC1", "C1CNCCC1", "C#CCC(C)C", "CC1CCCC(C)C1", "CC1C=CC(=C)CC1", "OC1CCCCC1",
            "NCC1CCCCC1", "NCC1CC=CCC1", "C=Cc1ccc(OC)cc1", "CCC1CCCCC1", "COC1C=CC(C)CC1", "CC1CCCNC1", "CC(C)C(C)C",
            "C1CCNC1", "CC(=C)C1CCCCC1", "CC(C)CC=C", "C#CCC1CCCCC1", "CC=CC(C)=C", "OC(O)c1ccccc1", "OC1C=CCCC1",
            "NC1CC=CCC1", "NC1CCCCC1", "C=CCCNC", "C=CNCCC", "CNC(C)C", "CCNC(C)C", "C=C(C)NCC", "COc1ccccc1C",
            "C=CC(CC)OC", "CC(C)CCO", "C=CC(C)C(C)=C", "CCC(C)C(C)=C", "C=C(C)C=CCO", "CCOC1CCCCC1", "CC(C)(C)N",
            "C#CCC(C)CC", "CC1CCNCC1", "CC1C=CCNC1", "C=CN(C)CC", "CCN(C)CC", "C=CC(C)CN", "O=C1C=CCC=C1", "CCC(C)OC",
            "COc1cccc(C)c1", "C1=CCCC=C1", "c1ccc2c(c1)C=CCC2", "CCOCC", "NC1C=CCCC1", "CC(C)CO", "C=CC(C)N", "CCC(C)N",
            "CC=CCO", "CCCCC(O)O", "CCCC(O)O", "CCC=CCC", "OCc1cccnc1", "OCNc1ccccc1", "OC=Nc1ccccc1", "CCC(C)CN",
            "COC(=O)c1ccccc1", "C=NCc1ccccc1", "C=CCCCN", "C=C(C)CNC", "Cc1cccnc1", "Cc1ccccn1", "CN(C)c1ccccc1",
            "CCCCCN", "COc1ccccc1C=N", "CN1CC=CCC1", "C#CC(C)(C)O", "O=Cc1cccnc1", "CC(C)CN", "CC1CCCO1", "C=C(O)CC",
            "C1CCOC1", "CC1CC=CC1", "CC1CCCC1", "C=CCNCC", "CCCNCC", "Nc1cccc(C=O)c1", "C=CCN(C)C", "COc1cccc(OC)c1",
            "COc1ccccc1OC", "C#CC(C)CCC", "CCCCCCCCCC", "C=C(N)C(=O)O", "C=CC(N)CC", "CCNCO", "CC=CCCO", "CCOC(C)O",
            "C=CC(C)CO", "CCC(C)CO"
    };

    public CFMFingerprinter() {
        super(SMARTS);
    }

}
