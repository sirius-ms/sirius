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

import lombok.*;

@Getter
@NoArgsConstructor
@ToString
public class TraceRef {

    /**
     * ID of the trace
     */
    private long traceId;

    /**
     * the index offset of the trace(!). This information is also contained in the trace itself, but
     * having this index here allows for fetching retention time information and so on without looking up the trace
     * itself
     */
    private int scanIndexOffsetOfTrace;

    /**
     * Start index of the segment relative to the trace
     */
    private int start;

    /**
     * Apex index of the segment relative to the trace
     */
    private int apex;

    /**
     * End index of the segment relative to the trace
     */
    private int end;

    public TraceRef(long traceId, int scanIndexOffsetOfTrace, int start, int apex, int end) {
        this.traceId = traceId;
        this.scanIndexOffsetOfTrace = scanIndexOffsetOfTrace;
        if (start < 0)
            throw new IllegalArgumentException(String.format("start must be >= 0 (was %d)", start));
        if (apex < 0)
            throw new IllegalArgumentException(String.format("apex must be >= 0 (was %d)", apex));
        if (end < 0)
            throw new IllegalArgumentException(String.format("end must be >= 0 (was %d)", end));
        if (apex < start)
            throw new IllegalArgumentException(String.format("start (was %d) must be <= apex (was %d)", start, apex));
        if (apex > end)
            throw new IllegalArgumentException(String.format("apex (was %d) must be <= end (was %d)", apex, end));

        this.start = start;
        this.apex = apex;
        this.end = end;
    }

    public int absoluteApexId() {
        return apex + getScanIndexOffsetOfTrace();
    }

}
