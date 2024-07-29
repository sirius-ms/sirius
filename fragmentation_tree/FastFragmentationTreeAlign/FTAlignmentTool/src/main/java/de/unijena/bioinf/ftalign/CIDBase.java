
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ftalign;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Kai Dührkop
 */
public class CIDBase {

    private final HashMap<String, Integer> cids;

    public CIDBase() {
        this.cids = new HashMap<String, Integer>(1000);
    }
    
    public Integer get(String name) {
        return cids.get(name);
    }

    public boolean read(File cidfile) throws IOException {
        return read(cidfile, 0, 1);
    }

    public boolean read(File cidfile, final int nameCol, final int cidCol) throws IOException {
        final boolean[] complete = new boolean[1];
        complete[0] = true;
        CSVReader.read(cidfile, new CSVHandler(){
            private Integer cid;
            private String name;
            @Override
            public void entry(int row, int col, String entry) {
                if (row > 0) {
                    if (col == cidCol) {
                        try {
                            cid = Integer.parseInt(entry);
                        } catch (NumberFormatException exc) {
                            cid = null;
                            complete[0] = false;
                        }
                    } else if (col == nameCol) {
                        name = entry;
                    }
                }
            }

            @Override
            public void endOfRow(int row) {
                if (cid != null) cids.put(name, cid);
            }
        });
        return complete[0];
    }


}
