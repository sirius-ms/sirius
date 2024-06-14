/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.fingerid.blast;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.ANY)
public class FingerprintStatistics {
    @Getter
    private final int numOfCandidates;
    @Getter
    private final int[] parentIndeces;
    @Getter
    private final int[] propertyAbundance;
    @Getter
    private final int[] propertyAbundanceWithParent;

    //just for jackson
    private FingerprintStatistics() {
        numOfCandidates = 0;
        parentIndeces = null;
        propertyAbundance = null;
        propertyAbundanceWithParent = null;
    }

    public FingerprintStatistics(int numOfCandidates, int fpCardinality) {
        this.numOfCandidates = numOfCandidates;
        this.parentIndeces = new int[fpCardinality];
        this.propertyAbundance = new int[fpCardinality];
        this.propertyAbundanceWithParent = new int[fpCardinality];
        Arrays.fill(this.parentIndeces, -1);
    }

    @JsonIgnore
    void setParentIndex(int fpIndex, int parentFpIndex){
        this.parentIndeces[fpIndex] = parentFpIndex;
    }

    @JsonIgnore
    void incrementPropertyAbundance(int fpIndex) {
        this.propertyAbundance[fpIndex]++;
    }

    @JsonIgnore
    void incrementPropertyAbundanceWithParent(int fpIndex) {
        this.propertyAbundanceWithParent[fpIndex]++;
    }

    @JsonIgnore
    public double getMarginalProbability(int fpIndex){
        return ((double) propertyAbundance[fpIndex]) / (double) numOfCandidates;
    }

    @JsonIgnore
    public double getConditionalProbability(int fpIndex){
        final int parentIndex = parentIndeces[fpIndex];
        if (parentIndex < 0)
            return getMarginalProbability(fpIndex);

        return (((double) propertyAbundanceWithParent[fpIndex]) / (double) numOfCandidates) / getMarginalProbability(parentIndex);
    }
}
