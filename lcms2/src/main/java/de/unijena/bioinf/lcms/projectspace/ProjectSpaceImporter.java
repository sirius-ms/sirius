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
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;

import java.io.IOException;
import java.util.List;

public class ProjectSpaceImporter implements ImportStrategy {

    private SiriusProjectDocumentDatabase<?> store;

    public ProjectSpaceImporter() {
        // TODO how to get currently open project space?
    }

    public ProjectSpaceImporter(SiriusProjectDocumentDatabase<?> store) {
        this.store = store;
    }

    @Override
    public void importRun(LCMSRun run) throws IOException {
        store.getStorage().insert(run);
    }

    @Override
    public void updateRun(LCMSRun run) throws IOException {
        store.getStorage().upsert(run);
    }

    @Override
    public void importMergedRun(MergedLCMSRun mergedRun) throws IOException {
        store.getStorage().insert(mergedRun);
    }

    @Override
    public void importScan(Scan scan) throws IOException {
        store.getStorage().insert(scan);
    }

    @Override
    public void importMSMSScan(MSMSScan scan) throws IOException {
        store.getStorage().insert(scan);
    }

    @Override
    public void importTrace(AbstractTrace trace) throws IOException {
        store.getStorage().insert(trace);
    }

    @Override
    public void importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException {
        store.importAlignedFeatures(List.of(alignedFeatures));
    }

    @Override
    public void importRetentionTimeAxis(RetentionTimeAxis axis) throws IOException {
        store.getStorage().insert(axis);
    }

}
