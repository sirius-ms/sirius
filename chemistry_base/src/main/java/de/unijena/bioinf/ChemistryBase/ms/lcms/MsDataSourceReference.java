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

package de.unijena.bioinf.ChemistryBase.ms.lcms;

import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Objects;

/**
 * A reference to a certain LC/MS run in a mzml file.
 * The reference has two purposes:
 * 1. if two spectra stem from different LC/MS runs, we need some kind of ID to distuingish those runs.
 *    in this case, the MsDataSourceReference is only an ID object
 * 2. we might have a registry that connects an source ref to a mzml file. Then this source ref acts
 *    like a pointer to the entry in the mzml file where we can find the raw data belonging to a compound
 *
 * Note: mzXML files do not have an ID and can only be referenced with a source location.
 */
public final class MsDataSourceReference {

    @Nullable protected final String runId;
    @Nullable protected final String mzmlId;

    @Nullable protected final String fileName;
    @Nullable protected final URI sourceLocation;

    @Nonnull  private final int hashcode;

    public MsDataSourceReference(@Nullable URI sourceLocation, @Nullable String fileName, @Nullable String lcmsRunId, @Nullable String mzmlId) {
        this.runId = lcmsRunId;
        this.mzmlId = mzmlId;
        this.fileName = fileName;
        this.sourceLocation = sourceLocation;
        this.hashcode = Objects.hash(lcmsRunId, mzmlId, sourceLocation, fileName);
    }

    public boolean equals(Object o) {
        if (o instanceof MsDataSourceReference)
            return equals((MsDataSourceReference)o);
        else return false;
    }
    public boolean equals(MsDataSourceReference sourceRef) {
        if (this==sourceRef) return true;
        return Objects.equals(runId,sourceRef.runId) && Objects.equals(mzmlId,sourceRef.mzmlId) && Objects.equals(sourceLocation,sourceRef.sourceLocation) && Objects.equals(sourceLocation,sourceRef.fileName);
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    public String getFileName() {
        return fileName;
    }
    public String getRunId() {
        return runId;
    }

    public String getMzmlId() {
        return mzmlId;
    }

    public URI getSourceLocation() {
        return sourceLocation;
    }

    public URI getSource() {
        return sourceLocation.resolve("./" + fileName);
    }
}
