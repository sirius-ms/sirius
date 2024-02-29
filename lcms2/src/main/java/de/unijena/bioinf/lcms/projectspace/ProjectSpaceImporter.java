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
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedRun;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.run.SampleStats;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ProjectSpaceImporter implements ImportStrategy {

    private Database<?> store;

    public ProjectSpaceImporter() {
        // TODO how to get currently open project space?
    }

    public ProjectSpaceImporter(Database<?> store) {
        this.store = store;
    }

    @Override
    public void importRun(Run run) throws IOException {
        store.insert(run);
    }

    @Override
    public void updateRun(Run run) throws IOException {
        store.upsert(run);
    }

    @Override
    public void importMergedRun(MergedRun mergedRun) throws IOException {
        store.insert(mergedRun);
    }

    @Override
    public void importScan(Scan scan) throws IOException {
        store.insert(scan);
    }

    @Override
    public void importMSMSScan(MSMSScan scan) throws IOException {
        store.insert(scan);
    }

    @Override
    public void importTrace(AbstractTrace trace) throws IOException {
        store.insert(trace);
    }

    @Override
    public void importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException {
        store.insert(alignedFeatures);

        List<Feature> childFeatures = alignedFeatures.getFeatures().orElse(Collections.emptyList()).stream().peek(f -> f.setAlignedFeatureId(alignedFeatures.getAlignedFeatureId())).toList();
        store.insertAll(childFeatures);
        alignedFeatures.setFeatureIds(LongArrayList.toListWithExpectedSize(childFeatures.stream().mapToLong(Feature::getFeatureId), childFeatures.size()));

        List<AlignedIsotopicFeatures> isotopicFeatures = alignedFeatures.getIsotopicFeatures().orElse(Collections.emptyList()).stream().peek(f -> f.setAlignedFeatureId(alignedFeatures.getAlignedFeatureId())).toList();
        store.insertAll(isotopicFeatures);

        for (AlignedIsotopicFeatures isotopicFeature : isotopicFeatures) {
            List<Feature> isotopicChildFeatures = isotopicFeature.getFeatures().orElse(Collections.emptyList()).stream().peek(f -> f.setAlignedFeatureId(isotopicFeature.getAlignedIsotopeFeatureId())).toList();
            isotopicFeature.setFeatureIds(LongArrayList.toListWithExpectedSize(isotopicChildFeatures.stream().mapToLong(Feature::getFeatureId), isotopicChildFeatures.size()));
        }
        store.upsertAll(isotopicFeatures);

        alignedFeatures.setIsotopicFeaturesIds(LongArrayList.toListWithExpectedSize(isotopicFeatures.stream().mapToLong(AlignedIsotopicFeatures::getAlignedIsotopeFeatureId), isotopicFeatures.size()));
        store.upsert(alignedFeatures);
    }

}
