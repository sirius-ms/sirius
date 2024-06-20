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
    BufferedImage BACKGROUND = Icons.DRAG_N_DROP_IMAGE_160();

    default void paintDropImage(Graphics g, Supplier<Boolean> onlyIf) {
        if (onlyIf.get()) {
            if (BACKGROUND != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                int x = getWidth() / 2 - BACKGROUND.getWidth() / 2;
                int y = getHeight() / 2 - BACKGROUND.getHeight() / 2;
                g2d.drawImage(BACKGROUND, null, x, y);
                g2d.dispose();
            }
        }
    }

    int getHeight();

    int getWidth();

}
