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
package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ProfileOptions {

    public static class Interpret {
        private final static Pattern INTERVAL = Pattern.compile("\\[(\\d*)\\]");
        public static FormulaConstraints getFormulaConstraints(ProfileOptions options) {
            final PeriodicTable PT = PeriodicTable.getInstance();
            final Pattern pattern = PT.getPattern();
            final String string = options.getElements();
            final Matcher matcher = pattern.matcher(string);
            if (!matcher.find()) throw new ArgumentValidationException("Invalid alphabet: " + options.getElements());
            HashMap<Element, Integer> elements = new HashMap<Element, Integer>();
            while(true) {
                final String m = matcher.group(0);
                if (m.charAt(0)=='(' || m.charAt(0) == ')') throw new ArgumentValidationException("Invalid alphabet: " + options.getElements());
                final Element element = PT.getByName(m);
                if (element == null) throw new ArgumentValidationException("Unknown character: " + m);
                final int start = matcher.end();
                final boolean next = matcher.find();
                final int end = next ? matcher.start() : string.length();
                elements.put(element, Integer.MAX_VALUE);
                if (end-start > 0) {
                    final Matcher n = INTERVAL.matcher(string.substring(start, end));
                    if (n.find()) {
                        final int a = n.group(1)!=null ? Integer.parseInt(n.group(1)) : Integer.MAX_VALUE;
                        elements.put(element, a);
                    }
                }
                if (!next) break;
            }
            final FormulaConstraints constraints = new FormulaConstraints(new ChemicalAlphabet(elements.keySet().toArray(new Element[0])));
            for (Map.Entry<Element, Integer> entry : elements.entrySet()) {
                constraints.getUpperbounds()[constraints.getChemicalAlphabet().indexOf(entry.getKey())] = entry.getValue();
            }
            constraints.addFilter(new ValenceFilter());
            return constraints;
        }

        public static MeasurementProfile getMeasurementProfile(ProfileOptions options) {

            Double ppmMax=null, ppmAbs=null, sdms1=null, sdms2=null, sdDiff=null;

            ppmMax = options.getPPMMax();
            ppmAbs = options.getAbsMax();
            if (ppmMax != null && ppmAbs == null)
                ppmAbs = ppmMax*100d*1e-6;
            if (ppmMax != null) {
                sdms1 = options.getStandardDeviationOfMs1()==null ? ppmMax/3d : options.getStandardDeviationOfMs1();
                sdms2 = options.getStandardDeviationOfMs2()==null ? ppmMax/3d : options.getStandardDeviationOfMs2();
                sdDiff = options.getStandardDeviationOfDiff()==null ? ppmMax/4d : options.getStandardDeviationOfMs2();
            }

            final MutableMeasurementProfile profile = new MutableMeasurementProfile(ppmMax!=null ? new Deviation(ppmMax, ppmAbs) : null,
                    sdms1 != null ? new Deviation(sdms1) : null,
                    sdms2 != null ? new Deviation(sdms2) : null,
                    sdDiff != null ? new Deviation(sdDiff) : null, getFormulaConstraints(options), options.getExpectedIntensityDeviation()==null ? 0d : options.getExpectedIntensityDeviation(),
                    options.getNoiseMedian()==null ? 0 : options.getNoiseMedian());
            profile.setIntensityDeviation(0d);
            // ...
            return profile;
        }
    }

    @Option(shortName = "p", defaultValue = "default", description =
            "A profile contains all scoring and preprocessing information that is necessary for the given data. " +
                    "It is either a profile.json file or the name of a predefined profile. Predefined profiles are: " +
                    "default, qtof, qtof.high, orbitrap")
    public String getProfile();

    @Option(defaultValue = "CHNOPS", description = "Allowed elements. Write e.g. CHNOPFI to allow the elements C, H, N, O, P, F and I. You can " +
            "also give upperbounds for the element by writing them in square brackets, e.g. CHNOPCl[1]F[5]Br[1]")
    public String getElements();

    /*
        ppm
     */

    @Option(shortName = "l", defaultToNull = true, description = "limit number of peaks to the n-th most intensive peaks. This makes computation much faster")
    public Integer getPeakLimit();

    @Option(longName = "ppm.max", defaultToNull = true, description = "maximal ppm of peaks (used for decompositions)")
    public Double getPPMMax();

    @Option(longName = "abs.max", defaultToNull = true, description = "maximal mass deviation of peaks (used for decomposition)")
    public Double getAbsMax();

    @Option(longName = "ppm.sd.ms2", defaultToNull = true, description = "ppm standard deviation of ms2 peaks")
    public Double getStandardDeviationOfMs1();

    @Option(longName = "ppm.sd.ms1", defaultToNull = true, description = "ppm standard deviation of ms1 peaks")
    public Double getStandardDeviationOfMs2();

    @Option(longName = "ppm.sd.diff", defaultToNull = true, description = "ppm standard deviation of ms1 peak differences (~recalibrated peaks)")
    public Double getStandardDeviationOfDiff();

    @Option(longName = "intensity.sd", defaultToNull = true, description = "intensity standard deviation of ms1 peaks")
    public Double getExpectedIntensityDeviation();

    @Option(longName = "noise.median", defaultToNull = true, description = "median intensity of noise peaks (above certain threshold)")
    public Double getNoiseMedian();

    @Option(longName = "treeSize", defaultToNull = true, description = "additional score bonus per explained peak. Higher values leads to bigger trees.")
    public Double getTreeSize();

}

