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

package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class ZodiacEdgeFilterThresholds implements Ms2ExperimentAnnotation {

    /**
     * Defines the proportion of edges of the complete network which will be ignored.
     */
    @DefaultProperty public final double thresholdFilter;

    /**
     * Minimum number of candidates per compound which are forced to have at least [minLocalConnections] connections to other compounds.
     * E.g. 2 candidates per compound must have at least 10 connections to other compounds
     */
    @DefaultProperty public final int minLocalCandidates;

    /**
     * Minimum number of connections per candidate which are forced for at least [minLocalCandidates] candidates to other compounds.
     * E.g. 2 candidates per compound must have at least 10 connections to other compounds
     */
    @DefaultProperty public final int minLocalConnections;

    private ZodiacEdgeFilterThresholds() {
//        thresholdFilter = 0.95;
//        minLocalCandidates = 1;
//        minLocalConnections = 10;
        thresholdFilter = Double.NaN;
        minLocalCandidates = -1;
        minLocalConnections = -1;
    }

    public ZodiacEdgeFilterThresholds(double thresholdFilter, int minLocalCandidates, int minLocalConnections) {
        this.thresholdFilter = thresholdFilter;
        this.minLocalCandidates = minLocalCandidates;
        this.minLocalConnections = minLocalConnections;
    }
}
