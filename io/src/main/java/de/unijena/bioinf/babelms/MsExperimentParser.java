
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

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.cef.AgilentCefExperimentParser;
import de.unijena.bioinf.babelms.json.JsonExperimentParserDispatcher;
import de.unijena.bioinf.babelms.massbank.MassbankExperimentParser;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.msp.MSPExperimentParser;
import de.unijena.bioinf.babelms.mzml.MzMlExperimentParser;
import de.unijena.bioinf.babelms.mzml.MzXmlExperimentParser;
import de.unijena.bioinf.babelms.sdf.SdfExperimentParser;
import de.unijena.bioinf.babelms.txt.TxtExperimentParser;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MsExperimentParser {

    protected static final Map<String, Class<? extends Parser<Ms2Experiment>>> KNOWN_ENDINGS = addKnownEndings();

    // there is no good solution without writing the endings here explicitly (otherwise DESCRIPTION can not be used in annotations)
    public static final String DESCRIPTION = ".ms, .mgf, .mzxml, .mzml, .cef, .msp, .mat, .mb, .mblib, .txt (MassBank), .json (GNPS, MoNA), .zip";

    /**
     * This postprocessor annotates Parameter configs to the {@link Ms2Experiment}. If {@link InputFileConfig} is given
     * this is preferred over the {@link PropertyManager#DEFAULTS} config.
     * <p>
     * If an input file type supports SIRIUS parameters (e.g. JenaMSParser) then it has to set this Parameters to it
     * own {@link de.unijena.bioinf.ms.properties.ParameterConfig} and annotate this config wrapped as {@link InputFileConfig}
     * to the experiment. This allow to keep track of where parameters come from.
     */
    public static final Consumer<Ms2Experiment> DEFAULTS_ANNOTATOR = exp -> exp.addAnnotationsFrom(
            exp.getAnnotation(InputFileConfig.class).map(c -> c.config).orElse(PropertyManager.DEFAULTS), Ms2ExperimentAnnotation.class);


    public GenericParser<Ms2Experiment> getParser(Path file) {
        return getParser(file.getFileName().toString());
    }

    public GenericParser<Ms2Experiment> getParser(File file) {
        return getParser(file.getName());
    }

    public GenericParser<Ms2Experiment> getParser(String fileName) {
        final int i = fileName.lastIndexOf('.');
        if (i < 0) return null; // no parser found
        final String extName = fileName.substring(i).toLowerCase();
        return getParserByExt(extName);
    }

    public GenericParser<Ms2Experiment> getParserByExt(String extName) {
        if (!extName.startsWith("."))
            extName = "." + extName;

        final Class<? extends Parser<Ms2Experiment>> pc = KNOWN_ENDINGS.get(extName);
        if (pc == null) return null;
        try {
            if (pc.equals(ZippedSpectraParser.class))
                return (GenericParser<Ms2Experiment>) pc.getConstructor().newInstance();

            return new GenericParser<>(pc.getConstructor().newInstance(), DEFAULTS_ANNOTATOR);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }


    public static boolean isSupportedFileName(final @NotNull String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0)
            return false;
        return isSupportedEnding(fileName.substring(index));
    }

    public static boolean isSupportedEnding(final @NotNull String fileEnding) {
        return KNOWN_ENDINGS.containsKey(fileEnding.toLowerCase());
    }

    private static Map<String, Class<? extends Parser<Ms2Experiment>>> addKnownEndings() {
        final Map<String, Class<? extends Parser<Ms2Experiment>>> endings = new ConcurrentHashMap<>(3);
        endings.put(".ms", JenaMsParser.class);
        endings.put(".mgf", MgfParser.class);
        endings.put(".zip", ZippedSpectraParser.class);
        endings.put(".mzxml", MzXmlExperimentParser.class);
        endings.put(".mzml", MzMlExperimentParser.class);
        endings.put(".cef", AgilentCefExperimentParser.class);
        endings.put(".msp", MSPExperimentParser.class);
        endings.put(".mat", MSPExperimentParser.class);
        endings.put(".mb", MassbankExperimentParser.class);
        endings.put(".mblib", MassbankExperimentParser.class);
        endings.put(".txt", TxtExperimentParser.class);
        endings.put(".json", JsonExperimentParserDispatcher.class);
        endings.put(".sdf", SdfExperimentParser.class);
        return endings;
    }
}
