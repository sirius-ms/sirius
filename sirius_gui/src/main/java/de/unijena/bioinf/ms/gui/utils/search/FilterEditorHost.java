/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

import org.jetbrains.annotations.NotNull;

/**
 * Opens the full editor for a {@link FilterTerm} whose value cannot be edited inline (set-valued or
 * complex facets). The concrete host decides what "full editor" means: the collapsed bar / overlay
 * open the filter dialog (later: selecting the term's tab), the embedded-in-dialog renderer just
 * selects the term's tab and focuses its picker. Keeps the query engine free of any dialog knowledge.
 */
public interface FilterEditorHost {

    /**
     * Reveal the full editor for {@code term} (e.g. open the filter dialog on the tab that owns it).
     */
    void openEditorFor(@NotNull FilterTerm term);
}
