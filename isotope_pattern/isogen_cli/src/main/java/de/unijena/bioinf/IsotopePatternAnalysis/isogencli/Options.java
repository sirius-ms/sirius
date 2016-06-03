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
package de.unijena.bioinf.IsotopePatternAnalysis.isogencli;


import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Options {

    @Option(shortName = "i", longName = "ionization", description = "ionization mode (e.g. [M+H+]+ or charge (e.g. 2) or [M+2.12]+",
    defaultToNull = true)
    public String getIonizationMode();


    @Option(shortName = "l", longName = "limit", description = "number of isotope peaks to generate",
    defaultToNull = true)
    public Integer getNumberOfIsotopePeaks();

    @Option(shortName = "t", longName = "treshold", description = "minimum intensity which should be listed",
            defaultToNull = true)
    public Double getIntensityTreshold();

    @Option(shortName = "n", longName = "norm", description = "normalization type (either sum or max)", pattern = "max|sum",
    defaultValue = "sum")
    public String getNormalization();

    @Option(shortName = "s", longName = "scale", description = "normalization scaling facor (by default 100%)",
    defaultValue = "100")
    public double getScalingFactor();

    @Option(longName = "distribution", shortName = "d", description = "file name of isotopic distribution file.", defaultToNull = true)
    public String getIsotopeDistributionFile();

    @Unparsed(description = "molecular formula for which the pattern is generated")
    public String getMolecularFormula();

    @Option(longName = {"version", "cite"})
    public boolean getVersion();

    @Option(shortName = "h", longName = "help", helpRequest = true)
    public boolean getHelp();



}
