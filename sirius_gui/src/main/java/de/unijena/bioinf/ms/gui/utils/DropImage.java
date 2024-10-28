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

import de.unijena.bioinf.ms.gui.configs.Icons;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public interface DropImage {
    BufferedImage EMPTY_DROP = Icons.DRAG_N_DROP_IMAGE_160();
    BufferedImage NO_FILTER_RESULTS = Icons.NO_RESULT_IMAGE_160();

    default void paintDropImage(Graphics g, Supplier<Boolean> isEmpty) {
        paintDropImage(g, isEmpty, () -> false);
    }

    default void paintDropImage(Graphics g, Supplier<Boolean> isEmpty, Supplier<Boolean> noResults) {
        BufferedImage image = null;
        if (isEmpty.get()) {
            image = EMPTY_DROP;
        } else if (noResults.get()) {
            image = NO_FILTER_RESULTS;
        }

        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            int x = getWidth() / 2 - image.getWidth() / 2;
            int y = getHeight() / 2 - image.getHeight() / 2;
            g2d.drawImage(image, null, x, y);
            g2d.dispose();
        }

    }

    int getHeight();

    int getWidth();

}
