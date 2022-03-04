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
import de.unijena.bioinf.chemdb.DataSources;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomDatabaseSettings {
    private final boolean inheritance;
    private final long filter;
    private final List<CdkFingerprintVersion.USED_FINGERPRINTS> fingerprintVersion;
    private final int schemaVersion;

    private Statistics statistics;

    public CustomDatabaseSettings(boolean inheritance, long filter, List<CdkFingerprintVersion.USED_FINGERPRINTS> fingerprintVersion, int schemaVersion, @Nullable Statistics statistics) {
        this.inheritance = inheritance;
        this.filter = filter;
        this.fingerprintVersion = fingerprintVersion;
        this.schemaVersion = schemaVersion;
        this.statistics = statistics == null ? new Statistics(0, 0) : statistics;
    }

    //json constructor
    private CustomDatabaseSettings() {
        this(false,0,null,0,null);
    }

    public static class Statistics {
        private final AtomicLong compounds;
        private final AtomicLong formulas;

        //json constructor
        private Statistics() {
            this(0,0);
        }

        public Statistics(int compounds, int formulas) {
            this(new AtomicLong(compounds),new AtomicLong(formulas));
        }

        private Statistics(AtomicLong compounds, AtomicLong formulas) {
            this.compounds = compounds;
            this.formulas = formulas;
        }

        @JsonIgnore
        protected AtomicLong compounds() {
            return compounds;
        }

        @JsonIgnore
        protected AtomicLong formulas() {
            return formulas;
        }

        public long getCompounds() {
            return compounds().get();
        }

        public long getFormulas() {
            return formulas().get();
        }


    }

    public boolean isInheritance() {
        return inheritance;
    }

    public long getFilter() {
        return filter;
    }

    @JsonIgnore
    public Set<String> getInheritedDBs() {
        return DataSources.getDataSourcesFromBitFlags(getFilter());
    }

    public List<CdkFingerprintVersion.USED_FINGERPRINTS> getFingerprintVersion() {
        return fingerprintVersion;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }
}
