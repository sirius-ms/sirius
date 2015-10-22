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

import java.io.File;
import java.util.HashMap;

public class MsExperimentParser {

    private final HashMap<String, Class<? extends Parser<Ms2Experiment>>> knownEndings;

    public MsExperimentParser() {
        this.knownEndings = new HashMap<String, Class<? extends Parser<Ms2Experiment>>>();
        addKnownEndings();
    }

    public GenericParser<Ms2Experiment> getParser(File f) {
        final String name = f.getName();
        final int i = name.lastIndexOf('.');
        if (i < 0) return null; // no parser found
        final String extName = name.substring(i).toLowerCase();
        final Class<? extends Parser<Ms2Experiment>> pc = knownEndings.get(extName);
        if (pc==null) return null;
        try {
            return new GenericParser<Ms2Experiment>(pc.newInstance());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void addKnownEndings() {
        knownEndings.put(".ms", (Class<? extends Parser<Ms2Experiment>>)JenaMsParser.class);
        knownEndings.put(".mgf", MgfParser.class);
    }
}
