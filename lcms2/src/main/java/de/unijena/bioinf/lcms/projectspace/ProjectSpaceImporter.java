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

package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.storage.db.nosql.Database;

import java.io.IOException;

public class ProjectSpaceImporter implements ImportStrategy {

    private Database<?> store;

    public ProjectSpaceImporter() {
        // TODO how to get currently open project space?
    }

    public ProjectSpaceImporter(Database<?> store) {
        this.store = store;
    }

    @Override
    public void importTrace(AbstractTrace trace) throws IOException {
        store.insert(trace);
    }

    @Override
    public void importAlignedFeature(AlignedFeatures alignedFeatures) {

    }

}
