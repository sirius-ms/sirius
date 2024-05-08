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

package de.unijena.bioinf.ms.persistence.model.core.trace;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A source trace is a trace belonging to a LCMS run that is projected onto a merged run.
 * Thus, it contains two different intensity array:
 * - intensities refers to the projected intensities relative to the merge run
 * - rawIntensities refers to the original intensity values from the LCMS run
 */

@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SourceTrace extends AbstractTrace {

    @Id
    @ToString.Include
    private long sourceTraceId;

    FloatList rawIntensities;

    int rawScanIndexOffset;
}
