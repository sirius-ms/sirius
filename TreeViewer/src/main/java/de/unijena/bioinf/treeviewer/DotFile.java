/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.treeviewer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


public class DotFile extends DotSource {

    private final File file;

    public DotFile(File file) {
        super(beautifyName(file));
        this.file = file;
    }

    @Override
    public Reader getContent() throws IOException{
        return new FileReader(file);
    }

    public static String beautifyName(File f) {
        if (f.getAbsolutePath().startsWith("/tmp")) {
            return f.getName().replaceFirst("\\d+\\.(?:dot|gv)$", "");
        } else {
            final String s = f.getName();
            final int i = f.getName().lastIndexOf('.');
            if (i >= 0)
                return s.substring(0, i);
            else return s;
        }
    }

    @Override
    public String getSource() {
        return file.getAbsolutePath();
    }
}
