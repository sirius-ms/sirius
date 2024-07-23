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
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class SiriusProjectDocumentDbAdapter implements SiriusDatabaseAdapter {

    public SiriusProjectDocumentDatabase<?> store;

    public SiriusProjectDocumentDbAdapter() {
        // TODO how to get currently open project space?
    }

    public SiriusProjectDocumentDbAdapter(SiriusProjectDocumentDatabase<?> store) {
        this.store = store;
    }

    @Override
    public void importRun(LCMSRun run) throws IOException {
        store.getStorage().insert(run);
        if (run.getRetentionTimeAxis().isPresent()) store.getStorage().insert(run.getRetentionTimeAxis().get());
    }

    @Override
    public void updateRun(LCMSRun run) throws IOException {
        store.getStorage().upsert(run);
        if (run.getRetentionTimeAxis().isPresent()) store.getStorage().upsert(run.getRetentionTimeAxis().get());
    }

    @Override
    public void importMergedRun(MergedLCMSRun mergedRun) throws IOException {
        store.getStorage().insert(mergedRun);
    }

    @Override
    public void updateMergedRun(MergedLCMSRun mergedRun) throws IOException {
        store.getStorage().upsert(mergedRun);
    }

    @Override
    public void removeMergedRun(MergedLCMSRun run) throws IOException {
        for (long id : run.getRunIds()) {
            store.getStorage().removeAll(Filter.where("runId").eq(id), LCMSRun.class);
        }
        store.getStorage().removeAll(Filter.where("runId").eq(run.getRunId()), MergedLCMSRun.class);
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
    public void removeMergedTrace(long id) throws IOException {
        store.getStorage().removeAll(Filter.where("mergedTraceId").eq(id), MergedTrace.class);
    }

    @Override
    public void removeSourceTrace(long id) throws IOException {
        store.getStorage().removeAll(Filter.where("sourceTraceId").eq(id), SourceTrace.class);
    }

    @Override
    public boolean importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException {
        if (Math.abs(alignedFeatures.getCharge()) > 1) {
            LoggerFactory.getLogger(SiriusProjectDocumentDbAdapter.class).debug(String.format(Locale.US,
                    "SIRIUS does not support multiple charged ions yet. This feature will be ignored: m/z = %.4f, rt = %.2f minutes",
                    alignedFeatures.getApexMass(), alignedFeatures.getRetentionTime().getMiddleTime()/60d));
            return false;
        }
        if (alignedFeatures.getCharge()==0) {
            throw new IllegalArgumentException("Charge cannot be zero!");
        }
        store.importAlignedFeatures(List.of(alignedFeatures));
        return true;
    }

    @Override
    public void importRetentionTimeAxis(RetentionTimeAxis axis, boolean update) throws IOException {
        if (update) {
            store.getStorage().upsert(axis);
        } else {
            store.getStorage().insert(axis);
        }
    }

    @Override
    public Stream<AlignedFeatures> getImportedFeatureStream(long runId) throws IOException {
        return store.getStorage().findStr(Filter.where("runId").eq(runId), AlignedFeatures.class).map(x -> store.fetchMsData(x));
    }

}
