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
import de.unijena.bioinf.ms.gui.utils.query.*;

import io.sirius.ms.sdk.model.SearchableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the accept key (Enter, or Tab) would do with the text currently typed into the free-text box
 * of the search bar.
 */
public sealed interface Completion {

    /**
     * Start a draft clause for a field, optionally negated and joined with a connector.
     */
    record ClauseStart(@NotNull SearchableField field, boolean negated,
                       @Nullable LogicOp logic) implements Completion {
    }

    /**
     * Open a (possibly negated) group; a field typed straight after the paren ({@code (ma}) opens
     * the group and starts a clause inside it at once.
     */
    record OpenGroup(boolean groupNegated, @Nullable LogicOp logic,
                     @Nullable ClauseStart clause) implements Completion {
    }

    /**
     * Close the innermost open group.
     */
    record CloseGroup() implements Completion {
    }
}
