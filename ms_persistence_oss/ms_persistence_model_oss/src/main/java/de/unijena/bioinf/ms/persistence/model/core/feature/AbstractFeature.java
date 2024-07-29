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
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public abstract class AbstractFeature {

    /**
     * charge determines the polarity as well as the number of charges
     */
    protected byte charge;

    /**
     * ID of the run this feature belongs to
     */
    protected Long runId;

    /**
     * m/z of the apex
     */
    protected Double apexMass;

    /**
     * intensity of the apex
     */
    protected Double apexIntensity;

    /**
     * average m/z
     */
    protected double averageMass;

    /**
     * width (in seconds) of the peak at half maximum
     */
    protected Double fwhm;

    /**
     * retention time (start, apex, end)
     */
    protected RetentionTime retentionTime;

    @JsonIgnore
    public boolean isRTInterval(){
        return retentionTime != null && retentionTime.isInterval();
    }

    /**
     * signal-to-noise ratio at the apex
     */
    protected Double snr;

    protected abstract TraceRef getTraceRef();

    public Optional<? extends TraceRef> getTraceReference() {
        return Optional.ofNullable(getTraceRef());
    }

    /**
     * Data source (e.g. file, url, stream etc.)
     */
    private DataSource dataSource;

    public Optional<DataSource> getDataSource() {
        return Optional.ofNullable(dataSource);
    }

}
