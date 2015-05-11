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

import de.unijena.bioinf.FTAnalysis.FTLearn;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 03.07.13
 * Time: 19:22
 * To change this template use File | Settings | File Templates.
 */
public class LearnCommand implements Command {
    @Override
    public String getDescription() {
        return "Learn a profile (and all scoring parameters) from reference measurements";
    }

    @Override
    public String getName() {
        return "learn";
    }

    @Override
    public void run(String[] args) {
        FTLearn.main(args);
    }

    @Override
    public String getVersion() {
        return FTLearn.VERSION_STRING;
    }
}
