
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

package de.unijena.bioinf.IsotopePatternAnalysis.isogencli;


import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Options {

    @Option(shortName = "i", longName = "ionization", description = "ionization mode (e.g. [M+H+]+ or charge (e.g. 2) or [M+2.12]+",
    defaultToNull = true)
    String getIonizationMode();


    @Option(shortName = "l", longName = "limit", description = "number of isotope peaks to generate",
    defaultToNull = true)
    Integer getNumberOfIsotopePeaks();

    @Option(shortName = "t", longName = "treshold", description = "minimum intensity which should be listed",
            defaultToNull = true)
    Double getIntensityTreshold();

    @Option(shortName = "n", longName = "norm", description = "normalization type (either sum or max)", pattern = "max|sum",
    defaultValue = "sum")
    String getNormalization();

    @Option(shortName = "s", longName = "scale", description = "normalization scaling facor (by default 100%)",
    defaultValue = "100")
    double getScalingFactor();

    @Option(longName = "distribution", shortName = "d", description = "file name of isotopic distribution file.", defaultToNull = true)
    String getIsotopeDistributionFile();

    @Unparsed(description = "molecular formula for which the pattern is generated")
    String getMolecularFormula();

    @Option(longName = {"version", "cite"})
    boolean getVersion();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    boolean getHelp();



}
