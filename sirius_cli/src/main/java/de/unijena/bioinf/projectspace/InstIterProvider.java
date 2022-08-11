/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public interface InstIterProvider extends Iterator<Ms2Experiment> {
    /**
     * @return the exact size of the result iterable or an upper bound if not available, if result is null it returns -1.
     */
    static int getResultSizeEstimate(@Nullable Iterable<? extends Instance> source) {;
        if (source == null)
            return -1;
        if (!source.iterator().hasNext())
            return 0;
        if (source instanceof Collection)
            return ((Collection<?>) source).size();
        if (source instanceof IterableWithSize)
            return ((IterableWithSize<?>) source).size();
        LoggerFactory.getLogger(InstIterProvider.class).warn("Estimating Iterable<Instance> size from project-space. Might be inaccurate and slow.");
        return source.iterator().next().getProjectSpaceManager().size();
    }
    default InstanceImportIteratorMS2Exp asInstanceIterator(ProjectSpaceManager<?> projectSpace) {
        return new InstanceImportIteratorMS2Exp(this, projectSpace);
    }

    default InstanceImportIteratorMS2Exp asInstanceIterator(ProjectSpaceManager<?> projectSpace, Predicate<CompoundContainer> compoundFilter) {
        return new InstanceImportIteratorMS2Exp(this, projectSpace, compoundFilter);
    }
}