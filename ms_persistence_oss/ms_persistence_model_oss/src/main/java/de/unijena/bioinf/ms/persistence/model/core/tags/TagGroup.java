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

package de.unijena.bioinf.ms.persistence.model.core.tags;

import jakarta.persistence.Id;
import lombok.*;

/**
 * Defines and stores a group of entities based on a lucene search query.
 * Allows to access the predefined group by name instead of the search query.
 * todo: in the future this can be abstracted and groups implementation based on other mechanisms can be implemented.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class TagGroup {

    @Id
    private long id;

    private String groupName;

    private String luceneQuery;

    private String groupType;

    @Builder.Default
    private Boolean editable = true;

    public boolean isEditable() {
        return editable == null ? true : editable;
    }

    public Boolean getEditable() {
        return isEditable();
    }

}
