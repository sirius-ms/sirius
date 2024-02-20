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

import de.unijena.bioinf.ms.persistence.model.core.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.scan.MergedMSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceSegment;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractFeature {

    /**
     * ID of the run this feature belongs to
     */
    @ToString.Include
    protected long runId;

    /**
     *
     */
    protected double apexMass;

    /**
     * Trace segments that define this feature. One segment per isotope
     */
    @Builder.Default
    protected List<TraceSegment> traceSegments = new ArrayList<>();

    /**
     * Extracted isotope pattern of this feature
     */
    protected IsotopePattern isotopePattern;

    /**
     * Merged MS/MS spectra of this feature
     */
    protected MergedMSMSScan mergedMSMSScan;

}
