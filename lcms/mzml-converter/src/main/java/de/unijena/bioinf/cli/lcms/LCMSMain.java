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

package de.unijena.bioinf.cli.lcms;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.Feature;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class LCMSMain {

    public static void main(String[] args) {
        for (String arg : args) {
            LCMSProccessingInstance instance = new LCMSProccessingInstance();
            InMemoryStorage storage = new InMemoryStorage();
            final File f = new File(arg);
            String nameWithoutEnding = f.getName();
            nameWithoutEnding = nameWithoutEnding.substring(0,nameWithoutEnding.lastIndexOf('.'));
            try (final BufferedWriter bw = FileUtils.getWriter(new File(f.getParent(), nameWithoutEnding + ".ms"))){
                LCMSRun parse = new MzXMLParser().parse(f, storage);
                ProcessedSample sample = instance.addSample(parse, storage);
                instance.detectFeatures(sample);
                for (FragmentedIon ion : sample.ions) {
                    Feature feature = instance.makeFeature(sample, ion, false);
//todo @Kai not compileable
                    //                    bw.write(feature.toMsExperiment());

                }
            } catch (IOException | InvalidInputData e) {
                e.printStackTrace();
                System.exit(1);
            }

        }
    }

}
