/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
//import net.sf.jniinchi.JniInchiException;
//import net.sf.jniinchi.JniInchiOutputKey;
//import net.sf.jniinchi.JniInchiWrapper;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class TrainingData {
    private final InChI[] trainingStructures;

    public TrainingData(InChI[] trainingStructures) {
        this.trainingStructures = trainingStructures;
    }

    public TrainingData(ArrayList<InChI> inchis) {
        this(inchis.toArray(new InChI[0]));
    }

    public InChI[] getTrainingStructures() {
        return trainingStructures;
    }


    public static TrainingData readTrainingData(BufferedReader br) throws IOException {
        ArrayList<InChI> inchis = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
//            try {
                String[] tabs = line.split("\t");
                InChI inChI;
                if (tabs.length == 1) {
                    //no InChiKeys contained. Compute them.
                    inChI =  InChISMILESUtils.getInchiWithKeyOrThrow(tabs[0]);
//                    String inchi = tabs[0];
//                    String key = InChISMILESUtils.(inchi);
//                    inChI = InChIs.newInChI(key, inchi);
                } else {
                    inChI = InChIs.newInChI(tabs[0], tabs[1]);
                }
                inchis.add(inChI);
//            } catch (JniInchiException ex) {
//                LoggerFactory.getLogger(TrainingData.class).warn("Could not parse training structure InChI, skipping this entry");
//            }
        }
        return new TrainingData(inchis);
    }


}
