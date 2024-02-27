/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.custom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Jacksonized
@Getter
public class CustomDatabaseSettings {
    private final List<CdkFingerprintVersion.USED_FINGERPRINTS> usedFingerprints;
    private final int schemaVersion;
    private final boolean matchRtOfReferenceSpectra;
    private final String name;
    private final String displayName;
    @Setter
    private Statistics statistics;

    public static class Statistics {
        private final AtomicLong compounds;
        private final AtomicLong formulas;

        private final AtomicLong spectra;

        //json constructor
        public Statistics() {
            this(0, 0, 0);
        }

        public Statistics(int compounds, int formulas, int spectra) {
            this(new AtomicLong(compounds), new AtomicLong(formulas), new AtomicLong(spectra));
        }

        private Statistics(AtomicLong compounds, AtomicLong formulas, AtomicLong spectra) {
            this.compounds = compounds;
            this.formulas = formulas;
            this.spectra = spectra;
        }

        @JsonIgnore
        protected AtomicLong compounds() {
            return compounds;
        }

        @JsonIgnore
        protected AtomicLong formulas() {
            return formulas;
        }

        @JsonIgnore
        protected AtomicLong spectra() {
            return spectra;
        }

        public long getCompounds() {
            return compounds().get();
        }

        public long getFormulas() {
            return formulas().get();
        }

        public long getSpectra() {
            return spectra().get();
        }
    }
}
