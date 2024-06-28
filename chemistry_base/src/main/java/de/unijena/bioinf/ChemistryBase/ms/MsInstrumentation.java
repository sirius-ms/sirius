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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MsInstrumentation can be attached to input files to decide on certain options of the algorithm.
 * It describes not only the choice of the instrument but also the experimental setup.
 */
public interface MsInstrumentation extends Ms2ExperimentAnnotation {

    boolean hasIsotopesInMs2();
    String description();
    Deviation getExpectedMassDeviation();
    String getRecommendedProfile();
    Chromatrography getChromatography();

    boolean isInstrument(String description);

    enum Chromatrography {
        GC, LC
    }

    public static MsInstrumentation getBestFittingInstrument(String name) {
        int fit = -1;
        MsInstrumentation best = Unknown;
        for (Instrument i : Instrument.values()) {
            Matcher matcher = i.pattern.matcher(name);
            while (matcher.find()) {
                final int len = matcher.end()-matcher.start();
                if (len > fit) {
                    fit = len;
                    best = i;
                }
            }
        }
        return best;
    }

    MsInstrumentation Unknown = new MsInstrumentation() {
        @Override
        public boolean hasIsotopesInMs2() {
            return false;
        }

        @Override
        public String description() {
            return "Unknown (LCMS)";
        }

        @Override
        public Deviation getExpectedMassDeviation() {
            return new Deviation(10);
        }

        @Override
        public String getRecommendedProfile() {
            return "default";
        }

        @Override
        public Chromatrography getChromatography() {
            return Chromatrography.LC;
        }

        @Override
        public boolean isInstrument(String description) {
            return true;
        }
    };

    enum Instrument implements MsInstrumentation {
        BRUKER_MAXIS("Bruker Q-ToF (LCMS)", "qtof"/*"bruker_tof"*/, new Deviation(10), true, "maxis|bruker|impact"),
        QTOF("Q-ToF (LCMS)", "qtof", new Deviation(10), false, "tof"),
        ORBI("Orbitrap (LCMS)", "orbitrap", new Deviation(5), false, "orbi|(?:q-)?exactive|velos|Lumos"),
        FTICR("FTICR (LCMS)", "orbitrap"/*"fticr"*/, new Deviation(5), false, "ft-?icr|Hybrid FT|LTQ-FTICR|ft"),
        IONTRAP("Ion Trap (LCMS)", "default", new Deviation(20), false, "ion\\s*trap|trap|lcq|QqIT|QqLIT|IT|LIT"),
        QQQ("Tripple-Quadrupole", "default", new Deviation(100,0.1), false, "QQQ|quadrupole|QQ|Q");

        protected boolean isotopes;
        protected String profile, description;
        protected Deviation ppm;
        protected Chromatrography chromatrography;
        protected boolean highres;
        protected Pattern pattern;

        Instrument(String description, String profile, Deviation dev, boolean iso, String pattern) {
            this.description = description;
            this.profile = profile;
            this.ppm = dev;
            this.isotopes = iso;
            this.chromatrography = Chromatrography.LC;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.highres = dev.getAbsolute() < 0.1;
        }

        public boolean isHighres() {
            return highres;
        }



        public boolean isInstrument(String description) {
            if (description.equalsIgnoreCase(this.description)) return true;
            return pattern.matcher(description).find();
        }

        public Chromatrography getChromatography() {
            return chromatrography;
        }

        @Override
        public boolean hasIsotopesInMs2() {
            return isotopes;
        }

        @Override
        public Deviation getExpectedMassDeviation() {
            return ppm;
        }

        @Override
        public String getRecommendedProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public String description() {
            return description;
        }
    }

}
