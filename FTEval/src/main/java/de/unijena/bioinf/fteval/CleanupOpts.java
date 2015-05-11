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
package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 10/10/13
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CleanupOpts extends EvalBasicOptions {

    @Option(shortName = "d", description = "delete files which tree contains less than <n> nodes", defaultValue = "0")
    public int getEdgeLimit();

    @Option(shortName = "I", description = "use InChi for identity test")
    public boolean useInchi();

}
