/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.model.core.statistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public abstract class FoldChange extends Statistics implements ForeignKey {

    protected double leftAbundance;
    protected double rightAbundance;

    /**
     * @deprecated This field is retained strictly for backward compatibility with older database schemas and software versions.
     * It should not be used in new code. Use {@link #getFoldChange()} or {@link #leftAbundance} and {@link #rightAbundance} instead.
     */
    @Deprecated
    private Double foldChange;

    @JsonIgnore
    public double getFoldChange(){
        if (leftAbundance == 0.0 && rightAbundance == 0.0 && foldChange != null) {
            return foldChange;
        }
        return (rightAbundance > 0) ? (leftAbundance / rightAbundance) : (leftAbundance > 0 ? Double.POSITIVE_INFINITY : 1.0);
    }

    @JsonGetter("foldChange")
    @Deprecated
    private Double getFoldChangeForJackson() {
        if (foldChange != null) {
            return foldChange;
        }
        return (rightAbundance > 0) ? (leftAbundance / rightAbundance) : (leftAbundance > 0 ? Double.POSITIVE_INFINITY : 1.0);
    }

    @JsonSetter("foldChange")
    @Deprecated
    private void setFoldChangeForJackson(Double foldChange) {
        this.foldChange = foldChange;
    }

    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class CompoundFoldChange extends FoldChange {
        protected long compoundId;

        @JsonIgnore
        @Override
        public long getForeignId() {
            return compoundId;
        }
    }

    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class AlignedFeaturesFoldChange extends FoldChange {
        protected long alignedFeatureId;

        @JsonIgnore
        @Override
        public long getForeignId() {
            return alignedFeatureId;
        }
    }

    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class NpcFoldChange extends FoldChange {
        protected long npcIndex;

        @JsonIgnore
        @Override
        public long getForeignId() {
            return npcIndex;
        }
    }

    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class ClassyfireFoldChange extends FoldChange {
        protected int classyfireIndex;

        @JsonIgnore
        @Override
        public long getForeignId() {
            return classyfireIndex;
        }
    }
}
