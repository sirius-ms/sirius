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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class MsDataSourceReference {

    @JsonProperty @Nullable protected final String runId;
    @JsonProperty @Nullable protected final String mzmlId;

    @JsonProperty @Nullable protected final String fileName;
    @JsonIgnore @Nullable protected final URI sourceLocation;

    private final int hashcode;

    public MsDataSourceReference(@Nullable URI sourceLocation, @Nullable String fileName, @Nullable String lcmsRunId, @Nullable String mzmlId) {
        this.runId = lcmsRunId;
        this.mzmlId = mzmlId;
        this.fileName = fileName;
        this.sourceLocation = sourceLocation;
        this.hashcode = Objects.hash(lcmsRunId, mzmlId, sourceLocation, fileName);
    }

    @JsonCreator
    public MsDataSourceReference(@JsonProperty("sourceLocation") String sourceLocation, @JsonProperty("filename") String filename, @JsonProperty("lcmsrunid") String lcmsrunid, @JsonProperty("mzmlid") String mzmlId) {
        this(URI.create(sourceLocation), filename, lcmsrunid, mzmlId);
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


    @JsonIgnore public Optional<String> getFileName() {
        return Optional.ofNullable(fileName);
    }
    @JsonIgnore public Optional<String> getRunId() {
        return Optional.ofNullable(runId);
    }

    @JsonIgnore public Optional<String> getMzmlId() {
        return Optional.ofNullable(mzmlId);
    }

    @JsonIgnore public Optional<URI> getSourceLocation() {
        return Optional.ofNullable(sourceLocation);
    }

    @JsonIgnore public Optional<URI> getSource() {
        return (sourceLocation!=null && fileName!=null) ? Optional.of(sourceLocation.resolve("./" + fileName)) : Optional.empty();
    }

    @JsonProperty("sourceLocation")
    private String getSourceURIAsString() {
        return sourceLocation==null ? null : sourceLocation.toString();
    }
}
