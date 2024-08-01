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

package de.unijena.bioinf.lcms.detection;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.Scan;
import de.unijena.bioinf.ms.persistence.model.core.Trace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.Getter;

import java.util.List;

@Getter
class TraceCandidate {

    private final IntList spectrumIndices = new IntArrayList();

    private final DoubleList masses = new DoubleArrayList();

    private final DoubleList intensities = new DoubleArrayList();

    public TraceCandidate(int spectrumIndex, Peak peak) {
        this.spectrumIndices.add(spectrumIndex);
        this.masses.add(peak.getMass());
        this.intensities.add(peak.getIntensity());
    }

    public Trace toTrace(Run run, List<Scan> scans) {
        return Trace.builder()
                .runId(run.getRunId())
                .scanIds(new LongArrayList(spectrumIndices.intStream().mapToLong(i -> scans.get(i).getScanId()).iterator()))
                .rts(new DoubleArrayList(spectrumIndices.intStream().mapToDouble(i -> scans.get(i).getScanTime()).iterator()))
                .mzs(masses)
                .intensities(intensities)
                .build();
    }

}
