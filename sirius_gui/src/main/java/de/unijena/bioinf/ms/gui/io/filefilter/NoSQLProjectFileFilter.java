/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.io.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

public class NoSQLProjectFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;
        return f.getName().endsWith(SIRIUS_PROJECT_SUFFIX); //todo nightsky: do we want a proper project db check here?
    }

    @Override
    public String getDescription() {
        return SIRIUS_PROJECT_SUFFIX;
    }
}
