/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ActionUtils {

    public static Action deriveFrom(@Nullable Consumer<ActionEvent> doBefore, @NotNull Action source) {
        return deriveFrom(doBefore, source, null);
    }

    public static Action deriveFrom(@NotNull Action source, @Nullable Consumer<ActionEvent> doAfter) {
        return deriveFrom(null, source, doAfter);
    }

    public static Action deriveFrom(@Nullable Consumer<ActionEvent> doBefore, @NotNull Action source, @Nullable Consumer<ActionEvent> doAfter) {
        return new DerivedAction(doBefore, source, doAfter);
    }

    public static final class DerivedAction extends AbstractAction {

        private final Action source;
        private final Consumer<ActionEvent> doBefore;
        private final Consumer<ActionEvent> doAfter;

        private DerivedAction(@Nullable Consumer<ActionEvent> doBefore, @NotNull Action source, @Nullable Consumer<ActionEvent> doAfter) {
            this.source = source;
            this.doBefore = doBefore;
            this.doAfter = doAfter;
            putValue(Action.NAME, source.getValue(Action.NAME));
            putValue(Action.SHORT_DESCRIPTION, source.getValue(Action.SHORT_DESCRIPTION));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (doBefore != null)
                doBefore.accept(e);
            source.actionPerformed(e);
            if (doAfter != null)
                doAfter.accept(e);
        }
    }
}
