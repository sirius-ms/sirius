/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core.feature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public abstract class AbstractAlignedFeatures extends AbstractFeature {

    @JsonIgnore
    @ToString.Exclude
    protected List<Feature> features;

    private TraceRef traceRef;

    @Override
    public TraceRef getTraceRef() {
        return traceRef;
    }

    public Optional<List<Feature>> getFeatures() {
        return Optional.ofNullable(features);
    }

    private boolean hasMs1;
    private boolean hasMsMs;

    @JsonIgnore
    @ToString.Exclude
    protected MSData msData;

    public Optional<MSData> getMSData() {
        return Optional.ofNullable(msData);
    }

    public abstract long databaseId();

}
