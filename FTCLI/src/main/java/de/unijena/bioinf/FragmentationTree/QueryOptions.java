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
package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.babelms.chemdb.Databases;

import java.io.File;
import java.util.List;

/**
 * TODO: Maybe export this code in an own module?
 */
public interface QueryOptions {

    @Option(shortName = "D", description = "use database", defaultValue="PUBCHEM")
    public Databases getDatabase();

    @Option(defaultToNull = true, description = "directory with cache file")
    public File getCachingDirectory();

    @Option(longName = "ppm", shortName = "p", defaultValue = "20", description = "relative mass error in ppm")
    public int getPPM();

    @Option(longName = "abs", shortName = "a", defaultValue = "0.001",description = "absolute mass error in Dalton")
    public double getAbsoluteDeviation();

    @Unparsed(description = "a list of masses and/or molecular formulas to query")
    public List<String> getQueries();





}
