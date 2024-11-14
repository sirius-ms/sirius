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

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public abstract class FoldChange extends Statistics implements ForeignKey {

    protected long foreignId;

    protected double foldChange;

    @AllArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class CompoundFoldChange extends FoldChange implements CompoundForeignKey {

    }

    @AllArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    @ToString
    public static class AlignedFeaturesFoldChange extends FoldChange implements AlignedFeaturesForeignKey {

    }

}
