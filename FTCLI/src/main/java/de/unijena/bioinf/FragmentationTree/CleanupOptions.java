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
package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.sirius.cli.BasicOptions;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import java.io.File;
import java.util.List;

/**
 * Created by kaidu on 12/5/13.
 */
public interface CleanupOptions extends ProfileOptions, BasicOptions {

    public static enum NOISE_FILTER {
        EXPLAINABLE, EXPLAINED;
    }

    @Option(shortName = "f", description = "molecular formula of the compound", defaultToNull = true)
    public String getFormula();

    @Option(description = "recalibrate the spectrum using the annotated peaks from the fragmentation tree")
    public boolean getRecalibrate();

    @Option(description = "EXPLAINABLE: delete all peaks with no explanation for the parent formula\nEXPLAINED: delete all peaks which are not contained in the tree", defaultToNull = true)
    public NOISE_FILTER getFilter();

    @Unparsed
    public List<File> getFiles();

    @Option(shortName = "t", defaultValue = "cleanedUp", description = "target directory for the output data")
    public File getTarget();

}
