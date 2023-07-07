
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

package de.unijena.bioinf.sirius;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration profile to store instrument specific algorithm properties.
 * Some of the default profiles are: 'qtof', 'orbitrap', 'fticr'.
 */
@DefaultProperty(propertyParent = "AlgorithmProfile")
public class Profile {
    public final FragmentationPatternAnalysis fragmentationPatternAnalysis;
    public final IsotopePatternAnalysis isotopePatternAnalysis;
    public final Ms2Preprocessor ms2Preprocessor;
    public final Ms1Preprocessor ms1Preprocessor;

    public Profile(String name) throws IOException {
        final boolean oldSirius = name.startsWith("oldSirius");
        if (oldSirius) {
            name = name.split(":")[1];
        }
        final JsonNode json = JSONDocumentType.getJSON("/profiles/" + name.toLowerCase() + ".json", name);
        final JSONDocumentType document = new JSONDocumentType();
        if (json.has("FragmentationPatternAnalysis")) this.fragmentationPatternAnalysis = FragmentationPatternAnalysis.loadFromProfile(document, json);
        else fragmentationPatternAnalysis=null;
        if (json.has("IsotopePatternAnalysis")) this.isotopePatternAnalysis = IsotopePatternAnalysis.loadFromProfile(document, json);
        else isotopePatternAnalysis=null;
        this.ms2Preprocessor = new Ms2Preprocessor();
        this.ms1Preprocessor = new Ms1Preprocessor();
    }

    @DefaultInstanceProvider
    public static Profile fromString(@DefaultProperty String value) {
        try {
            return new Profile(value);
        } catch (IOException e) {
            throw new RuntimeException("Could not find profile JSON", e);
        }
    }


    public Profile(IsotopePatternAnalysis ms1, FragmentationPatternAnalysis ms2) {
        this.fragmentationPatternAnalysis = ms2;
        this.isotopePatternAnalysis = ms1;
        this.ms2Preprocessor = new Ms2Preprocessor();
        this.ms1Preprocessor = new Ms1Preprocessor();
    }

    public void writeToFile(@NotNull final String fileName) throws IOException  {
        writeToFile(new File(fileName));
    }

    public void writeToFile(@NotNull final File file) throws IOException {
        final FileWriter writer = new FileWriter(file);
        final JSONDocumentType json = new JSONDocumentType();
        final ObjectNode obj = json.newDictionary();
        if (fragmentationPatternAnalysis != null) {
            fragmentationPatternAnalysis.writeToProfile(json, obj);
        }
        if (isotopePatternAnalysis != null) {
            isotopePatternAnalysis.writeToProfile(json, obj);
        }
        try {
            JSONDocumentType.writeJson(json, obj, writer);
        } finally {
            writer.close();
        }
    }

}
