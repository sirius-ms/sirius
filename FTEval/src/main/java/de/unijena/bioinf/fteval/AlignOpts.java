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
import com.lexicalscope.jewel.cli.Unparsed;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 8/12/13
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AlignOpts extends EvalBasicOptions {

    @Unparsed
    public List<String> names();

    @Option(shortName = "J")
    public boolean isNoMultijoin();

    @Option(shortName = "t", defaultValue = "matrix.csv")
    public String getTarget();

    @Option(shortName = "x", defaultToNull = true)
    public List<String> getXtra();

    @Option(shortName = "Z")
    boolean isNoNormalizing();
}
