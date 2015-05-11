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
package de.unijena.bioinf.siriuscli;

public class AlignCommand implements Command {

    public AlignCommand() {
    }

    @Override
    public String getDescription() {
        return "compute alignment scores between fragmentation trees"; // TODO
    }

    @Override
    public String getName() {
        return "align";
    }

    @Override
    public void run(String[] args) {
        de.unijena.bioinf.ftalign.Main.main(args);
    }

    @Override
    public String getVersion() {
        return de.unijena.bioinf.ftalign.Main.VERSION;
    }
}
