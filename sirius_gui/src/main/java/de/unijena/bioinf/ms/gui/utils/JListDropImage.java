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

package de.unijena.bioinf.ms.gui.utils;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class JListDropImage<T> extends JList<T> implements DropImage {

    private Supplier<Boolean> isEmptyCheck = () -> getModel().getSize() == 0;
    private Supplier<Boolean> noResultsCheck = () -> false;

    public JListDropImage(ListModel<T> dataModel) {
        super(dataModel);
    }
    public JListDropImage(ListModel<T> dataModel, @NotNull Supplier<Boolean> isEmptyCheck, @NotNull Supplier<Boolean> noResultsCheck) {
        super(dataModel);
        this.isEmptyCheck = isEmptyCheck;
        this.noResultsCheck = noResultsCheck;
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintDropImage(g, isEmptyCheck, noResultsCheck);
    }
}
