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
package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.mzml.MzmlExperimentParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsExperimentParser {

    private static final Map<String, Class<? extends Parser<Ms2Experiment>>> knownEndings = addKnownEndings();


    public GenericParser<Ms2Experiment> getParser(File f) {
        final String name = f.getName();
        final int i = name.lastIndexOf('.');
        if (i < 0) return null; // no parser found
        final String extName = name.substring(i).toLowerCase();
        final Class<? extends Parser<Ms2Experiment>> pc = knownEndings.get(extName);
        if (pc==null) return null;
        try {
            if (pc.equals(ZippedSpectraParser.class)){
                return (GenericParser<Ms2Experiment>)pc.newInstance();
            }
            return new GenericParser<Ms2Experiment>(pc.newInstance());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
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
        return knownEndings.containsKey(fileEnding.toLowerCase());
    }

    private static Map<String, Class<? extends Parser<Ms2Experiment>>> addKnownEndings() {
        final Map<String, Class<? extends Parser<Ms2Experiment>>> endings = new ConcurrentHashMap<>(3);
        endings.put(".ms", JenaMsParser.class);
        endings.put(".mgf", MgfParser.class);
        endings.put(".zip", ZippedSpectraParser.class);
        endings.put(".mzxml", MzmlExperimentParser.class);
        return endings;
    }
}
